package com.example.authenticationmodule.core.common.events.oauth2

import com.example.authenticationmodule.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import com.example.authenticationmodule.core.authorization.authorizationstate.AuthorizationState
import com.example.authenticationmodule.core.common.accessparams.OAuth2AccessParams
import com.example.authenticationmodule.core.common.events.eventbus.DataSourceEvent

/**
 * OAuth1 authorization flow specific [DataSourceEvent] definitions.
 */
sealed class OAuth2Event : DataSourceEvent() {

    /**
     * Fired when a user started an OAuth1 authorization process.
     *
     * @param stateId ID the the [AuthorizationState] that is associated with this authorization session.
     * @param dataSourceId ID of the data source.
     */
    data class UserEnrollmentRequested(val stateId: String, override val dataSourceId: String): OAuth2Event()

    /**
     * Fired when the application received an OAuth2 Authorization Code.
     *
     * @param code OAuth2 Authorization Code.
     * @param params Additional parameters that need to be present when making an Access Token request with the [code].
     * @param stateId ID the the [AuthorizationState] that is associated with this authorization session.
     * @param dataSourceId ID of the data source.
     */
    data class AuthorizationCodeAcquired(val code: String, val params: OAuth2AuthorizationRequestParams, val stateId: String, override val dataSourceId: String): OAuth2Event()

    /**
     * Fired when the application received an OAuth2 Access token.
     *
     * @param parameters OAuth2 access parameters.
     * @param stateId ID the the [AuthorizationState] that is associated with this authorization session.
     * @param dataSourceId ID of the data source.
     */
    data class AccessTokenAcquired(val parameters: OAuth2AccessParams, val stateId: String, override val dataSourceId: String): OAuth2Event()

    /**
     * Fired when the application received an OAuth2 Access token response
     * from a token refresh request.
     *
     * @param parameters OAuth2 access parameters.
     * @param dataSourceId ID of the data source.
     * @param userId The ID of the user the parameters belong to.
     */
    data class AccessParametersRefreshed(val userId: String, override val dataSourceId: String, val parameters: OAuth2AccessParams): OAuth2Event()
}