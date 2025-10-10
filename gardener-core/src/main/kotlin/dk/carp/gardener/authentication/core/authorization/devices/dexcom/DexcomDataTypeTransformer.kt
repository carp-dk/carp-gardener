package dk.carp.gardener.authentication.core.authorization.devices.dexcom

import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer

/**
 * Defines the data transformation functions for each supported [DataCollectionType].
 */
interface DexcomDataTypeTransformer : IDataTypeTransformer {

    fun transformCalibrations(data: ThirdPartyData): List<Any>
    fun transformDataRange(data: ThirdPartyData): List<Any>
    fun transformEgvs(data: ThirdPartyData): List<Any>
    fun transformStatistics(data: ThirdPartyData): List<Any>

}