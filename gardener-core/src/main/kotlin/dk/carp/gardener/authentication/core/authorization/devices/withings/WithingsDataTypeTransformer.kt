package dk.carp.gardener.authentication.core.authorization.devices.withings

import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer

/**
 * Defines the data transformation functions for each supported [DataCollectionType].
 */
interface WithingsDataTypeTransformer : IDataTypeTransformer {

    fun transformActivities(data: ThirdPartyData): List<Any>
    fun transformHeartList(data: ThirdPartyData): List<Any>
    fun transformSleep(data: ThirdPartyData): List<Any>

}