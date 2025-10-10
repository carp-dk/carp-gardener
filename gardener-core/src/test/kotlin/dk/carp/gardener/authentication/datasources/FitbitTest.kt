package dk.carp.gardener.authentication.datasources

import dk.carp.gardener.authentication.base.OAuth2Test
import dk.carp.gardener.authentication.base.TestProperties
import dk.carp.gardener.authentication.base.TestUtil
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.AuthorizationType
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataCollectionType
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.collection.data.TransformedData
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionExecutionEvent
import dk.carp.gardener.authentication.core.common.events.datacollection.DataSuccessfullyCollectedEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IntegrationEvent
import dk.carp.gardener.authentication.core.common.events.oauth2.OAuth2Event
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import org.mockito.kotlin.*
import kotlin.test.*

/**
 * Integration tests for [FitbitDataSource].
 */
open class FitbitTest : OAuth2Test() {

    private val fitbitActivitiesPing: JsonNode
            = ConfiguredObjectMapper.instance.readTree(TestUtil.getResourceAsText("/fitbit/fitbit_activities_ping.json"))
    private val fitbitExpiredAccessParams: JsonNode
            = ConfiguredObjectMapper.instance.readTree(TestUtil.getResourceAsText("/fitbit/fitbit_expired_access_params.json"))
    private val fitbitInvalidPing: JsonNode
            = ConfiguredObjectMapper.instance.readTree(TestUtil.getResourceAsText("/fitbit/fitbit_invalid_ping.json"))

    @Test
    fun returnsTheCorrectId() {
        val id = fitbitDataSource.getId()
        assertEquals(FitbitDataSource.DATA_SOURCE_ID, id)
    }

    @Test
    fun returnsTheCorrectAuthorizationType() {
        val type = fitbitDataSource.getAuthorizationType()
        assertEquals(AuthorizationType.OAUTH2, type)
    }

    @Test
    fun theEstablishedRequestParamsAreCorrect() {
        val params = fitbitDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
        assertTrue { params.additionalParamsForGrants.getMap().isEmpty() }
        assertTrue { params.additionalParamsForTokens.getMap().isEmpty() }
        assertTrue { params.scopes.getList().isEmpty() }
    }

    @Test
    fun newAuthorizationCreatesNewState() {
        val userId = "newAuthorizationCreatesNewState"
        val dataSourceId = FitbitDataSource.DATA_SOURCE_ID
        val params = fitbitDataSource.getEstablishedAuthorizationRequestParams()

        val request = fitbitDataSource.initiateUserAuthorization(userId, dataSourceId, params)

        // Assert that the authorization state is saved
        val state = authorizationStateService.getById(request.authorizationState.id)
        assertEquals(request.authorizationState, state)
        assertEquals(userId, state.userId)
        assertEquals(dataSourceId, state.dataSourceId)
    }

    @Test
    fun authorizationStateSavesApplicationDataIfPresent() {
        val userId = "authorizationStateSavesApplicationDataIfPresent"
        val dataSourceId = FitbitDataSource.DATA_SOURCE_ID
        val params = fitbitDataSource.getEstablishedAuthorizationRequestParams()
        val applicationData = "appData"
        params.applicationData = applicationData

        val request = fitbitDataSource.initiateUserAuthorization(userId, dataSourceId, params)

        // Assert that the authorization state is saved
        val state = authorizationStateService.getById(request.authorizationState.id)
        assertEquals(applicationData, state.applicationData)
    }

    @Test
    fun theSameUserCannotAuthorizeTwiceToTheSameDataSource() {
        val userId = "theSameUserCannotAuthorizeTwiceToTheSameDataSource"
        val dataSourceId = FitbitDataSource.DATA_SOURCE_ID
        val params = fitbitDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        // Imitate a previous successful authorization
        oauth2Operator.retrieveAccessParams(userId, dataSourceId, "authorization_code", params)

        assertFailsWith<IllegalArgumentException> { fitbitDataSource.initiateUserAuthorization(userId, dataSourceId, params) }
    }

    @Test
    fun accessParamsCanBeRetrieved() {
        val userId = "accessParamsCanBeRetrieved"
        val dataSourceId = FitbitDataSource.DATA_SOURCE_ID
        val params = fitbitDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        val actualAccessParams = oauth2Operator.retrieveAccessParams(userId, dataSourceId, "authorization_code", params)
        oauth2Operator.retrieveAccessParams("dummy1", dataSourceId, "authorization_code", params)
        oauth2Operator.retrieveAccessParams("dummy2", dataSourceId, "authorization_code", params)

        val accessParams = fitbitDataSource.getAccessParametersFor(userId)
        assertEquals(actualAccessParams.id, accessParams.id)
    }

