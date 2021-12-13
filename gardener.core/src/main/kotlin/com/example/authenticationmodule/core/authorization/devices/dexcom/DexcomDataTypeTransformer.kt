package com.example.authenticationmodule.core.authorization.devices.dexcom

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformer

/**
 * Defines the data transformation functions for each supported [DataCollectionType].
 */
interface DexcomDataTypeTransformer : IDataTypeTransformer {

    fun transformCalibrations(data: ThirdPartyData): List<Any>
    fun transformDataRange(data: ThirdPartyData): List<Any>
    fun transformEgvs(data: ThirdPartyData): List<Any>
    fun transformStatistics(data: ThirdPartyData): List<Any>

}