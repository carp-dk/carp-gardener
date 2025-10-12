package dk.carp.gardener.authentication.datasources

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.base.OAuth2Test
import dk.carp.gardener.authentication.base.TestProperties
import dk.carp.gardener.authentication.base.TestUtil.Companion.getResourceAsText
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.AuthorizationType
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataCollectionType
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataSource
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.collection.data.TransformedData
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionExecutionEvent
import dk.carp.gardener.authentication.core.common.events.datacollection.DataSuccessfullyCollectedEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IntegrationEvent
import dk.carp.gardener.authentication.core.common.events.oauth2.OAuth2Event
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for [WithingsDataSource].
 */
class WithingsTest : OAuth2Test() {
    private val withingsActivitiesPing: String =
        getResourceAsText("/withings/withings_activities_ping.txt")
    private val withingsExpiredAccessParams: JsonNode =
        ConfiguredObjectMapper.instance.readTree(getResourceAsText("/withings/withings_expired_access_params.json"))
    private val withingsInvalidPing: String =
        getResourceAsText("/withings/withings_invalid_ping.txt")

    @Test
    fun returnsTheCorrectId() {
        val id = withingsDataSource.getId()
        assertEquals(WithingsDataSource.DATA_SOURCE_ID, id)
    }

    @Test
    fun returnsTheCorrectAuthorizationType() {
        val type = withingsDataSource.getAuthorizationType()
        assertEquals(AuthorizationType.OAUTH2, type)
    }

    @Test
    fun theEstablishedRequestParamsAreCorrect() {
        val params = withingsDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
        assertTrue { params.additionalParamsForGrants.getMap().isEmpty() }
        assertTrue { params.additionalParamsForTokens.getMap().size == 1 }
        assertTrue { params.additionalParamsForTokens.getMap()["action"] != null }
        assertEquals("requesttoken", params.additionalParamsForTokens.getMap()["action"])
        assertTrue { params.scopes.getList().isEmpty() }
    }

    @Test
    fun newAuthorizationCreatesNewState() {
        val userId = "newAuthorizationCreatesNewState"
        val dataSourceId = WithingsDataSource.DATA_SOURCE_ID
        val params = withingsDataSource.getEstablishedAuthorizationRequestParams()

        val request = withingsDataSource.initiateUserAuthorization(userId, dataSourceId, params)

        // Assert that the authorization state is saved
        val state = authorizationStateService.getById(request.authorizationState.id)
        assertEquals(request.authorizationState, state)
        assertEquals(userId, state.userId)
        assertEquals(dataSourceId, state.dataSourceId)
    }

    @Test
    fun authorizationStateSavesApplicationDataIfPresent() {
        val userId = "authorizationStateSavesApplicationDataIfPresent"
        val dataSourceId = WithingsDataSource.DATA_SOURCE_ID
        val params = withingsDataSource.getEstablishedAuthorizationRequestParams()
        val applicationData = "appData"
        params.applicationData = applicationData

        val request = withingsDataSource.initiateUserAuthorization(userId, dataSourceId, params)

        // Assert that the authorization state is saved
        val state = authorizationStateService.getById(request.authorizationState.id)
        assertEquals(applicationData, state.applicationData)
    }

    @Test
    fun theSameUserCannotAuthorizeTwiceToTheSameDataSource() {
        val userId = "theSameUserCannotAuthorizeTwiceToTheSameDataSource"
        val dataSourceId = WithingsDataSource.DATA_SOURCE_ID
        val params = withingsDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        // Imitate a previous successful authorization
        oauth2Operator.retrieveAccessParams(userId, dataSourceId, "authorization_code", params)

        assertFailsWith<IllegalArgumentException> { fitbitDataSource.initiateUserAuthorization(userId, dataSourceId, params) }
    }

    @Test
    fun accessParamsCanBeRetrieved() {
        val userId = "accessParamsCanBeRetrieved"
        val dataSourceId = WithingsDataSource.DATA_SOURCE_ID
        val params = withingsDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        val actualAccessParams = oauth2Operator.retrieveAccessParams(userId, dataSourceId, "authorization_code", params)
        oauth2Operator.retrieveAccessParams("dummy1", dataSourceId, "authorization_code", params)
        oauth2Operator.retrieveAccessParams("dummy2", dataSourceId, "authorization_code", params)

        val accessParams = withingsDataSource.getAccessParametersFor(userId)
        assertEquals(actualAccessParams.id, accessParams.id)
    }

    @Test
    fun dataCollectionPreparationEventCanBeExtractedFromValidPing() {
        val notificationText = withingsActivitiesPing
        val events = withingsDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertTrue { events.size == 1 }
        val event = events[0]
        assertEquals(WithingsDataSource.DATA_SOURCE_ID, event.dataSourceId)
        assertEquals(WithingsDataCollectionType.DAILY_ACTIVITY, event.dataType)
    }

    @Test
    fun dataCollectionPreparationEventFailsWithInvalidPing() {
        val notificationText = "fail"
        assertFailsWith<IllegalArgumentException> {
            fitbitDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        }
    }

    @Test
    fun dataCollectionPreparationEventFailsWithInvalidDataType() {
        val notificationText = withingsInvalidPing
        assertFailsWith<IllegalArgumentException> {
            withingsDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        }
    }

