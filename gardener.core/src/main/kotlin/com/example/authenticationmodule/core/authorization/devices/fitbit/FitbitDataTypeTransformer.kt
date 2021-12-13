package com.example.authenticationmodule.core.authorization.devices.fitbit

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformer

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