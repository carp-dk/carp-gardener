package com.example.authenticationmodule.datasources

import com.example.authenticationmodule.base.OAuth1Test
import com.example.authenticationmodule.base.TestProperties
import com.example.authenticationmodule.base.TestUtil.Companion.getResourceAsText
import com.example.authenticationmodule.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import com.example.authenticationmodule.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import com.example.authenticationmodule.core.authorization.datasource.AuthorizationType
import com.example.authenticationmodule.core.authorization.datasource.oauth1.OAuth1RequestToken
import com.example.authenticationmodule.core.authorization.devices.fitbit.FitbitDataSource
import com.example.authenticationmodule.core.authorization.devices.garmin.GarminDataCollectionType
import com.example.authenticationmodule.core.authorization.devices.garmin.GarminDataSource
import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.collection.data.TransformedData
import com.example.authenticationmodule.core.common.accessparams.OAuth1AccessParams
import com.example.authenticationmodule.core.common.events.datacollection.DataCollectionExecutionEvent
import com.example.authenticationmodule.core.common.events.datacollection.DataSuccessfullyCollectedEvent
import com.example.authenticationmodule.core.common.events.eventbus.IntegrationEvent
import com.example.authenticationmodule.core.common.events.oauth1.OAuth1Event
import com.example.authenticationmodule.core.common.events.oauth2.OAuth2Event
import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlin.test.*

/**
 * Integration tests for [GarminDataSource].
 */
class GarminTest : OAuth1Test() {

    private val garminStressPing: JsonNode
            = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/garmin/garmin_stress_ping.json"))
    private val garminInvalidPing: JsonNode
            = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/garmin/garmin_invalid_ping.json"))

    @Test
    fun returnsTheCorrectId() {
        val id = garminDataSource.getId()
        assertEquals(GarminDataSource.DATA_SOURCE_ID, id)
    }

    @Test
    fun returnsTheCorrectAuthorizationType() {
        val type = garminDataSource.getAuthorizationType()
        assertEquals(AuthorizationType.OAUTH1, type)
    }

    @Test
    fun theEstablishedRequestParamsAreCorrect() {
        val params = garminDataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams
        assertTrue { params.additionalParamsForGrants.getMap().isEmpty() }
        assertTrue { params.additionalParamsForTokens.getMap().isEmpty() }
        assertTrue { params.additionalParamsForUnsignedToken.getMap().isEmpty() }
    }

    @Test
    fun newAuthorizationCreatesNewState() {
        val userId = "newAuthorizationCreatesNewState"
        val dataSourceId = GarminDataSource.DATA_SOURCE_ID
        val params = garminDataSource.getEstablishedAuthorizationRequestParams()

        val request = garminDataSource.initiateUserAuthorization(userId, dataSourceId, params)

        // Assert that the authorization state is saved
        val state = authorizationStateService.getById(request.authorizationState.id)
        assertEquals(request.authorizationState, state)
        assertEquals(userId, state.userId)
        assertEquals(dataSourceId, state.dataSourceId)
    }

    @Test
    fun authorizationStateSavesApplicationDataIfPresent() {
        val userId = "authorizationStateSavesApplicationDataIfPresent"
        val dataSourceId = GarminDataSource.DATA_SOURCE_ID
        val params = garminDataSource.getEstablishedAuthorizationRequestParams()
        val applicationData = "appData"
        params.applicationData = applicationData

        val request = garminDataSource.initiateUserAuthorization(userId, dataSourceId, params)

        // Assert that the authorization state is saved
        val state = authorizationStateService.getById(request.authorizationState.id)
        assertEquals(applicationData, state.applicationData)
    }

    @Test
    fun theSameUserCannotAuthorizeTwiceToTheSameDataSource() {
        val userId = "theSameUserCannotAuthorizeTwiceToTheSameDataSource"
        val dataSourceId = GarminDataSource.DATA_SOURCE_ID
        val params = garminDataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams

        // Imitate a previous successful authorization
        oauth1Operator.acquireAccessToken(userId, dataSourceId, OAuth1RequestToken("", ""), "", params)

        assertFailsWith<IllegalArgumentException> { garminDataSource.initiateUserAuthorization(userId, dataSourceId, params) }
    }

