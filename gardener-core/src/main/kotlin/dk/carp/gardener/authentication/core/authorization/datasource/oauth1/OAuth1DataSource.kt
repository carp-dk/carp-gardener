package dk.carp.gardener.authentication.core.authorization.datasource.oauth1

import dk.carp.gardener.authentication.core.authorization.authorizationrequest.AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth1AuthorizationState
import dk.carp.gardener.authentication.core.authorization.datasource.DataSource
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionExecutionEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.DataSourceEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.events.oauth1.OAuth1Event
import dk.carp.gardener.authentication.core.common.util.uri.Uri

/**
 * Base class for any third-party vendor implementation,
 * who uses OAuth1 as authorization protocol.
 */
abstract class OAuth1DataSource(
    eventBus: IEventBus,
    stateService: IAuthorizationStateService,
    accessParamsService: IAccessParamsService,
    protected val clientSettings: OAuth1ClientSettings,
    protected val authorizationOperator: IOAuth1AuthorizationOperator,
) : DataSource(accessParamsService, stateService, eventBus) {
    init {
        @Suppress("UNCHECKED_CAST")
        eventBus.subscribe(
            subscriber = this::class,
            eventType = OAuth1Event.AuthorizedTokenAcquired::class,
            dataSourceId = this.getId(),
            handler =
            { event: OAuth1Event.AuthorizedTokenAcquired -> acquireAccessToken(event) } as (
                DataSourceEvent,
            ) -> Unit,
        )
    }

    /**
     * A callback function that is executed when an [OAuth1Event.AuthorizedTokenAcquired] is fired.
     * It retrieves [OAuth1AccessParams] from the vendor.
     *
     * @param event The event object that contains the OAuth1 Tokens, verifier and additional information.
     *
     * @throws IllegalArgumentException When no state entry is found with the given state stateId.
     * @throws IllegalStateException When the access parameters cannot be retrieved.
     */
    protected fun acquireAccessToken(event: OAuth1Event.AuthorizedTokenAcquired) {
        val state = stateService.getById(event.stateId)
        state as OAuth1AuthorizationState
        val accessParams: OAuth1AccessParams =
            authorizationOperator.acquireAccessToken(
                userId = state.userId,
                dataSourceId = state.dataSourceId,
                requestToken =
                OAuth1RequestToken(
                    event.requestToken,
                    state.tokenSecret,
                ),
                verifier = event.tokenVerifier,
                params = event.params,
            )
        accessParams.applicationData = state.applicationData
        accessParamsService.addParams(accessParams)
        eventBus.publish(
            this::class,
            OAuth1Event.AccessTokenAcquired(stateId = state.id, params = accessParams, dataSourceId = this.getId()),
        )
        stateService.setSuccessfulState(state)
    }

    /**
     * Creates a new [AuthorizationState] for the given user indicated by the [userId].
     * and for the data source indicated by the [dataSourceId].
     * In case of OAuth1 the retrieval of Unsigned Tokens is performed additionally.
     *
     * @throws IllegalStateException When an error is encountered during the token retrieval.
     */
    override fun registerAuthorizationRequestForUser(
        userId: String,
        dataSourceId: String,
        applicationData: String?,
    ): AuthorizationState {
        val requestToken: OAuth1RequestToken =
            authorizationOperator.acquireUnsignedRequestToken(
                getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams,
            )
        val state =
            OAuth1AuthorizationState(
                userId,
                dataSourceId,
                requestToken.requestToken,
                requestToken.tokenSecret,
                applicationData,
            )
        stateService.save(state)
        eventBus.publish(this::class, OAuth1Event.UserEnrollmentRequested(stateId = state.id, dataSourceId = this.getId()))
        eventBus.publish(
            this::class,
            OAuth1Event.UnauthorizedTokenAcquired(
                stateId = state.id,
                requestToken = requestToken.requestToken,
                tokenSecret = requestToken.tokenSecret,
                dataSourceId = this.getId(),
            ),
        )
        return state
    }

    /**
     * Creates a completed Authorization redirect URI for the data source.
     *
     * @param state An [AuthorizationState] object associated with the current authorization session.
     * @param params Additional parameters that should be included in the URI.
     * @return A completed URI string customized with the state id and given parameters.
     * where the user should be redirected to.
     */
    override fun getAuthorizationRedirectUri(
        state: AuthorizationState,
        params: AuthorizationRequestParams,
    ): String {
        state as OAuth1AuthorizationState
        params as OAuth1AuthorizationRequestParams
        return authorizationOperator.getCompleteAuthorizationUrlForUser(
            stateId = state.id,
            requestToken =
            OAuth1RequestToken(
                state.requestToken,
                state.tokenSecret,
            ),
            params = params,
        )
    }

    /**
     * Publishes authorization protocol specific [DataCollectionExecutionEvent].
     *
     * @param dataType What kind of data should be collected.
     * @param dataCollectionUri Where should that data be collected.
     * @param accessParams The parameters needed for authentication.
     */
    override fun publishDataCollectionExecutionEvent(
        dataType: DataCollectionType,
        dataCollectionUri: Uri,
        accessParams: AccessParams,
    ) {
        eventBus.publish(
            this::class,
            DataCollectionExecutionEvent.OAuth1ExecutionEvent(
                accessParams = accessParams as OAuth1AccessParams,
                clientSettings = clientSettings,
                uri = dataCollectionUri,
                dataIdentifier = dataType,
            ),
        )
    }

    /**
     * Returns an [AuthorizationRequestParams] object that already contains
     * vendor specific necessary parameters for the authorization flow.
     */
    override fun getEstablishedAuthorizationRequestParams(): AuthorizationRequestParams = OAuth1AuthorizationRequestParams()
}
