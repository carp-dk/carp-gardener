package dk.carp.gardener.authentication.core.authorization.devices.withings

import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer

/**
 * Supported Withings [DataCollectionType]s.
 */
enum class WithingsDataCollectionType(
    private val id: String,
    private val ep: String,
    private val cn: String,
    private val ns: String,
    val action: String,
) : DataCollectionType {
    DAILY_ACTIVITY(
        id = "16",
        ep = "/v2/measure",
        cn = "Withings daily activity summary",
        ns = "com.withings.daily_activity",
        action = "getactivity",
    ) {
        override fun acceptTransformer(
            transformer: IDataTypeTransformer,
            data: ThirdPartyData,
        ): List<Any> {
            transformer as WithingsDataTypeTransformer
            return transformer.transformActivities(data)
        }
    },
    HEART_LIST(
        id = "54",
        ep = "/v2/heart",
        cn = "Withings ECG recordings",
        ns = "com.withings.heart_list",
        action = "list",
    ) {
        override fun acceptTransformer(
            transformer: IDataTypeTransformer,
            data: ThirdPartyData,
        ): List<Any> {
            transformer as WithingsDataTypeTransformer
            return transformer.transformHeartList(data)
        }
    },
    SLEEP(
        id = "44",
        ep = "/v2/sleep ",
        cn = "Withings sleep summary",
        ns = "com.withings.sleep",
        action = "getsummary",
    ) {
        override fun acceptTransformer(
            transformer: IDataTypeTransformer,
            data: ThirdPartyData,
        ): List<Any> {
            transformer as WithingsDataTypeTransformer
            return transformer.transformSleep(data)
        }
    }, ;

    companion object {
        fun from(type: String?): DataCollectionType? = entries.find { it.id == type }
    }

    override fun getIdentifier() = id

    override fun getEndpoint() = ep

    override fun getCustomName() = cn

    override fun getNamespace(): String = ns
}
