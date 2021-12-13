package com.example.authenticationmodule.core.authorization.datasource.oauth1

import com.example.authenticationmodule.core.common.accessparams.OAuth1AccessParams
import com.example.authenticationmodule.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams

/**
 * Defines the necessary functions to complete the OAuth1 authorization flow.
 */
interface IOAuth1AuthorizationOperator {

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
    fun getCompleteAuthorizationUrlForUser(stateId: String, requestToken: OAuth1RequestToken, params: OAuth1AuthorizationRequestParams): String

    /**
     * Returns an OAuth1 Unsigned Token pair from the vendor.
     *
     * @param params Additional parameters to be included in the URI as query parameters.
     *
     * @return [OAuth1RequestToken] The Unsigned Token Pair.
     *
     * @throws IllegalStateException When an error is encountered during the communication with the vendor.
     */
    fun acquireUnsignedRequestToken(params: OAuth1AuthorizationRequestParams): OAuth1RequestToken

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
    fun acquireAccessToken(userId: String, dataSourceId: String, requestToken: OAuth1RequestToken, verifier: String, params: OAuth1AuthorizationRequestParams): OAuth1AccessParams

}