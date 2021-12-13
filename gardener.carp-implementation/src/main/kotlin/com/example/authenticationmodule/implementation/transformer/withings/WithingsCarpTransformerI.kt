package com.example.authenticationmodule.implementation.transformer.withings

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.authorization.devices.withings.WithingsDataTypeTransformer
import com.example.authenticationmodule.implementation.transformer.carp.CarpDataPointBuilder

/**
 * CARP specific [WithingsDataTypeTransformer] implementation.
 */
class WithingsCarpTransformerI : WithingsDataTypeTransformer {

    override fun transformActivities(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformHeartList(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformSleep(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

}