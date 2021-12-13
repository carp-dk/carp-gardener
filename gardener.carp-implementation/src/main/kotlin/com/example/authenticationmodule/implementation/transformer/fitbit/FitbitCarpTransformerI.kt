package com.example.authenticationmodule.implementation.transformer.fitbit

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.authorization.devices.fitbit.FitbitDataTypeTransformer
import com.example.authenticationmodule.implementation.transformer.carp.CarpDataPointBuilder

/**
 * CARP specific [FitbitDataTypeTransformer] implementation.
 */
class FitbitCarpTransformerI : FitbitDataTypeTransformer {

    override fun transformActivities(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformHeartRate(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformBody(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformSleep(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformFood(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

}