    @Test
    fun failsWhenThereAreNoAccessParams() {
        val userId = "failsWhenThereAreNoAccessParams"
        val dataSourceId = FitbitDataSource.DATA_SOURCE_ID
        val params = fitbitDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        oauth2Operator.retrieveAccessParams("dummy1", dataSourceId, "authorization_code", params)
        oauth2Operator.retrieveAccessParams("dummy2", dataSourceId, "authorization_code", params)

        assertFailsWith<IllegalArgumentException> { fitbitDataSource.getAccessParametersFor(userId) }
    }

    @Test
    fun dataCollectionPreparationEventCanBeExtractedFromValidPing() {
        val notificationText = fitbitActivitiesPing.toString()
        val events = fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertTrue { events.size == 1 }
        val event = events[0]
        assertEquals(FitbitDataSource.DATA_SOURCE_ID, event.dataSourceId)
        assertEquals(FitbitDataCollectionType.ACTIVITIES, event.dataType)
    }

    @Test
    fun dataCollectionPreparationEventFailsWithInvalidPing() {
        val notificationText = "fail"
        assertFailsWith<IllegalArgumentException> { fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText) }
    }

    @Test
    fun dataCollectionPreparationEventFailsWithInvalidDataType() {
        val notificationText = fitbitInvalidPing.toString()
        assertFailsWith<IllegalArgumentException> { fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText) }
    }

    @Test
    fun fitbitAccessParamsAreRetrievedWhenAuthorizationCodeIsReceived() {
        val userId = "fitbitAccessParamsAreRetrievedWhenAuthorizationCodeIsReceived"
        val dataSourceId = FitbitDataSource.DATA_SOURCE_ID
        val params = fitbitDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        // Save a state for the authorization
        val request = fitbitDataSource.initiateUserAuthorization(userId, dataSourceId, params)
        // Publish an authorization code event
        spyingEventBus.publish(this::class, OAuth2Event.AuthorizationCodeAcquired(
            code = "code",
            stateId = request.authorizationState.id,
            dataSourceId = dataSourceId,
            params = params
        ))

        // Verify that the user has access params saved
        val savedParams = accessParamService.getCurrentForInternalUserIdAndDataSource(userId, dataSourceId)
        assertEquals(userId, savedParams.internalUserId)
        assertEquals(TestProperties.FITBIT_TEST_USER_EXTERNAL_ID, savedParams.externalUserId)
        assertEquals(FitbitDataSource.DATA_SOURCE_ID, savedParams.dataSourceId)
        assertNull(savedParams.applicationData)
    }

    @Test
    fun applicationDataIsSavedWithAccessParams() {
        val userId = "applicationDataIsSavedWithAccessParams"
        val dataSourceId = FitbitDataSource.DATA_SOURCE_ID
        val params = fitbitDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
        val appData = "appData"
        params.applicationData = appData

        // Save a state for the authorization
        val request = fitbitDataSource.initiateUserAuthorization(userId, dataSourceId, params)
        // Publish an authorization code event
        spyingEventBus.publish(this::class, OAuth2Event.AuthorizationCodeAcquired(
            code = "code",
            stateId = request.authorizationState.id,
            dataSourceId = dataSourceId,
            params = params
        ))

        // Verify that the user has access params saved
        val savedParams = accessParamService.getCurrentForInternalUserIdAndDataSource(userId, dataSourceId)
        assertEquals(appData, savedParams.applicationData)
    }

    @Test
    fun dataCollectionExecutionEventIsSent() {
        val userId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
        val params = registerFitbitUserFor(userId)

        // Fire preparation events
        val notificationText = fitbitActivitiesPing.toString()
        val events = fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = argumentCaptor<IntegrationEvent>()
        verify(spyingEventBus, atLeast(2)).publish(any(), argumentCaptor.capture())

        // Check if an execution event was fired
        val executedEvent = argumentCaptor.allValues.filterIsInstance<DataCollectionExecutionEvent>().firstOrNull()
        assertNotNull(executedEvent)
        executedEvent as DataCollectionExecutionEvent.OAuth2ExecutionEvent
        assertEquals(params, executedEvent.accessParams)
        assertEquals(FitbitDataCollectionType.ACTIVITIES, executedEvent.dataIdentifier as FitbitDataCollectionType)
    }

    @Test
    fun accessParamsGetRefreshedWhenExpired() {
        // Save expired user access params
        val params =  OAuth2AccessParams(
            internalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID,
            dataSourceId = FitbitDataSource.DATA_SOURCE_ID,
            params = fitbitExpiredAccessParams,
            externalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
        )
        accessParamService.addParams(params)

        // Fire preparation events
        val notificationText = fitbitActivitiesPing.toString()
        val events = fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = argumentCaptor<IntegrationEvent>()
        verify(spyingEventBus, atLeast(3)).publish(any(), argumentCaptor.capture())

        // Check if a refreshment event was fired
        val executedEvent = argumentCaptor.allValues.filterIsInstance<OAuth2Event.AccessParametersRefreshed>().firstOrNull()
        assertNotNull(executedEvent)
    }

    @Test
    fun accessParamsAreNotRefreshedWhenTheyAreNotExpired() {
        val userId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
        registerFitbitUserFor(userId)

        // Fire preparation events
        val notificationText = fitbitActivitiesPing.toString()
        val events = fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = argumentCaptor<IntegrationEvent>()
        verify(spyingEventBus, atLeast(2)).publish(any(), argumentCaptor.capture())

        // Check if a refreshment event was fired
        val executedEvent = argumentCaptor.allValues.filterIsInstance<OAuth2Event.AccessParametersRefreshed>().firstOrNull()
        assertNull(executedEvent)
    }

    @Test
    fun successfulDataCollectionEventIsFiredUponSuccessfulCollection() {
        val userId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
        registerFitbitUserFor(userId)

        // Fire preparation events
        val notificationText = fitbitActivitiesPing.toString()
        val events = fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = argumentCaptor<IntegrationEvent>()
        verify(spyingEventBus, atLeast(3)).publish(any(), argumentCaptor.capture())

        // Check if a data collected event is fired
        val executedEvent = argumentCaptor.allValues.filterIsInstance<DataSuccessfullyCollectedEvent>().firstOrNull()
        assertNotNull(executedEvent)
        assertEquals(userId, executedEvent.userId)
        assertEquals(FitbitDataSource.DATA_SOURCE_ID, executedEvent.dataSourceId)
        assertEquals(FitbitDataCollectionType.ACTIVITIES, executedEvent.dataType as FitbitDataCollectionType)
    }

    @Test
    fun theRightDataTransformerGetsCalled() {
        val userId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
        registerFitbitUserFor(userId)

        // Fire preparation events
        val notificationText = fitbitActivitiesPing.toString()
        val events = fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = argumentCaptor<ThirdPartyData>()
        verify(spyingFitbitTransformer, times(1)).transformActivities(argumentCaptor.capture())

        // Check if a data collected event is fired
        val transformerArgument = argumentCaptor.allValues.firstOrNull()
        assertNotNull(transformerArgument)
        assertEquals(userId, transformerArgument.userId)
        assertEquals(FitbitDataSource.DATA_SOURCE_ID, transformerArgument.dataSourceId)
        assertEquals(FitbitDataCollectionType.ACTIVITIES, transformerArgument.dataIdentifier as FitbitDataCollectionType)
    }

    @Test
    fun rightTransformedDataGetsPublished() {
        val userId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
        registerFitbitUserFor(userId)

        // Fire preparation events
        val notificationText = fitbitActivitiesPing.toString()
        val events = fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the published data
        val argumentCaptor = argumentCaptor<TransformedData>()
        verify(spyingPublisher, times(1)).publishCollectedData(argumentCaptor.capture())

        // Check if a data collected event is fired
        val publishedData = argumentCaptor.allValues.firstOrNull()
        assertNotNull(publishedData)
        assertEquals(userId, publishedData.userId)
        assertEquals(FitbitDataSource.DATA_SOURCE_ID, publishedData.dataSourceId)
        assertEquals(FitbitDataCollectionType.ACTIVITIES, publishedData.dataType as FitbitDataCollectionType)
    }

    private fun registerFitbitUserFor(userId: String): OAuth2AccessParams {
        val dataSourceId = FitbitDataSource.DATA_SOURCE_ID
        return oauth2Operator.retrieveAccessParams(
            userId,
            dataSourceId,
            "authorization_code",
            fitbitDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
        )
    }

}