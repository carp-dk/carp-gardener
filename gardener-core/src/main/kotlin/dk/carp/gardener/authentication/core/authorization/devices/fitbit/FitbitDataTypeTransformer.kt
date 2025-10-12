package dk.carp.gardener.authentication.core.authorization.devices.fitbit

import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer

/**
 * Defines the data transformation functions for each supported [DataCollectionType].
 */
interface FitbitDataTypeTransformer : IDataTypeTransformer {
    fun transformActivities(data: ThirdPartyData): List<Any>

    fun transformHeartRate(data: ThirdPartyData): List<Any>

    fun transformBody(data: ThirdPartyData): List<Any>

    fun transformSleep(data: ThirdPartyData): List<Any>

    fun transformFood(data: ThirdPartyData): List<Any>
}
