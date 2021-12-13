package com.example.authenticationmodule.core.infrastructure.transformer.withings

import com.example.authenticationmodule.core.authorization.devices.withings.WithingsDataTypeTransformer
import com.example.authenticationmodule.core.collection.data.ThirdPartyData

/**
 * A simple [WithingsDataTypeTransformer] implementation for testing purposes which
 * does not perform any transformation, just returns the data.
 *
 * The class is open due to mocking purposes during testing.
 */
open class WithingsEmptyTransformerI : WithingsDataTypeTransformer {

    override fun transformActivities(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformHeartList(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

    override fun transformSleep(data: ThirdPartyData): List<Any> {
        return listOf(data)
    }

}