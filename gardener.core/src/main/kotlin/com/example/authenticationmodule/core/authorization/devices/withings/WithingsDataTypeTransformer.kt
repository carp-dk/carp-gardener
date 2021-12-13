package com.example.authenticationmodule.core.authorization.devices.withings

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformer

/**
 * Defines the data transformation functions for each supported [DataCollectionType].
 */
interface WithingsDataTypeTransformer : IDataTypeTransformer {

    fun transformActivities(data: ThirdPartyData): List<Any>
    fun transformHeartList(data: ThirdPartyData): List<Any>
    fun transformSleep(data: ThirdPartyData): List<Any>

}