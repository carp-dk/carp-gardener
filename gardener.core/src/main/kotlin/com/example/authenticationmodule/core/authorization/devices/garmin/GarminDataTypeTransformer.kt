package com.example.authenticationmodule.core.authorization.devices.garmin

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformer

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