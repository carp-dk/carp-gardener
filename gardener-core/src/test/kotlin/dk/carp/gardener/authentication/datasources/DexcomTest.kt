package dk.carp.gardener.authentication.datasources

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.base.OAuth2Test
import dk.carp.gardener.authentication.base.TestProperties
import dk.carp.gardener.authentication.base.TestUtil.Companion.getResourceAsText
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.AuthorizationType
import dk.carp.gardener.authentication.core.authorization.devices.dexcom.DexcomDataCollectionType
import dk.carp.gardener.authentication.core.authorization.devices.dexcom.DexcomDataSource
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.collection.data.TransformedData
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionExecutionEvent
import dk.carp.gardener.authentication.core.common.events.datacollection.DataSuccessfullyCollectedEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IntegrationEvent
import dk.carp.gardener.authentication.core.common.events.oauth2.OAuth2Event
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.times
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for [DexcomDataSource].
 */
class DexcomTest : OAuth2Test() {
    private val dexcomEgvsPing: JsonNode =
        ConfiguredObjectMapper.instance.readTree(getResourceAsText("/dexcom/dexcom_egvs_ping.json"))
    private val dexcomExpiredAccessParams: JsonNode =
        ConfiguredObjectMapper.instance.readTree(getResourceAsText("/dexcom/dexcom_expired_access_params.json"))
    private val dexcomInvalidPing: JsonNode =
        ConfiguredObjectMapper.instance.readTree(getResourceAsText("/dexcom/dexcom_invalid_ping.json"))

    @Test
    fun returnsTheCorrectId() {
        val id = dexcomDataSource.getId()
        assertEquals(DexcomDataSource.DATA_SOURCE_ID, id)
    }

    @Test
    fun returnsTheCorrectAuthorizationType() {
        val type = dexcomDataSource.getAuthorizationType()
        assertEquals(AuthorizationType.OAUTH2, type)
    }

    @Test
    fun theEstablishedRequestParamsAreCorrect() {
        val params = dexcomDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
        assertTrue { params.additionalParamsForGrants.getMap().isEmpty() }
        assertTrue { params.additionalParamsForTokens.getMap().isEmpty() }
        assertTrue { params.scopes.getList().isEmpty() }
    }

    @Test
    fun newAuthorizationCreatesNewState() {
        val userId = "newAuthorizationCreatesNewState"
        val dataSourceId = DexcomDataSource.DATA_SOURCE_ID
        val params = dexcomDataSource.getEstablishedAuthorizationRequestParams()

        val request = dexcomDataSource.initiateUserAuthorization(userId, dataSourceId, params)

        // Assert that the authorization state is saved
        val state = authorizationStateService.getById(request.authorizationState.id)
        assertEquals(request.authorizationState, state)
        assertEquals(userId, state.userId)
        assertEquals(dataSourceId, state.dataSourceId)
    }

    @Test
    fun authorizationStateSavesApplicationDataIfPresent() {
        val userId = "authorizationStateSavesApplicationDataIfPresent"
        val dataSourceId = DexcomDataSource.DATA_SOURCE_ID
        val params = dexcomDataSource.getEstablishedAuthorizationRequestParams()
        val applicationData = "appData"
        params.applicationData = applicationData

        val request = dexcomDataSource.initiateUserAuthorization(userId, dataSourceId, params)

        // Assert that the authorization state is saved
        val state = authorizationStateService.getById(request.authorizationState.id)
        assertEquals(applicationData, state.applicationData)
    }

    @Test
    fun theSameUserCannotAuthorizeTwiceToTheSameDataSource() {
        val userId = "theSameUserCannotAuthorizeTwiceToTheSameDataSource"
        val dataSourceId = DexcomDataSource.DATA_SOURCE_ID
        val params = dexcomDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        // Imitate a previous successful authorization
        oauth2Operator.retrieveAccessParams(userId, dataSourceId, "authorization_code", params)

        assertFailsWith<IllegalArgumentException> { dexcomDataSource.initiateUserAuthorization(userId, dataSourceId, params) }
    }

