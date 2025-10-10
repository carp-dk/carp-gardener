package dk.carp.gardener.authentication.implementation.oauth1

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuth1AccessToken
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Response
import com.github.scribejava.core.model.Verb
import com.github.scribejava.core.oauth.OAuth10aService
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.IOAuth1AuthorizationOperator
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1ClientSettings
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1RequestToken
import dk.carp.gardener.authentication.core.authorization.devices.garmin.GarminAccessTokenResponseExtractor
import dk.carp.gardener.authentication.core.authorization.devices.garmin.GarminDataSource
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import org.slf4j.LoggerFactory

/**
 * Provides an implementation for [IOAuth1AuthorizationOperator] and [IDataCollectionOperator].
 */
class OAuth1Operator(private val clientSettings: OAuth1ClientSettings) : IOAuth1AuthorizationOperator,
    IDataCollectionOperator {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OAuth1Operator::class.java)
    }

    private val service: OAuth10aService = ServiceBuilder(clientSettings.consumerKey)
        .apiSecret(clientSettings.consumerSecret)
        .build(OAuth1ApiDefinition(clientSettings.requestTokenUri, clientSettings.accessTokenUri, clientSettings.authorizationUri))

    /**
     * Returns a completed authorization URI string that can be used to redirect the user
     * to the vendor's website to authorize.
     *
     * The URI must contain the [stateId], [requestToken] and the additional parameters
     * defined in [params].
     *
     * @param stateId The ID of the state associated with the current authorization session.
     * @param requestToken OAuth1 Unsigned Request Token.
     * @param params Additional parameters to be included in the URI as query parameters.
     *
     * @return URI string.
     */
    override fun getCompleteAuthorizationUrlForUser(
        stateId: String,
        requestToken: OAuth1RequestToken,
        params: OAuth1AuthorizationRequestParams
    ): String {
        val baseUri = service.getAuthorizationUrl(
            com.github.scribejava.core.model.OAuth1RequestToken(
                requestToken.requestToken,
                requestToken.tokenSecret
            )
        )
        return if (clientSettings.clientCallbackUri == null) {
            baseUri
        } else {
            appendCallback(baseUri, clientSettings.clientCallbackUri!!, stateId)
        }
    }

    /**
     * Returns an OAuth1 Unsigned Token pair from the vendor.
     *
     * @param params Additional parameters to be included in the URI as query parameters.
     *
     * @return [OAuth1RequestToken] The Unsigned Token Pair.
     *
     * @throws IllegalStateException When an error is encountered during the communication with the vendor.
     */
    override fun acquireUnsignedRequestToken(params: OAuth1AuthorizationRequestParams): OAuth1RequestToken {
        val token: com.github.scribejava.core.model.OAuth1RequestToken
        try {
            token = service.requestToken
        } catch (ex: Exception) {
            throw IllegalStateException("Failed OAUth1 Unsigned Token request: ${ex.message}")
        }

        LOGGER.info("OAuth1 Unsigned Token successfully received from third-party API.")
        return OAuth1RequestToken(
            token.token,
            token.tokenSecret
        )
    }

    /**
     * Acquires OAuth1 Access parameters.
     *
     * @param userId The ID of the user the access parameters will belong to.
     * @param dataSourceId The ID of the data source that the parameters must be retrieved from.
     * @param requestToken The tokens retrieved from the first step in the OAuth1 protocol.
     * @param verifier The token verifier retrieved from the second step in the OAuth1 protocol.
     * @param params Additional parameters that need to be included in the request.
     *
     * @return [OAuth1AccessParams] for the user and the data source.
     *
     * @throws IllegalStateException When an error is encountered during the communication with the vendor.
     */
    override fun acquireAccessToken(
        userId: String,
        dataSourceId: String,
        requestToken: OAuth1RequestToken,
        verifier: String,
        params: OAuth1AuthorizationRequestParams
    ): OAuth1AccessParams {
        val rawResponse: String
        try {
            rawResponse = service.getAccessToken(
                com.github.scribejava.core.model.OAuth1RequestToken(requestToken.requestToken, requestToken.tokenSecret),
                verifier
            ).rawResponse
        } catch (ex: Exception) {
            throw IllegalStateException("Failed OAUth1 Access Token request for ${dataSourceId}/${userId}: ${ex.message}")
        }

        LOGGER.info("OAuth1 Access token is successfully retrieved from third-party API for $dataSourceId/$userId.")
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
    override fun executeRequest(uri: Uri, dataType: DataCollectionType, accessParams: AccessParams, callback: (String) -> Unit) {
        accessParams as OAuth1AccessParams
        val request = OAuthRequest(Verb.GET, uri.uri)
        service.signRequest(
            OAuth1AccessToken(accessParams.extractAccessToken(), accessParams.extractTokenSecret()),
            request
        )

        val response: Response
        try {
            response = service.execute(request)
        } catch (ex: Exception) {
            throw IllegalStateException("Failed OAUth1 http request for ${accessParams.dataSourceId}/${accessParams.internalUserId}: ${ex.message}")
        }
        if (response.code < 200 || response.code >= 300) {
            throw IllegalStateException("Failed OAUth1 http request for ${accessParams.dataSourceId}/${accessParams.internalUserId}: ${response.body}")
        }

        LOGGER.info("OAuth1 data successfully collected from third-party API for ${accessParams.dataSourceId}/${accessParams.internalUserId}.")
        callback(response.body)
    }

    private fun extractParams(userId: String, dataSourceId: String, rawResponse: String): OAuth1AccessParams {
        return when (dataSourceId) {
            GarminDataSource.DATA_SOURCE_ID -> {
                GarminAccessTokenResponseExtractor.getAccessParamsFromResponse(userId, dataSourceId, rawResponse)
            }
            else -> {
                throw IllegalArgumentException("DataSourceId $dataSourceId is not valid.")
            }
        }
    }

    private fun appendCallback(authorizationUri: String, callbackUri: String, stateId: String): String {
        return "$authorizationUri&oauth_callback=$callbackUri?state=$stateId"
    }

}
