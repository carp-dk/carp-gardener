package com.example.authenticationmodule.core.infrastructure.transformer.fitbit

import com.example.authenticationmodule.core.authorization.devices.fitbit.FitbitDataTypeTransformer
import com.example.authenticationmodule.core.collection.data.ThirdPartyData

/**
 * A simple [FitbitDataTypeTransformer] implementation for testing purposes which
 * does not perform any transformation, just returns the data.
 *
 * The class is open due to mocking purposes during testing.
 */
open class FitbitEmptyTransformerI : FitbitDataTypeTransformer {

    override fun transformActivities(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformHeartRate(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformBody(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformSleep(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformFood(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

}