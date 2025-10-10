package dk.carp.gardener.authentication.core.common.events.oauth1

import dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import dk.carp.gardener.authentication.core.common.events.eventbus.DataSourceEvent
import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState

/**
 * OAuth1 authorization flow specific [DataSourceEvent] definitions.
 */
sealed class OAuth1Event: DataSourceEvent() {

    /**
     * Fired when a user started an OAuth1 authorization process.
     *
     * @param stateId ID the the [AuthorizationState] that is associated with this authorization session.
     * @param dataSourceId ID of the data source.
     */
    data class UserEnrollmentRequested(val stateId: String, override val dataSourceId: String): OAuth1Event()

    /**
     * Fired when the application received an OAuth1 Unauthorized Token.
     *
     * @param stateId ID the [AuthorizationState] that is associated with this authorization session.
     * @param dataSourceId ID of the data source.
     * @param requestToken OAuth1 token.
     * @param tokenSecret OAuth1 token secret.
     */
    data class UnauthorizedTokenAcquired(val stateId: String, val requestToken: String, val tokenSecret: String, override val dataSourceId: String): OAuth1Event()

    /**
     * Fired when the application received an OAuth1 Authorized Token.
     *
     * @param stateId ID the the [AuthorizationState] that is associated with this authorization session.
     * @param dataSourceId ID of the data source.
     * @param requestToken OAuth1 token.
     * @param tokenVerifier OAuth1 token verifier.
     * @param params Additional params that must be included when making an Access Token request.
     */
    data class AuthorizedTokenAcquired(val stateId: String, val requestToken: String, val tokenVerifier: String, val params: OAuth1AuthorizationRequestParams, override val dataSourceId: String): OAuth1Event()

    /**
     * Fired when the application received an OAuth1 Access Token.
     *
     * @param stateId ID the the [AuthorizationState] that is associated with this authorization session.
     * @param dataSourceId ID of the data source.
     * @param params OAuth1 access parameters.
     */
    data class AccessTokenAcquired(val stateId: String, val params: OAuth1AccessParams, override val dataSourceId: String): OAuth1Event()
}