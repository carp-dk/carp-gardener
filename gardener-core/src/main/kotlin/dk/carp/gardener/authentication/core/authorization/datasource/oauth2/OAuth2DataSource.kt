package dk.carp.gardener.authentication.core.authorization.datasource.oauth2

import dk.carp.gardener.authentication.core.authorization.authorizationrequest.AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth2AuthorizationState
import dk.carp.gardener.authentication.core.authorization.datasource.DataSource
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionExecutionEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.DataSourceEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.events.oauth2.OAuth2Event
import dk.carp.gardener.authentication.core.common.util.uri.Uri

/**
 * Base class for any third-party vendor implementation,
 * who uses OAuth2 as authorization protocol.
 */
abstract class OAuth2DataSource(
    eventBus: IEventBus,
    stateService: IAuthorizationStateService,
    accessParamsService: IAccessParamsService,
    protected val clientSettings: OAuth2ClientSettings,
    protected val authorizationOperator: IOAuth2AuthorizationOperator
) : DataSource(accessParamsService, stateService, eventBus) {

  init {
    @Suppress("UNCHECKED_CAST")
    eventBus.subscribe(
      subscriber = this::class,
      eventType = OAuth2Event.AuthorizationCodeAcquired::class,
      dataSourceId = this.getId(),
      handler = { event: OAuth2Event.AuthorizationCodeAcquired -> acquireAccessToken(event) } as (DataSourceEvent) -> Unit
    )
  }

  /**
   * A callback function that is executed when an [OAuth2Event.AuthorizationCodeAcquired] is fired.
   * It retrieves [OAuth2AccessParams] from the vendor.
   *
   * @param event The event object that contains the Authorization Code and additional information.
   *
   * @throws IllegalArgumentException When no state entry is found with the given state stateId.
   * @throws IllegalStateException When the access parameters cannot be retrieved.
   */
  protected fun acquireAccessToken(event: OAuth2Event.AuthorizationCodeAcquired) {
    val state = stateService.getById(event.stateId)
    val accessParams: OAuth2AccessParams = authorizationOperator.retrieveAccessParams(
      userId = state.userId,
      dataSourceId = state.dataSourceId,
      authorizationCode = event.code,
      params = event.params
    )
    accessParams.applicationData = state.applicationData
    accessParamsService.addParams(accessParams)
    eventBus.publish(this::class, OAuth2Event.AccessTokenAcquired(parameters = accessParams, stateId = state.id, dataSourceId = this.getId()))
    stateService.setSuccessfulState(state)
  }

  /**
   * Creates a new [AuthorizationState] for the given user indicated by the [userId].
   * and for the data source indicated by the [dataSourceId].
   */
  override fun registerAuthorizationRequestForUser(userId: String, dataSourceId: String, applicationData: String?): AuthorizationState {
    val state = OAuth2AuthorizationState(
        userId = userId,
        dataSourceId = dataSourceId,
        applicationData = applicationData
    )
    stateService.save(state)
    eventBus.publish(this::class, OAuth2Event.UserEnrollmentRequested(stateId = state.id, dataSourceId = this.getId()))
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
  override fun getAuthorizationRedirectUri(state: AuthorizationState, params: AuthorizationRequestParams): String {
    params as OAuth2AuthorizationRequestParams
    return authorizationOperator.getCompleteAuthorizationUrlForState(
      stateId = state.id,
      requestedScopes = constructScopeStringsForAuthorizationUrl(params.scopes.getList()),
      params = params
    )
  }

  /**
   * Refreshes expired OAuth2 access parameters.
   *
   * @param userId The ID of the user the parameters belong to.
   * @param dataSourceId The ID of the dataSource that must be contacted.
   * @param params Additional parameters that must be included in the request.
   *
   * @return Refreshed [OAuth2AccessParams] for the given user and data source.
   *
   * @throws IllegalArgumentException When no access parameters is found for the user and data source.
   * @throws IllegalStateException When:
   *    - no refresh token is found in the access parameters.
   *    - the token refresh process encountered an error.
   */
  fun refreshAccessParametersForUserAndDevice(
      userId: String,
      dataSourceId: String,
      params: OAuth2TokenRefreshParams
  ): OAuth2AccessParams {
    val currentParams = accessParamsService.getCurrentForInternalUserIdAndDataSource(userId, dataSourceId) as OAuth2AccessParams
    val expiredRefreshToken = currentParams.extractRefreshToken() ?: throw IllegalStateException("Refresh token is not present for $dataSourceId/$userId!")

    val refreshedParams = authorizationOperator.refreshTokens(userId, dataSourceId, expiredRefreshToken, params)
    currentParams.params = refreshedParams.params

    accessParamsService.addParams(currentParams)
    eventBus.publish(this::class, OAuth2Event.AccessParametersRefreshed(userId = userId, dataSourceId = dataSourceId, parameters = currentParams))
    return currentParams
  }

  /**
   * Publishes authorization protocol specific [DataCollectionExecutionEvent].
   * In case of OAuth2, the access parameters are refreshed if
   * they are determined expired.
   *
   * @param dataType What kind of data should be collected.
   * @param dataCollectionUri Where should that data be collected.
   * @param accessParams The parameters needed for authentication.
   *
   * @throws IllegalArgumentException When the tokens are expired and
   * no access parameters is found for the user and data source.
   * @throws IllegalStateException When the tokens are expired and:
   *    - no refresh token is found in the access parameters.
   *    - the token refresh process encountered an error.
   */
  override fun publishDataCollectionExecutionEvent(
    dataType: DataCollectionType,
    dataCollectionUri: Uri,
    accessParams: AccessParams
  ) {
    val tokensAreExpired = (accessParams as OAuth2AccessParams).determineExpiration()
    val refreshedParams = if (tokensAreExpired) {
      refreshAccessParametersForUserAndDevice(
        accessParams.internalUserId,
        accessParams.dataSourceId,
        getEstablishedTokenRefreshParams()
      )
    } else accessParams

    eventBus.publish(this::class, DataCollectionExecutionEvent.OAuth2ExecutionEvent(
      accessParams = refreshedParams,
      clientSettings = clientSettings,
      uri = dataCollectionUri,
      dataIdentifier = dataType
    ))
  }

  /**
   * Returns an [AuthorizationRequestParams] object that already contains
   * vendor specific necessary parameters for the authorization flow.
   */
  override fun getEstablishedAuthorizationRequestParams(): AuthorizationRequestParams {
    return OAuth2AuthorizationRequestParams()
  }

  /**
   * Returns an [OAuth2TokenRefreshParams] object that already contains
   * vendor specific necessary parameters for the OAuth2 token refresh flow.
   */
  protected open fun getEstablishedTokenRefreshParams(): OAuth2TokenRefreshParams {
    return OAuth2TokenRefreshParams()
  }

  /**
   * Constructs a string made out of the [scopes] according to the vendor's specification.
   *
   * @throws IllegalArgumentException When any of the requested [scopes] is not valid or supported.
   */
  protected abstract fun constructScopeStringsForAuthorizationUrl(scopes: List<String>?): String

}
