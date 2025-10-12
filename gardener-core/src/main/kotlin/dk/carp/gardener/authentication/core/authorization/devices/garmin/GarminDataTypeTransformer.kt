package dk.carp.gardener.authentication.core.authorization.devices.garmin

import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer

/**
 * Defines the data transformation functions for each supported [DataCollectionType].
 */
interface GarminDataTypeTransformer : IDataTypeTransformer {
    fun transformActivity(data: ThirdPartyData): List<Any>

    fun transformDailySummary(data: ThirdPartyData): List<Any>

    fun transformSleep(data: ThirdPartyData): List<Any>

    fun transformStress(data: ThirdPartyData): List<Any>

    fun transformBodyComposition(data: ThirdPartyData): List<Any>

    fun transformRespiration(data: ThirdPartyData): List<Any>
}
