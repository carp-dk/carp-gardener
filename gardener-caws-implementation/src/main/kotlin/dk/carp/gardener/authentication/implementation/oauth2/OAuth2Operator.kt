package dk.carp.gardener.authentication.implementation.oauth2

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.model.OAuthConstants
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.oauth.AccessTokenRequestParams
import com.github.scribejava.core.oauth.OAuth20Service
import com.github.scribejava.core.pkce.PKCE
import com.google.common.hash.Hashing
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.IOAuth2AuthorizationOperator
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2TokenRefreshParams
import dk.carp.gardener.authentication.core.authorization.devices.dexcom.DexcomDataSource
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitAccessTokenResponseExtractor
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsAccessTokenResponseExtractor
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataSource
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import dk.carp.gardener.authentication.ktor.PropertiesConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Provides an implementation for [IOAuth2AuthorizationOperator] and [IDataCollectionOperator].
 */
class OAuth2Operator(
    private val clientSettings: OAuth2ClientSettings,
    private val properties: PropertiesConfig,
    private val httpClient: HttpClient = HttpClient(CIO) { expectSuccess = false },
) : IOAuth2AuthorizationOperator,
    IDataCollectionOperator {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(OAuth2Operator::class.java)
    }

    private val api: DefaultApi20 =
        OAuth2ApiDefinition(clientSettings.accessUri, clientSettings.authorizationUri)

    private val service: OAuth20Service =
        ServiceBuilder(clientSettings.clientId)
            .apiSecret(clientSettings.clientSecret)
            .callback(clientSettings.callbackUri)
            .build(api)

    /**
     * Returns a completed authorization URI string that can be used to redirect the user
     * to the vendor's website to authorize.
     *
     * The URI must contain the [stateId], [requestedScopes] and the additional parameters
     * defined in [params].
     *
     * @param stateId The ID of the state associated with the current authorization session.
     * @param requestedScopes A string containing the requested scopes in a vendor specific format.
     * @param params Additional parameters to be included in the URI as query parameters.
     *
     * @return URI string.
     */
    override fun getCompleteAuthorizationUrlForState(
        stateId: String,
        requestedScopes: String,
        params: OAuth2AuthorizationRequestParams,
    ): String {
        if (params.additionalParamsForGrants.getMap().isNotEmpty()) {
            return service
                .createAuthorizationUrlBuilder()
                .additionalParams(params.additionalParamsForGrants.getMap())
                .scope(requestedScopes)
                .state(stateId)
                .build()
        }

        return service
            .createAuthorizationUrlBuilder()
            .scope(requestedScopes)
            .state(stateId)
            .build()
    }

    /**
     * Retrieves OAuth2 access parameters for the given user noted by [userId] and
     * for the given data source noted by [dataSourceId].
     *
     * It also created subscriptions to the third-party API's.
     *
     * @param userId The ID of the user.
     * @param dataSourceId The ID of the data source.
     * @param authorizationCode The authorization code in the OAuth2 Authorization Code Grant Flow.
     * @param params Additional parameters to be included in the request.
     *
     * @return OAuth2 Access Parameters for the user and data source.
     *
     * @throws IllegalStateException When the access parameters cannot be retrieved.
     */
    @Suppress("LongMethod", "TooGenericExceptionCaught", "UseCheckOrError")
    override fun retrieveAccessParams(
        userId: String,
        dataSourceId: String,
        authorizationCode: String,
        params: OAuth2AuthorizationRequestParams,
    ): OAuth2AccessParams {
        val request = createAccessTokenRequest(authorizationCode, params.additionalParamsForTokens.getMap())
        val rawResponse: String
        try {
            rawResponse = sendAccessTokenRequestSync(request)
        } catch (ex: Exception) {
            throw IllegalStateException(
                "OAuth2 Access Params retrieval failed from third-party API for $dataSourceId/$userId.",
                ex,
            )
        }
        LOGGER.info("OAuth2 Access Params are successfully retrieved from third-party API for $dataSourceId/$userId.")

        val result = extractParams(userId, dataSourceId, rawResponse)
        if (dataSourceId == FitbitDataSource.DATA_SOURCE_ID) {
            runBlocking {
                runCatching {
                    httpClient.post("https://api.fitbit.com/1/user/-/apiSubscriptions/$userId.json") {
                        parameter("subscriberId", "1")
                        header("Authorization", "Bearer ${result.extractAccessToken()}")
                    }
                }.onSuccess { response ->
                    if (!response.status.isSuccess()) {
                        val body = runCatching { response.bodyAsText() }.getOrDefault("")
                        LOGGER.info("Subscription creation failed (${response.status.value}): $body")
                    } else {
                        LOGGER.info("(HTTP ${response.status.value}) Fitbit notification created for user $userId")
                    }
                }.onFailure { ex -> LOGGER.info("Fitbit notification creation failed for user $userId: ${ex.message}") }
            }
        }
        if (dataSourceId == WithingsDataSource.DATA_SOURCE_ID) {
            // Get a nounce
            val nonceAction = "getnonce"
            val clientId = clientSettings.clientId
            val nonceTimestamp = Instant.now().epochSecond
            val nonceConcatenatedParams = "$nonceAction,$clientId,$nonceTimestamp"
            val clientSecret = clientSettings.clientSecret
            val nonceSignature =
                Hashing
                    .hmacSha256(
                        clientSecret.encodeToByteArray(),
                    ).hashString(nonceConcatenatedParams, StandardCharsets.UTF_8)
                    .toString()

            runBlocking {
                runCatching {
                    httpClient.post("https://wbsapi.withings.net/v2/signature") {
                        parameter("action", nonceAction)
                        parameter("client_id", clientId)
                        parameter("timestamp", nonceTimestamp.toString())
                        parameter("signature", nonceSignature)
                    }
                }.onSuccess { response ->
                    if (!response.status.isSuccess()) {
                        LOGGER.info("Error while getting nounce for Withings subscription for user $userId: HTTP ${response.status.value}")
                        return@onSuccess
                    }
                    val bodyNode = ConfiguredObjectMapper.instance.readTree(response.bodyAsText())

                    val nonce = bodyNode.get("body").get("nonce").textValue()
                    val subAction = "subscribe"
                    val subCallback = properties.getProperty("withings.client.collection.callback.uri")
                    val subAppli = "16"
                    val subConcatenatedParams = "$subAction,$clientId,$nonce"
                    val subSignature =
                        Hashing
                            .hmacSha256(
                                clientSecret.encodeToByteArray(),
                            ).hashString(subConcatenatedParams, StandardCharsets.UTF_8)
                            .toString()

                    runCatching {
                        httpClient.post("https://wbsapi.withings.net/notify") {
                            header("Authorization", "Bearer ${result.extractAccessToken()}")
                            parameter("action", subAction)
                            parameter("callbackurl", subCallback)
                            parameter("appli", subAppli)
                            parameter("nonce", nonce)
                            parameter("client_id", clientId)
                            parameter("signature", subSignature)
                        }
                    }.onSuccess { subscriptionResponse ->
                        val body = runCatching { subscriptionResponse.bodyAsText() }.getOrDefault("")
                        LOGGER.info(
                            "Withings subscription created for user $userId with appli 16 - (HTTP ${subscriptionResponse.status.value}) $body",
                        )
                    }.onFailure { ex ->
                        LOGGER.info("Withings subscription creation failed for user $userId: ${ex.message}")
                    }
                }.onFailure { ex ->
                    LOGGER.info("Error while getting nounce for Withings subscription for user $userId: ${ex.message}")
                }
            }
        }

        return result
    }

    /**
     * Refreshes OAuth2 access parameters for the given user noted by [userId] and
     * for the given data source noted by [dataSourceId].
     *
     * @param userId The ID of the user.
     * @param dataSourceId The ID of the data source.
     * @param refreshToken The refresh token to be used to get new access parameters.
     * @param params Additional parameters to be included in the request.
     *
     * @return Newly refreshed OAuth2 Access Parameters for the user and data source.
     *
     * @throws IllegalStateException When the access parameters cannot be retrieved.
     */
    @Suppress("TooGenericExceptionCaught", "UseCheckOrError")
    override fun refreshTokens(
        userId: String,
        dataSourceId: String,
        refreshToken: String,
        params: OAuth2TokenRefreshParams,
    ): OAuth2AccessParams {
        val request = createRefreshTokenRequest(refreshToken, params.additionalParams.getMap())
        val rawResponse: String
        try {
            rawResponse = sendAccessTokenRequestSync(request)
        } catch (ex: Exception) {
            throw IllegalStateException(
                "OAuth2 Refresh Token retrieval failed from third-party API for $dataSourceId/$userId.",
                ex,
            )
        }

        LOGGER.info("OAuth2 tokens are successfully refreshed from third-party API for $dataSourceId/$userId.")
        return extractParams(userId, dataSourceId, rawResponse)
    }

    /**
     * Contacts the third-party APIs to collect data.
     *
     * @param uri The URI of the Web Server that needs to be contacted.
     * @param dataType The type of data that needs to be collected.
     * @param accessParams Access parameters for authentication.
     * @param callback A callback function that needs to be called upon successful data retrieval.
     *
     * @throws IllegalStateException When the data collection failed.
     */
    @Suppress("TooGenericExceptionCaught", "UseCheckOrError")
    override fun executeRequest(
        uri: Uri,
        dataType: DataCollectionType,
        accessParams: AccessParams,
        callback: (String) -> Unit,
    ) {
        runBlocking {
            val response =
                try {
                    withContext(Dispatchers.IO) {
                        httpClient.get(uri.uri) {
                            header("Authorization", "Bearer ${accessParams.extractAccessToken()}")
                            uri.queryParams?.forEach { parameter(it.key, it.value) }
                        }
                    }
                } catch (ex: Exception) {
                    throw IllegalStateException(
                        "OAuth2 data collection failed from third-party API for ${accessParams.dataSourceId}/${accessParams.internalUserId}: ${ex.message}",
                        ex,
                    )
                }

            if (!response.status.isSuccess()) {
                val body = runCatching { response.bodyAsText() }.getOrDefault("")
                throw IllegalStateException(
                    "OAuth2 data collection failed from third-party API for ${accessParams.dataSourceId}/${accessParams.internalUserId}: $body",
                )
            }
            LOGGER.info(
                "OAuth2 data successfully collected from third-party API for ${accessParams.dataSourceId}/${accessParams.internalUserId}.",
            )
            callback(response.bodyAsText())
        }
    }

    private fun extractParams(
        userId: String,
        dataSourceId: String,
        rawResponse: String,
    ): OAuth2AccessParams =
        when (dataSourceId) {
            FitbitDataSource.DATA_SOURCE_ID -> {
                FitbitAccessTokenResponseExtractor.getAccessParamsFromResponse(userId, dataSourceId, rawResponse)
            }
            WithingsDataSource.DATA_SOURCE_ID -> {
                WithingsAccessTokenResponseExtractor.getAccessParamsFromResponse(userId, dataSourceId, rawResponse)
            }
            DexcomDataSource.DATA_SOURCE_ID -> {
                dk.carp.gardener.authentication.core.authorization.devices.dexcom.DexcomAccessTokenResponseExtractor
                    .getAccessParamsFromResponse(
                        userId,
                        dataSourceId,
                        rawResponse,
                    )
            }
            else -> {
                throw IllegalArgumentException("Access parameter extraction failed: DataSourceId $dataSourceId is not valid.")
            }
        }

    private fun createAccessTokenRequest(
        code: String,
        additionalParams: Map<String, String>?,
    ): OAuthRequest {
        val params = AccessTokenRequestParams.create(code)
        val request = OAuthRequest(api.accessTokenVerb, api.accessTokenEndpoint)
        api.clientAuthentication.addClientAuthentication(request, service.apiKey, service.apiSecret)
        request.addParameter(OAuthConstants.CODE, params.code)

        additionalParams?.forEach { (key, value) -> request.addBodyParameter(key, value) }
        request.addBodyParameter("grant_type", "authorization_code")
        request.addBodyParameter("client_id", service.apiKey)
        request.addBodyParameter("client_secret", service.apiSecret)
        request.addBodyParameter("code", params.code)
        request.addBodyParameter("redirect_uri", service.callback)

        val callback: String = service.callback
        request.addParameter(OAuthConstants.REDIRECT_URI, callback)
        val scope = params.scope
        if (scope != null) {
            request.addParameter(OAuthConstants.SCOPE, scope)
        } else if (service.defaultScope != null) {
            request.addParameter(OAuthConstants.SCOPE, service.defaultScope)
        }
        request.addParameter(OAuthConstants.GRANT_TYPE, OAuthConstants.AUTHORIZATION_CODE)
        val pkceCodeVerifier = params.pkceCodeVerifier
        if (pkceCodeVerifier != null) {
            request.addParameter(PKCE.PKCE_CODE_VERIFIER_PARAM, pkceCodeVerifier)
        }

        return request
    }

    private fun createRefreshTokenRequest(
        refreshToken: String?,
        additionalParams: Map<String, String>?,
        scope: String? = null,
    ): OAuthRequest {
        require(!refreshToken.isNullOrEmpty()) { "The refreshToken cannot be null or empty" }
        val request = OAuthRequest(api.accessTokenVerb, api.refreshTokenEndpoint)
        api.clientAuthentication.addClientAuthentication(request, service.apiKey, service.apiSecret)

        additionalParams?.forEach { (key, value) -> request.addBodyParameter(key, value) }
        request.addBodyParameter("grant_type", "refresh_token")
        request.addBodyParameter("client_id", service.apiKey)
        request.addBodyParameter("client_secret", service.apiSecret)
        request.addBodyParameter("redirect_uri", service.callback)

        if (scope != null) {
            request.addParameter(OAuthConstants.SCOPE, scope)
        } else if (service.defaultScope != null) {
            request.addParameter(OAuthConstants.SCOPE, service.defaultScope)
        }
        request.addParameter(OAuthConstants.REFRESH_TOKEN, refreshToken)
        request.addParameter(OAuthConstants.GRANT_TYPE, OAuthConstants.REFRESH_TOKEN)
        return request
    }

    private fun sendAccessTokenRequestSync(request: OAuthRequest): String = service.execute(request).body
}
