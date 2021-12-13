package com.example.authenticationmodule.core.authorization.devices.withings

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformer

/**
 * Supported Withings [DataCollectionType]s.
 */
enum class WithingsDataCollectionType(
    private val id: String,
    private val ep: String,
    private val cn: String,
    private val ns: String,
    val action: String
) : DataCollectionType {

    DAILY_ACTIVITY(
        id = "16",
        ep = "/v2/measure",
        cn = "Withings daily activity summary",
        ns = "com.withings.daily_activity",
        action = "getactivity"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as WithingsDataTypeTransformer
            return transformer.transformActivities(data)
        }
    },
    HEART_LIST(
        id = "54",
        ep = "/v2/heart",
        cn = "Withings ECG recordings",
        ns = "com.withings.heart_list",
        action = "list"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as WithingsDataTypeTransformer
            return transformer.transformHeartList(data)
        }
    },
    SLEEP(
        id = "44",
        ep = "/v2/sleep ",
        cn = "Withings sleep summary",
        ns = "com.withings.sleep",
        action = "getsummary"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as WithingsDataTypeTransformer
            return transformer.transformSleep(data)
        }
    };

    companion object {
        fun from(type: String?): DataCollectionType? = values().find { it.id == type }
    }

    override fun getIdentifier() = id
    override fun getEndpoint() = ep
    override fun getCustomName() = cn
    override fun getNamespace(): String = ns
}