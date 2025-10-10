package dk.carp.gardener.authentication.core.authorization.datasource

import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1DataSource
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionPreparationEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionExecutionEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.DataSourceEvent

/**
 * A common superclass for both OAuth1 and OAuth2 data sources.
 */
abstract class DataSource(
    protected val accessParamsService: IAccessParamsService,
    protected val stateService: IAuthorizationStateService,
    protected val eventBus: IEventBus
) : IDataSource {

    init {
        @Suppress("UNCHECKED_CAST")
        eventBus.subscribe(
            subscriber = this::class,
            eventType = DataCollectionPreparationEvent::class,
            dataSourceId = this.getId(),
            handler = { event: DataCollectionPreparationEvent -> prepareDataCollection(event) } as (DataSourceEvent) -> Unit
        )
    }

    /**
     * Creates a new authorization state for the authorization process and returns
     * the completed authorization URL parametrized with the state id
     * where the user should be redirected to.
     *
     * @param userId The ID of the user that wants to authorize.
     * @param dataSourceId The ID of the data source where the authorization needs to be done.
     * @param params Additional parameters that need to be considered while making the authorization URL.
     * @return A new [AuthorizationRequest] object containing the newly created state for the user and the URL.
     *
     * @throws IllegalArgumentException When the user is already authorized with the given data source.
     */
    override fun initiateUserAuthorization(
        userId: String,
        dataSourceId: String,
        params: AuthorizationRequestParams
    ): AuthorizationRequest {
        if (accessParamsService.isUserAlreadyRegistered(userId, dataSourceId)) {
            throw IllegalArgumentException("User $dataSourceId/$userId is already authorized. Request aborted.")
        }

        val state = registerAuthorizationRequestForUser(userId, dataSourceId, params.applicationData)
        val uri = getAuthorizationRedirectUri(state, params)
        return AuthorizationRequest(uri, state)
    }

    /**
     * Returns the current [AccessParams] for the user for the given data source.
     *
     * @param userId The ID of the user.
     *
     * @throws IllegalArgumentException When no access parameters were found for the user
     * for this data source.
     */
    override fun getAccessParametersFor(userId: String): AccessParams {
        return accessParamsService.getCurrentForInternalUserIdAndDataSource(userId, this.getId())
    }

    /**
     * Returns the [AuthorizationType] of the data source,
     * corresponding to the authorization mechanism it uses.
     */
    override fun getAuthorizationType(): AuthorizationType {
        return if (this is OAuth1DataSource) {
            AuthorizationType.OAUTH1
        } else AuthorizationType.OAUTH2
    }

    /**
     * A handler that is executed when a [DataCollectionPreparationEvent] is fired.
     * It collects the information required to execute data collection and publishes
     * the event.
     */
    protected fun prepareDataCollection(event: DataCollectionPreparationEvent) {
        val accessParams = getAccessParametersFor(event.userId)
        val uri = assembleDataCollectionUri(accessParams, event.dataType, event.rawPing)
        publishDataCollectionExecutionEvent(event.dataType, uri, accessParams)
    }

    /**
     * Creates a completed [Uri], which can be used to collect the [dataType].
     *
     * @param accessParams The current access parameters for the data source/user.
     * @param dataType A [DataCollectionType] noting what kind of data should be collected.
     * @param rawPing A Json representation of the notification received from the vendor.
     * @return The completed [Uri].
     */
    protected abstract fun assembleDataCollectionUri(
        accessParams: AccessParams,
        dataType: DataCollectionType,
        rawPing: JsonNode
    ) : Uri

    /**
     * Publishes authorization protocol specific [DataCollectionExecutionEvent].
     *
     * @param dataType What kind of data should be collected.
     * @param dataCollectionUri Where should that data be collected.
     * @param accessParams The parameters needed for authentication.
     */
    protected abstract fun publishDataCollectionExecutionEvent(
        dataType: DataCollectionType,
        dataCollectionUri: Uri,
        accessParams: AccessParams
    )

    /**
     * Creates a new [AuthorizationState] for the given user indicated by the [userId].
     * and for the data source indicated by the [dataSourceId].
     */
    protected abstract fun registerAuthorizationRequestForUser(userId: String, dataSourceId: String, applicationData: String?): AuthorizationState

    /**
     * Creates a completed Authorization redirect URI for the data source.
     *
     * @param state An [AuthorizationState] object associated with the current authorization session.
     * @param params Additional parameters that should be included in the URI.
     * @return A completed URI string customized with the state id and given parameters.
     * where the user should be redirected to.
     */
    protected abstract fun getAuthorizationRedirectUri(state: AuthorizationState, params: AuthorizationRequestParams): String
}