package dk.carp.gardener.authentication.core.authorization.datasource.oauth2

import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams

/**
 * Defines the necessary functions to complete the OAuth2 authorization flow
 * and to refresh the tokens.
 */
interface IOAuth2AuthorizationOperator {

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
    fun getCompleteAuthorizationUrlForState(stateId: String, requestedScopes: String, params: OAuth2AuthorizationRequestParams): String

    /**
     * Retrieves OAuth2 access parameters for the given user noted by [userId] and
     * for the given data source noted by [dataSourceId].
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
    fun retrieveAccessParams(userId: String, dataSourceId: String, authorizationCode: String, params: OAuth2AuthorizationRequestParams): OAuth2AccessParams

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
    fun refreshTokens(userId: String, dataSourceId: String, refreshToken: String, params: OAuth2TokenRefreshParams): OAuth2AccessParams

}