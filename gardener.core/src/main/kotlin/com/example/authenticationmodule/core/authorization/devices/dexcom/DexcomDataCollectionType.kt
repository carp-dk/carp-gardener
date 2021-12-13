package com.example.authenticationmodule.core.authorization.devices.dexcom

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformer

/**
 * Supported Dexcom [DataCollectionType]s.
 */
enum class DexcomDataCollectionType(
    private val id: String,
    private val ep: String,
    private val cn: String,
    private val ns: String
) : DataCollectionType {

    CALIBRATIONS(
        id = "calibrations",
        ep = "/calibrations",
        cn = "Dexcom Calibrations data",
        ns = "com.dexcom.calibrations"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as DexcomDataTypeTransformer
            return transformer.transformCalibrations(data)
        }
    },
    DATA_RANGE(
        id = "dataRange",
        ep = "/dataRange",
        cn = "Dexcom Data Range records",
        ns = "com.dexcom.data_range"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as DexcomDataTypeTransformer
            return transformer.transformDataRange(data)
        }
    },
    EGVS(
        id = "egvs",
        ep = "/egvs",
        cn = "Dexcom glucose value data",
        ns = "com.dexcom.egvs"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as DexcomDataTypeTransformer
            return transformer.transformEgvs(data)
        }
    },
    STATISTICS(
        id = "statistics",
        ep = "/statistics",
        cn = "Dexcom summary statistics",
        ns = "com.dexcom.statistics"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as DexcomDataTypeTransformer
            return transformer.transformStatistics(data)
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