    @Test
    fun accessParamsCanBeRetrieved() {
        val userId = "accessParamsCanBeRetrieved"
        val dataSourceId = DexcomDataSource.DATA_SOURCE_ID
        val params = dexcomDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        val actualAccessParams = oauth2Operator.retrieveAccessParams(userId, dataSourceId, "authorization_code", params)
        oauth2Operator.retrieveAccessParams("dummy1", dataSourceId, "authorization_code", params)
        oauth2Operator.retrieveAccessParams("dummy2", dataSourceId, "authorization_code", params)

        val accessParams = dexcomDataSource.getAccessParametersFor(userId)
        assertEquals(actualAccessParams.id, accessParams.id)
    }

    @Test
    fun failsWhenThereAreNoAccessParams() {
        val userId = "failsWhenThereAreNoAccessParams"
        val dataSourceId = DexcomDataSource.DATA_SOURCE_ID
        val params = dexcomDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        oauth2Operator.retrieveAccessParams("dummy1", dataSourceId, "authorization_code", params)
        oauth2Operator.retrieveAccessParams("dummy2", dataSourceId, "authorization_code", params)

        assertFailsWith<IllegalArgumentException> { dexcomDataSource.getAccessParametersFor(userId) }
    }

    @Test
    fun dataCollectionPreparationEventCanBeExtractedFromValidPing() {
        val notificationText = dexcomEgvsPing.toString()
        val events = dexcomDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertTrue { events.size == 1 }
        val event = events[0]
        assertEquals(DexcomDataSource.DATA_SOURCE_ID, event.dataSourceId)
        assertEquals(DexcomDataCollectionType.EGVS, event.dataType)
    }

    @Test
    fun dataCollectionPreparationEventFailsWithInvalidPing() {
        val notificationText = "fail"
        assertFailsWith<IllegalArgumentException> {
            dexcomDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        }
    }

    @Test
    fun dataCollectionPreparationEventFailsWithInvalidDataType() {
        val notificationText = dexcomInvalidPing.toString()
        assertFailsWith<IllegalArgumentException> {
            dexcomDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        }
    }

    @Test
    fun dexcomAccessParamsAreRetrievedWhenAuthorizationCodeIsReceived() {
        val userId = "dexcomAccessParamsAreRetrievedWhenAuthorizationCodeIsReceived"
        val dataSourceId = DexcomDataSource.DATA_SOURCE_ID
        val params = dexcomDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams

        // Save a state for the authorization
        val request = dexcomDataSource.initiateUserAuthorization(userId, dataSourceId, params)
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
        assertEquals(TestProperties.DEXCOM_TEST_USER_EXTERNAL_ID, savedParams.externalUserId)
        assertEquals(DexcomDataSource.DATA_SOURCE_ID, savedParams.dataSourceId)
    }

    @Test
    fun applicationDataIsSavedWithAccessParams() {
        val userId = "applicationDataIsSavedWithAccessParams"
        val dataSourceId = DexcomDataSource.DATA_SOURCE_ID
        val params = dexcomDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
        val appData = "appData"
        params.applicationData = appData

        // Save a state for the authorization
        val request = dexcomDataSource.initiateUserAuthorization(userId, dataSourceId, params)
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
        val userId = TestProperties.DEXCOM_PING_USER_ID
        val params = registerDexcomUserFor(userId)

        // Fire preparation events
        val notificationText = dexcomEgvsPing.toString()
        val events = dexcomDataSource.getDataCollectionPreparationEventFromPing(notificationText)
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
        assertEquals(DexcomDataCollectionType.EGVS, executedEvent.dataIdentifier as DexcomDataCollectionType)
    }

