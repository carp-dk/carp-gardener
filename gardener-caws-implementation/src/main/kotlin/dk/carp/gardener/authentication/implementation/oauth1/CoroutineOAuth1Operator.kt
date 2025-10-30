package dk.carp.gardener.authentication.implementation.oauth1

import com.github.scribejava.core.model.OAuth1AccessToken
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Response
import com.github.scribejava.core.oauth.OAuth10aService
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1ClientSettings
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1RequestToken
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoroutineOAuth1Operator(
    private val clientSettings: OAuth1ClientSettings,
    private val service: OAuth10aService,
) {
    companion object {
        private const val HTTP_SUCCESS_LOWER_BOUND = 200
        private const val HTTP_SUCCESS_UPPER_BOUND_EXCLUSIVE = 300
    }

    @Suppress("UnusedParameter")
    suspend fun getCompleteAuthorizationUrlForUser(
        stateId: String,
        requestToken: OAuth1RequestToken,
        params: OAuth1AuthorizationRequestParams,
    ): String =
        withContext(Dispatchers.Default) {
            val baseUri =
                service.getAuthorizationUrl(
                    com.github.scribejava.core.model
                        .OAuth1RequestToken(requestToken.requestToken, requestToken.tokenSecret),
                )
            if (clientSettings.clientCallbackUri ==
                null
            ) {
                baseUri
            } else {
                "$baseUri&oauth_callback=${clientSettings.clientCallbackUri}?state=$stateId"
            }
        }

    @Suppress("TooGenericExceptionCaught", "UnusedParameter")
    suspend fun acquireUnsignedRequestToken(params: OAuth1AuthorizationRequestParams): OAuth1RequestToken =
        withContext(Dispatchers.IO) {
            val token =
                try {
                    service.requestToken
                } catch (ex: Exception) {
                    throw IllegalStateException("Failed OAUth1 Unsigned Token request: ${ex.message}", ex)
                }
            OAuth1RequestToken(token.token, token.tokenSecret)
        }

    @Suppress("TooGenericExceptionCaught", "UnusedParameter")
    suspend fun acquireAccessToken(
        userId: String,
        dataSourceId: String,
        requestToken: OAuth1RequestToken,
        verifier: String,
        params: OAuth1AuthorizationRequestParams,
    ): OAuth1AccessParams =
        withContext(Dispatchers.IO) {
            val rawResponse =
                try {
                    service
                        .getAccessToken(
                            com.github.scribejava.core.model
                                .OAuth1RequestToken(requestToken.requestToken, requestToken.tokenSecret),
                            verifier,
                        ).rawResponse
                } catch (ex: Exception) {
                    throw IllegalStateException(
                        "Failed OAUth1 Access Token request for $dataSourceId/$userId: ${ex.message}",
                        ex,
                    )
                }
            // Use local extraction logic (copied from original OAuth1Operator) to avoid accessing private members
            when (dataSourceId) {
                dk.carp.gardener.authentication.core.authorization.devices.garmin.GarminDataSource.DATA_SOURCE_ID -> {
                    dk.carp.gardener.authentication.core.authorization.devices.garmin.GarminAccessTokenResponseExtractor
                        .getAccessParamsFromResponse(userId, dataSourceId, rawResponse)
                }
                else -> throw IllegalArgumentException("DataSourceId $dataSourceId is not valid.")
            }
        }

    @Suppress("TooGenericExceptionCaught", "UnusedParameter", "UseCheckOrError")
    suspend fun executeRequest(
        uri: Uri,
        dataType: DataCollectionType,
        accessParams: AccessParams,
    ): String =
        withContext(Dispatchers.IO) {
            val ap = accessParams as OAuth1AccessParams
            val request = OAuthRequest(com.github.scribejava.core.model.Verb.GET, uri.uri)
            service.signRequest(OAuth1AccessToken(ap.extractAccessToken(), ap.extractTokenSecret()), request)

            val response: Response =
                try {
                    service.execute(request)
                } catch (ex: Exception) {
                    throw IllegalStateException(
                        "Failed OAUth1 http request for ${ap.dataSourceId}/${ap.internalUserId}: ${ex.message}",
                        ex,
                    )
                }

            if (response.code !in HTTP_SUCCESS_LOWER_BOUND until HTTP_SUCCESS_UPPER_BOUND_EXCLUSIVE) {
                error("Failed OAUth1 http request for ${ap.dataSourceId}/${ap.internalUserId}: ${response.body}")
            }
            response.body
        }
}