    @Test
    fun withingsAccessParamsAreRetrievedWhenAuthorizationCodeIsReceived() {
        val userId = "withingsAccessParamsAreRetrievedWhenAuthorizationCodeIsReceived"
        val dataSourceId = WithingsDataSource.DATA_SOURCE_ID
        val params = withingsDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        // Save a state for the authorization
        val request = withingsDataSource.initiateUserAuthorization(userId, dataSourceId, params)
        // Publish an authorization code event
        spyingEventBus.publish(
            this::class,
            OAuth2Event.AuthorizationCodeAcquired(
                code = "code",
                stateId = request.authorizationState.id,
                dataSourceId = dataSourceId,
                params = params,
            ),
        )

        // Verify that the user has access params saved
        val savedParams = accessParamService.getCurrentForInternalUserIdAndDataSource(userId, dataSourceId)
        assertEquals(userId, savedParams.internalUserId)
        assertEquals(TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID, savedParams.externalUserId)
        assertEquals(WithingsDataSource.DATA_SOURCE_ID, savedParams.dataSourceId)
    }

    @Test
    fun applicationDataIsSavedWithAccessParams() {
        val userId = "applicationDataIsSavedWithAccessParams"
        val dataSourceId = WithingsDataSource.DATA_SOURCE_ID
        val params = withingsDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
        val appData = "appData"
        params.applicationData = appData

        // Save a state for the authorization
        val request = withingsDataSource.initiateUserAuthorization(userId, dataSourceId, params)
        // Publish an authorization code event
        spyingEventBus.publish(
            this::class,
            OAuth2Event.AuthorizationCodeAcquired(
                code = "code",
                stateId = request.authorizationState.id,
                dataSourceId = dataSourceId,
                params = params,
            ),
        )

        // Verify that the user has access params saved
        val savedParams = accessParamService.getCurrentForInternalUserIdAndDataSource(userId, dataSourceId)
        assertEquals(appData, savedParams.applicationData)
    }

    @Test
    fun dataCollectionExecutionEventIsSent() {
        val userId = TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID
        val params = registerWithingsUserFor(userId)

        // Fire preparation events
        val notificationText = withingsActivitiesPing
        val events = withingsDataSource.getDataCollectionPreparationEventFromPing(notificationText)
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
        assertEquals(WithingsDataCollectionType.DAILY_ACTIVITY, executedEvent.dataIdentifier as WithingsDataCollectionType)
    }

    @Test
    fun accessParamsGetRefreshedWhenExpired() {
        // Save expired user access params
        val params =
            OAuth2AccessParams(
                internalUserId = TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID,
                dataSourceId = WithingsDataSource.DATA_SOURCE_ID,
                params = withingsExpiredAccessParams,
                externalUserId = TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID,
            )
        accessParamService.addParams(params)

        // Fire preparation events
        val notificationText = withingsActivitiesPing
        val events = withingsDataSource.getDataCollectionPreparationEventFromPing(notificationText)
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
        val userId = TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID
        registerWithingsUserFor(userId)

        // Fire preparation events
        val notificationText = withingsActivitiesPing
        val events = withingsDataSource.getDataCollectionPreparationEventFromPing(notificationText)
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
        val userId = TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID
        registerWithingsUserFor(userId)

        // Fire preparation events
        val notificationText = withingsActivitiesPing
        val events = withingsDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = argumentCaptor<IntegrationEvent>()
        verify(spyingEventBus, atLeast(3)).publish(any(), argumentCaptor.capture())

        // Check if a data collected event is fired
        val executedEvent = argumentCaptor.allValues.filterIsInstance<DataSuccessfullyCollectedEvent>().firstOrNull()
        assertNotNull(executedEvent)
        assertEquals(userId, executedEvent.userId)
        assertEquals(WithingsDataSource.DATA_SOURCE_ID, executedEvent.dataSourceId)
        assertEquals(WithingsDataCollectionType.DAILY_ACTIVITY, executedEvent.dataType as WithingsDataCollectionType)
    }

    @Test
    fun theRightDataTransformerGetsCalled() {
        val userId = TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID
        registerWithingsUserFor(userId)

        // Fire preparation events
        val notificationText = withingsActivitiesPing
        val events = withingsDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = argumentCaptor<ThirdPartyData>()
        verify(spyingWithingsTransformer, times(1)).transformActivities(argumentCaptor.capture())

        // Check if a data collected event is fired
        val transformerArgument = argumentCaptor.allValues.firstOrNull()
        assertNotNull(transformerArgument)
        assertEquals(userId, transformerArgument.userId)
        assertEquals(WithingsDataSource.DATA_SOURCE_ID, transformerArgument.dataSourceId)
        assertEquals(WithingsDataCollectionType.DAILY_ACTIVITY, transformerArgument.dataIdentifier as WithingsDataCollectionType)
    }

    @Test
    fun rightTransformedDataGetsPublished() {
        val userId = TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID
        registerWithingsUserFor(userId)

        // Fire preparation events
        val notificationText = withingsActivitiesPing
        val events = withingsDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the published data
        val argumentCaptor = argumentCaptor<TransformedData>()
        verify(spyingPublisher, times(1)).publishCollectedData(argumentCaptor.capture())

        // Check if a data collected event is fired
        val publishedData = argumentCaptor.allValues.firstOrNull()
        assertNotNull(publishedData)
        assertEquals(userId, publishedData.userId)
        assertEquals(WithingsDataSource.DATA_SOURCE_ID, publishedData.dataSourceId)
        assertEquals(WithingsDataCollectionType.DAILY_ACTIVITY, publishedData.dataType as WithingsDataCollectionType)
    }

    private fun registerWithingsUserFor(userId: String): OAuth2AccessParams {
        val dataSourceId = WithingsDataSource.DATA_SOURCE_ID
        return oauth2Operator.retrieveAccessParams(
            userId,
            dataSourceId,
            "authorization_code",
            withingsDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams,
        )
    }
}
