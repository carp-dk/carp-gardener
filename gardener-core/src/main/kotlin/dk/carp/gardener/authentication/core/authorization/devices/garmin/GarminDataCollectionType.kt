package dk.carp.gardener.authentication.core.authorization.devices.garmin

import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer

/**
 * Supported Garmin [DataCollectionType]s.
 */
enum class GarminDataCollectionType(
    private val id: String,
    private val ep: String,
    private val cn: String,
    private val ns: String,
) : DataCollectionType {

    ACTIVITY(
        "https://apis.garmin.com/wellness-api/rest/activities",
        "https://apis.garmin.com/wellness-api/rest/activities",
        "Garmin Activity Summary",
        "com.garmin.activity"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as GarminDataTypeTransformer
            return transformer.transformActivity(data)
        }
    },
    DAILY_SUMMARY(
        "https://apis.garmin.com/wellness-api/rest/dailies",
        "https://apis.garmin.com/wellness-api/rest/dailies",
        "Garmin Daily Summary",
        "com.garmin.daily_summary"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as GarminDataTypeTransformer
            return transformer.transformDailySummary(data)
        }
    },
    SLEEP(
        "https://apis.garmin.com/wellness-api/rest/sleeps",
        "https://apis.garmin.com/wellness-api/rest/sleeps",
        "Garmin Sleep Logs",
        "com.garmin.sleep"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as GarminDataTypeTransformer
            return transformer.transformSleep(data)
        }
    },
    STRESS(
        "https://apis.garmin.com/wellness-api/rest/stressDetails",
        "https://apis.garmin.com/wellness-api/rest/stressDetails",
        "Garmin Stress logs",
        "com.garmin.stress"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as GarminDataTypeTransformer
            return transformer.transformStress(data)
        }
    },
    BODY_COMPOSITION(
        "https://apis.garmin.com/wellness-api/rest/bodyComps",
        "https://apis.garmin.com/wellness-api/rest/bodyComps",
        "Garmin Body Composition",
        "com.garmin.body_composition"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as GarminDataTypeTransformer
            return transformer.transformBodyComposition(data)
        }
    },
    RESPIRATION(
        "https://apis.garmin.com/wellness-api/rest/respiration",
        "https://apis.garmin.com/wellness-api/rest/respiration",
        "Garmin Respiration",
        "com.garmin.respiration"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as GarminDataTypeTransformer
            return transformer.transformRespiration(data)
        }
    };

    companion object {
        fun from(type: String?): DataCollectionType? = entries.find { it.id == type }
    }

    override fun getIdentifier() = id
    override fun getEndpoint() = ep
    override fun getCustomName() = cn
    override fun getNamespace(): String = ns
}