    @Test
    fun accessParamsCanBeRetrieved() {
        val userId = "accessParamsCanBeRetrieved"
        val dataSourceId = GarminDataSource.DATA_SOURCE_ID
        val params = garminDataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams

        val actualAccessParams = oauth1Operator.acquireAccessToken(userId, dataSourceId, OAuth1RequestToken("", ""), "", params)
        oauth1Operator.acquireAccessToken("dummy1", dataSourceId, OAuth1RequestToken("", ""), "", params)
        oauth1Operator.acquireAccessToken("dummy1", dataSourceId, OAuth1RequestToken("", ""), "", params)

        val accessParams = garminDataSource.getAccessParametersFor(userId)
        assertEquals(actualAccessParams.id, accessParams.id)
    }

    @Test
    fun failsWhenThereAreNoAccessParams() {
        val userId = "failsWhenThereAreNoAccessParams"
        val dataSourceId = GarminDataSource.DATA_SOURCE_ID
        val params = garminDataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams

        oauth1Operator.acquireAccessToken("dummy1", dataSourceId, OAuth1RequestToken("", ""), "", params)
        oauth1Operator.acquireAccessToken("dummy1", dataSourceId, OAuth1RequestToken("", ""), "", params)

        assertFailsWith<IllegalArgumentException> { garminDataSource.getAccessParametersFor(userId) }
    }

    @Test
    fun dataCollectionPreparationEventCanBeExtractedFromValidPing() {
        val notificationText = garminStressPing.toString()
        val events = garminDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertTrue { events.size == 1 }
        val event = events[0]
        assertEquals(GarminDataSource.DATA_SOURCE_ID, event.dataSourceId)
        assertEquals(GarminDataCollectionType.STRESS, event.dataType)
    }

    @Test
    fun dataCollectionPreparationEventFailsWithInvalidFormat() {
        val notificationText = "fail"
        assertFailsWith<IllegalArgumentException> { garminDataSource.getDataCollectionPreparationEventFromPing(notificationText) }
    }

    @Test
    fun dataCollectionPreparationEventFailsWithInvalidDataType() {
        val notificationText = garminInvalidPing.toString()
        assertFailsWith<IllegalArgumentException> { garminDataSource.getDataCollectionPreparationEventFromPing(notificationText) }
    }

    @Test
    fun garminAccessParamsAreRetrievedWhenAuthorizesTokensAreReceived() {
        val userId = "garminAccessParamsAreRetrievedWhenAuthorizesTokensAreReceived"
        val dataSourceId = GarminDataSource.DATA_SOURCE_ID
        val params = garminDataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams

        // Save a state for the authorization
        val request = garminDataSource.initiateUserAuthorization(userId, dataSourceId, params)
        // Publish an authorization code event
        spyingEventBus.publish(this::class, OAuth1Event.AuthorizedTokenAcquired(
            stateId = request.authorizationState.id,
            requestToken = "request_token",
            tokenVerifier = "token_verifier",
            params = params,
            dataSourceId = GarminDataSource.DATA_SOURCE_ID
        ))

        // Verify that the user has access params saved
        val savedParams = accessParamService.getCurrentForInternalUserIdAndDataSource(userId, dataSourceId)
        assertEquals(userId, savedParams.internalUserId)
        assertEquals(TestProperties.GARMIN_TEST_USER_EXTERNAL_ID, savedParams.externalUserId)
        assertEquals(GarminDataSource.DATA_SOURCE_ID, savedParams.dataSourceId)
    }

    @Test
    fun applicationDataIsSavedWithAccessParams() {
        val userId = "applicationDataIsSavedWithAccessParams"
        val dataSourceId = GarminDataSource.DATA_SOURCE_ID
        val params = garminDataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams
        val appData = "appData"
        params.applicationData = appData

        // Save a state for the authorization
        val request = garminDataSource.initiateUserAuthorization(userId, dataSourceId, params)
        // Publish an authorization code event
        spyingEventBus.publish(this::class, OAuth1Event.AuthorizedTokenAcquired(
            stateId = request.authorizationState.id,
            requestToken = "request_token",
            tokenVerifier = "token_verifier",
            params = params,
            dataSourceId = GarminDataSource.DATA_SOURCE_ID
        ))

        // Verify that the user has access params saved
        val savedParams = accessParamService.getCurrentForInternalUserIdAndDataSource(userId, dataSourceId)
        assertEquals(appData, savedParams.applicationData)
    }

