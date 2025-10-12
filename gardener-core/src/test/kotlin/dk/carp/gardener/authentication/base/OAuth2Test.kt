package dk.carp.gardener.authentication.base

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.base.TestUtil.Companion.getResourceAsText
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.authorization.devices.dexcom.DexcomDataSource
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataSource
import dk.carp.gardener.authentication.core.collection.oauth2.OAuth2DataCollectionService
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import dk.carp.gardener.authentication.core.infrastructure.transformer.dexcom.DexcomEmptyTransformerI
import dk.carp.gardener.authentication.core.infrastructure.transformer.fitbit.FitbitEmptyTransformerI
import dk.carp.gardener.authentication.core.infrastructure.transformer.withings.WithingsEmptyTransformerI
import dk.carp.gardener.authentication.mock.OAuth2DataCollectionOperatorBuilder
import dk.carp.gardener.authentication.mock.OAuth2Operator
import org.mockito.Mockito

/**
 * A common test class parent for every data sources that
 * use OAuth2 authorization.
 */
abstract class OAuth2Test : CoreTest() {
    // OAuth2 Operators
    protected val oauth2CollectionService: OAuth2DataCollectionService
    protected val oauth2Operator: OAuth2Operator

    // Transformers
    protected val spyingFitbitTransformer = Mockito.spy(FitbitEmptyTransformerI())
    protected val spyingWithingsTransformer = Mockito.spy(WithingsEmptyTransformerI())
    protected val spyingDexcomTransformer = Mockito.spy(DexcomEmptyTransformerI())

    // Data Source Settings
    // Fitbit
    protected val fitbitClientSettings: OAuth2ClientSettings
    protected val fitbitDataSource: FitbitDataSource
    protected val fitbitActivitiesData: JsonNode
    protected val fitbitAccessParams: JsonNode

    // Withings
    protected val withingsClientSettings: OAuth2ClientSettings
    protected val withingsDataSource: WithingsDataSource
    protected val withingsActivitiesData: JsonNode
    protected val withingsAccessParams: JsonNode

    // Dexcom
    protected val dexcomClientSettings: OAuth2ClientSettings
    protected val dexcomDataSource: DexcomDataSource
    protected val dexcomEgvsData: JsonNode
    protected val dexcomAccessParams: JsonNode

    init {
        // Register transformers
        transformerRegistry.registerTransformer(FitbitDataSource.DATA_SOURCE_ID, spyingFitbitTransformer)
        transformerRegistry.registerTransformer(WithingsDataSource.DATA_SOURCE_ID, spyingWithingsTransformer)
        transformerRegistry.registerTransformer(DexcomDataSource.DATA_SOURCE_ID, spyingDexcomTransformer)

        // Initialize test data
        // Fitbit
        fitbitActivitiesData = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/fitbit/fitbit_activities_data.json"))
        fitbitAccessParams = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/fitbit/fitbit_access_params.json"))
        // Withings
        withingsActivitiesData = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/withings/withings_activities_data.json"))
        withingsAccessParams = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/withings/withings_access_params.json"))
        // Dexcom
        dexcomEgvsData = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/dexcom/dexcom_egvs_data.json"))
        dexcomAccessParams = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/dexcom/dexcom_access_params.json"))

        // Initialize Operators
        oauth2Operator =
            OAuth2Operator(
                stateService = authorizationStateService,
                accessParamsService = accessParamService,
                fitbitAccessParams = fitbitAccessParams,
                fitbitActivitiesData = fitbitActivitiesData,
                withingsAccessParams = withingsAccessParams,
                withingsActivitiesData = withingsActivitiesData,
                dexcomAccessParams = dexcomAccessParams,
                dexcomEgvsData = dexcomEgvsData,
            )
        oauth2CollectionService =
            OAuth2DataCollectionService(
                spyingEventBus,
                spyingPublisher,
                transformerRegistry,
                OAuth2DataCollectionOperatorBuilder(
                    stateService = authorizationStateService,
                    accessParamsService = accessParamService,
                    fitbitAccessParams = fitbitAccessParams,
                    fitbitActivitiesData = fitbitActivitiesData,
                    withingsAccessParams = withingsAccessParams,
                    withingsActivitiesData = withingsActivitiesData,
                    dexcomAccessParams = dexcomAccessParams,
                    dexcomEgvsData = dexcomEgvsData,
                ),
            )

        // Initialize OAuth1 Data Sources
        // Fitbit
        fitbitClientSettings =
            OAuth2ClientSettings(
                TestProperties.FITBIT_CLIENT_ID,
                TestProperties.FITBIT_CLIENT_SECRET,
                TestProperties.FITBIT_AUTHORIZATION_URI,
                TestProperties.FITBIT_TOKEN_URI,
                TestProperties.FITBIT_DATA_URI,
                "callback",
            )
        fitbitDataSource =
            FitbitDataSource(
                spyingEventBus,
                authorizationStateService,
                accessParamService,
                fitbitClientSettings,
                oauth2Operator,
            )
        // Withings
        withingsClientSettings =
            OAuth2ClientSettings(
                TestProperties.WITHINGS_CLIENT_ID,
                TestProperties.WITHINGS_CLIENT_SECRET,
                TestProperties.WITHINGS_AUTHORIZATION_URI,
                TestProperties.WITHINGS_TOKEN_URI,
                TestProperties.WITHINGS_DATA_URI,
                "callback",
            )
        withingsDataSource =
            WithingsDataSource(
                spyingEventBus,
                authorizationStateService,
                accessParamService,
                withingsClientSettings,
                oauth2Operator,
            )
        // Dexcom
        dexcomClientSettings =
            OAuth2ClientSettings(
                TestProperties.DEXCOM_CLIENT_ID,
                TestProperties.DEXCOM_CLIENT_SECRET,
                TestProperties.DEXCOM_AUTHORIZATION_URI,
                TestProperties.DEXCOM_TOKEN_URI,
                TestProperties.DEXCOM_DATA_URI,
                "callback",
            )
        dexcomDataSource =
            DexcomDataSource(
                spyingEventBus,
                authorizationStateService,
                accessParamService,
                dexcomClientSettings,
                oauth2Operator,
            )
    }
}
