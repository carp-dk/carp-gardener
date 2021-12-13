package com.example.authenticationmodule.core.infrastructure.transformer.garmin

import com.example.authenticationmodule.core.authorization.devices.garmin.GarminDataTypeTransformer
import com.example.authenticationmodule.core.collection.data.ThirdPartyData

/**
 * A simple [GarminDataTypeTransformer] implementation for testing purposes which
 * does not perform any transformation, just returns the data.
 *
 * The class is open due to mocking purposes during testing.
 */
open class GarminEmptyTransformerI : GarminDataTypeTransformer {

    override fun transformActivity(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformDailySummary(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformSleep(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformStress(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformBodyComposition(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformRespiration(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

}