    @Test
    fun dataCollectionExecutionEventIsSent() {
        val userId = TestProperties.GARMIN_TEST_USER_EXTERNAL_ID
        val params = registerGarminUserFor(userId)

        // Fire preparation events
        val notificationText = garminStressPing.toString()
        val events = garminDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = com.nhaarman.mockitokotlin2.argumentCaptor<IntegrationEvent>()
        verify(spyingEventBus, atLeast(2)).publish(any(), argumentCaptor.capture())

        // Check if an execution event was fired
        val executedEvent = argumentCaptor.allValues.filterIsInstance<DataCollectionExecutionEvent>().firstOrNull()
        assertNotNull(executedEvent)
        executedEvent as DataCollectionExecutionEvent.OAuth1ExecutionEvent
        assertEquals(params, executedEvent.accessParams)
        assertEquals(GarminDataCollectionType.STRESS, executedEvent.dataIdentifier as GarminDataCollectionType)
    }

    @Test
    fun successfulDataCollectionEventIsFiredUponSuccessfulCollection() {
        val userId = TestProperties.GARMIN_TEST_USER_EXTERNAL_ID
        registerGarminUserFor(userId)

        // Fire preparation events
        val notificationText = garminStressPing.toString()
        val events = garminDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = com.nhaarman.mockitokotlin2.argumentCaptor<IntegrationEvent>()
        verify(spyingEventBus, atLeast(3)).publish(any(), argumentCaptor.capture())

        // Check if a data collected event is fired
        val executedEvent = argumentCaptor.allValues.filterIsInstance<DataSuccessfullyCollectedEvent>().firstOrNull()
        assertNotNull(executedEvent)
        assertEquals(userId, executedEvent.userId)
        assertEquals(GarminDataSource.DATA_SOURCE_ID, executedEvent.dataSourceId)
        assertEquals(GarminDataCollectionType.STRESS, executedEvent.dataType as GarminDataCollectionType)
    }

    @Test
    fun theRightDataTransformerGetsCalled() {
        val userId = TestProperties.GARMIN_TEST_USER_EXTERNAL_ID
        registerGarminUserFor(userId)

        // Fire preparation events
        val notificationText = garminStressPing.toString()
        val events = garminDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = com.nhaarman.mockitokotlin2.argumentCaptor<ThirdPartyData>()
        verify(spyingGarminTransformer, times(1)).transformStress(argumentCaptor.capture())

        // Check if a data collected event is fired
        val transformerArgument = argumentCaptor.allValues.firstOrNull()
        assertNotNull(transformerArgument)
        assertEquals(userId, transformerArgument.userId)
        assertEquals(GarminDataSource.DATA_SOURCE_ID, transformerArgument.dataSourceId)
        assertEquals(GarminDataCollectionType.STRESS, transformerArgument.dataIdentifier as GarminDataCollectionType)
    }

    @Test
    fun rightTransformedDataGetsPublished() {
        val userId = TestProperties.GARMIN_TEST_USER_EXTERNAL_ID
        registerGarminUserFor(userId)

        // Fire preparation events
        val notificationText = garminStressPing.toString()
        val events = garminDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the published data
        val argumentCaptor = com.nhaarman.mockitokotlin2.argumentCaptor<TransformedData>()
        verify(spyingPublisher, times(1)).publishCollectedData(argumentCaptor.capture())

        // Check if a data collected event is fired
        val publishedData = argumentCaptor.allValues.firstOrNull()
        assertNotNull(publishedData)
        assertEquals(userId, publishedData.userId)
        assertEquals(GarminDataSource.DATA_SOURCE_ID, publishedData.dataSourceId)
        assertEquals(GarminDataCollectionType.STRESS, publishedData.dataType as GarminDataCollectionType)
    }

    private fun registerGarminUserFor(userId: String): OAuth1AccessParams {
        val dataSourceId = GarminDataSource.DATA_SOURCE_ID
        return oauth1Operator.acquireAccessToken(
            userId = userId,
            dataSourceId = dataSourceId,
            requestToken = OAuth1RequestToken("test", "test"),
            verifier = "test",
            params = garminDataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams
        )
    }

}