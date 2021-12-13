package com.example.authenticationmodule.base

import com.example.authenticationmodule.base.TestUtil.Companion.getResourceAsText
import com.example.authenticationmodule.core.authorization.datasource.oauth1.OAuth1ClientSettings
import com.example.authenticationmodule.core.authorization.devices.garmin.GarminDataSource
import com.example.authenticationmodule.core.collection.oauth1.OAuth1DataCollectionService
import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper
import com.example.authenticationmodule.core.infrastructure.transformer.garmin.GarminEmptyTransformerI
import com.example.authenticationmodule.mock.OAuth1DataCollectionOperatorBuilder
import com.example.authenticationmodule.mock.OAuth1Operator
import com.fasterxml.jackson.databind.JsonNode
import org.mockito.Mockito

/**
 * A common test class parent for every data sources that
 * use OAuth1 authorization.
 */
abstract class OAuth1Test : CoreTest()  {

    // OAuth1 Operators
    protected val oauth1CollectionService: OAuth1DataCollectionService
    protected val oauth1Operator: OAuth1Operator

    // Transformers
    protected val spyingGarminTransformer = Mockito.spy(GarminEmptyTransformerI())

    // Data Source Settings
    // Garmin
    protected val garminClientSettings: OAuth1ClientSettings
    protected val garminDataSource: GarminDataSource
    protected val garminStressData: JsonNode
    protected val garminAccessParams: JsonNode

    init {
        // Register transformers
        transformerRegistry.registerTransformer(GarminDataSource.DATA_SOURCE_ID, spyingGarminTransformer)

        // Initialize test data
        // Garmin

        garminStressData = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/garmin/garmin_stress_data.json"))
        garminAccessParams = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/garmin/garmin_access_params.json"))

        // Initialize Operators
        oauth1Operator = OAuth1Operator(
            stateService = authorizationStateService,
            accessParamsService = accessParamService,
            garminAccessParams = garminAccessParams,
            garminStressData = garminStressData
        )
        oauth1CollectionService = OAuth1DataCollectionService(
            spyingEventBus,
            spyingPublisher,
            transformerRegistry,
            OAuth1DataCollectionOperatorBuilder(
                stateService = authorizationStateService,
                accessParamsService = accessParamService,
                garminAccessParams = garminAccessParams,
                garminStressData = garminStressData
            )
        )

        // Initialize OAuth1 Data Sources
        // Garmin
        garminClientSettings = OAuth1ClientSettings(
            TestProperties.GARMIN_CONSUMER_KEY,
            TestProperties.GARMIN_CONSUMER_SECRET,
            "test",
            "test",
            TestProperties.GARMIN_REQUEST_TOKEN_URI,
            TestProperties.GARMIN_ACCESS_TOKEN_URI,
            TestProperties.GARMIN_AUTHORIZATION_URI,
            TestProperties.GARMIN_DATA_URI
        )
        garminDataSource = GarminDataSource(
            spyingEventBus,
            authorizationStateService,
            accessParamService,
            garminClientSettings,
            oauth1Operator
        )
    }

}