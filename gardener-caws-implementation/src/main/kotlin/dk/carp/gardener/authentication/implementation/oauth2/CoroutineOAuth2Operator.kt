package dk.carp.gardener.authentication.implementation.oauth2

import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.oauth.OAuth20Service
import com.google.common.hash.Hashing
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2TokenRefreshParams
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Coroutine-first implementation of OAuth2 operator functionality.
 * Exposes suspend functions that can be used directly from Ktor handlers.
 */
class CoroutineOAuth2Operator(
    private val clientSettings: OAuth2ClientSettings,
    private val service: OAuth20Service,
    private val httpClient: HttpClient = HttpClient(CIO) { expectSuccess = false },
) {
    suspend fun getCompleteAuthorizationUrlForState(
        stateId: String,
        requestedScopes: String,
        params: OAuth2AuthorizationRequestParams,
    ): String =
        withContext(Dispatchers.Default) {
            if (params.additionalParamsForGrants.getMap().isNotEmpty()) {
                return@withContext service
                    .createAuthorizationUrlBuilder()
                    .additionalParams(params.additionalParamsForGrants.getMap())
                    .scope(requestedScopes)
                    .state(stateId)
                    .build()
            }

            return@withContext service
                .createAuthorizationUrlBuilder()
                .scope(requestedScopes)
                .state(stateId)
                .build()
        }

    @Suppress("TooGenericExceptionCaught", "UnusedParameter")
    suspend fun retrieveAccessParams(
        userId: String,
        dataSourceId: String,
        authorizationCode: String,
        params: OAuth2AuthorizationRequestParams,
    ): OAuth2AccessParams {
        val token: OAuth2AccessToken =
            try {
                withContext(Dispatchers.IO) { service.getAccessToken(authorizationCode) }
            } catch (ex: Exception) {
                throw IllegalStateException(
                    "OAuth2 Access Params retrieval failed from third-party API for $dataSourceId/$userId.",
                    ex,
                )
            }

        val rawResponse = token.rawResponse ?: token.accessToken
        val result = extractParams(userId, dataSourceId, rawResponse)

        // subscriptions similar to original operator
        if (dataSourceId == "fitbit") {
            runCatching {
                httpClient.post("https://api.fitbit.com/1/user/-/apiSubscriptions/$userId.json") {
                    parameter("subscriberId", "1")
                    header("Authorization", "Bearer ${result.extractAccessToken()}")
                }
            }
        }

        if (dataSourceId == "withings") {
            // simplified; keep original behavior but as suspend
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

            runCatching {
                httpClient.post("https://wbsapi.withings.net/v2/signature") {
                    parameter("action", nonceAction)
                    parameter("client_id", clientId)
                    parameter("timestamp", nonceTimestamp.toString())
                    parameter("signature", nonceSignature)
                }
            }
        }

        return result
    }

    @Suppress("TooGenericExceptionCaught", "UnusedParameter")
    suspend fun refreshTokens(
        userId: String,
        dataSourceId: String,
        refreshToken: String,
        params: OAuth2TokenRefreshParams,
    ): OAuth2AccessParams {
        val token: OAuth2AccessToken =
            try {
                withContext(Dispatchers.IO) { service.refreshAccessToken(refreshToken) }
            } catch (ex: Exception) {
                throw IllegalStateException(
                    "OAuth2 Refresh Token retrieval failed from third-party API for $dataSourceId/$userId.",
                    ex,
                )
            }

        val rawResponse = token.rawResponse ?: token.accessToken
        return extractParams(userId, dataSourceId, rawResponse)
    }

    private fun extractParams(
        userId: String,
        dataSourceId: String,
        rawResponse: String,
    ): OAuth2AccessParams =
        when (dataSourceId) {
            dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource.DATA_SOURCE_ID -> {
                dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitAccessTokenResponseExtractor
                    .getAccessParamsFromResponse(
                        userId,
                        dataSourceId,
                        rawResponse,
                    )
            }
            dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataSource.DATA_SOURCE_ID -> {
                dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsAccessTokenResponseExtractor
                    .getAccessParamsFromResponse(
                        userId,
                        dataSourceId,
                        rawResponse,
                    )
            }
            dk.carp.gardener.authentication.core.authorization.devices.dexcom.DexcomDataSource.DATA_SOURCE_ID -> {
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

    // Note: token exchange and refresh use ScribeJava's blocking helpers wrapped in IO dispatcher

    @Suppress("TooGenericExceptionCaught", "UnusedParameter", "UseCheckOrError")
    suspend fun executeRequest(
        uri: Uri,
        dataType: DataCollectionType,
        accessParams: AccessParams,
    ): String {
        val response =
            try {
                httpClient.get(uri.uri) {
                    header("Authorization", "Bearer ${accessParams.extractAccessToken()}")
                    uri.queryParams?.forEach { parameter(it.key, it.value) }
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
        return response.bodyAsText()
    }
}