    @Test
    fun accessParamsGetRefreshedWhenExpired() {
        // Save expired user access params
        val params =
            OAuth2AccessParams(
                internalUserId = TestProperties.DEXCOM_PING_USER_ID,
                dataSourceId = DexcomDataSource.DATA_SOURCE_ID,
                params = dexcomExpiredAccessParams,
                externalUserId = TestProperties.DEXCOM_TEST_USER_EXTERNAL_ID,
            )
        accessParamService.addParams(params)

        // Fire preparation events
        val notificationText = dexcomEgvsPing.toString()
        val events = dexcomDataSource.getDataCollectionPreparationEventFromPing(notificationText)
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
        val userId = TestProperties.DEXCOM_PING_USER_ID
        registerDexcomUserFor(userId)

        // Fire preparation events
        val notificationText = dexcomEgvsPing.toString()
        val events = dexcomDataSource.getDataCollectionPreparationEventFromPing(notificationText)
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
        val userId = TestProperties.DEXCOM_PING_USER_ID
        registerDexcomUserFor(userId)

        // Fire preparation events
        val notificationText = dexcomEgvsPing.toString()
        val events = dexcomDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = argumentCaptor<IntegrationEvent>()
        verify(spyingEventBus, atLeast(3)).publish(any(), argumentCaptor.capture())

        // Check if a data collected event is fired
        val executedEvent = argumentCaptor.allValues.filterIsInstance<DataSuccessfullyCollectedEvent>().firstOrNull()
        assertNotNull(executedEvent)
        assertEquals(userId, executedEvent.userId)
        assertEquals(DexcomDataSource.DATA_SOURCE_ID, executedEvent.dataSourceId)
        assertEquals(DexcomDataCollectionType.EGVS, executedEvent.dataType as DexcomDataCollectionType)
    }

    @Test
    fun theRightDataTransformerGetsCalled() {
        val userId = TestProperties.DEXCOM_PING_USER_ID
        registerDexcomUserFor(userId)

        // Fire preparation events
        val notificationText = dexcomEgvsPing.toString()
        val events = dexcomDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the events that were published
        val argumentCaptor = argumentCaptor<ThirdPartyData>()
        verify(spyingDexcomTransformer, times(1)).transformEgvs(argumentCaptor.capture())

        // Check if a data collected event is fired
        val transformerArgument = argumentCaptor.allValues.firstOrNull()
        assertNotNull(transformerArgument)
        assertEquals(userId, transformerArgument.userId)
        assertEquals(DexcomDataSource.DATA_SOURCE_ID, transformerArgument.dataSourceId)
        assertEquals(DexcomDataCollectionType.EGVS, transformerArgument.dataIdentifier as DexcomDataCollectionType)
    }

    @Test
    fun rightTransformedDataGetsPublished() {
        val userId = TestProperties.DEXCOM_PING_USER_ID
        registerDexcomUserFor(userId)

        // Fire preparation events
        val notificationText = dexcomEgvsPing.toString()
        val events = dexcomDataSource.getDataCollectionPreparationEventFromPing(notificationText)
        assertEquals(1, events.size)
        events.forEach { event -> spyingEventBus.publish(this::class, event) }

        // Capture the published data
        val argumentCaptor = argumentCaptor<TransformedData>()
        verify(spyingPublisher, times(1)).publishCollectedData(argumentCaptor.capture())

        // Check if a data collected event is fired
        val publishedData = argumentCaptor.allValues.firstOrNull()
        assertNotNull(publishedData)
        assertEquals(userId, publishedData.userId)
        assertEquals(DexcomDataSource.DATA_SOURCE_ID, publishedData.dataSourceId)
        assertEquals(DexcomDataCollectionType.EGVS, publishedData.dataType as DexcomDataCollectionType)
    }

    private fun registerDexcomUserFor(userId: String): OAuth2AccessParams {
        val dataSourceId = DexcomDataSource.DATA_SOURCE_ID
        return oauth2Operator.retrieveAccessParams(
            userId,
            dataSourceId,
            "authorization_code",
            dexcomDataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams,
        )
    }
}
