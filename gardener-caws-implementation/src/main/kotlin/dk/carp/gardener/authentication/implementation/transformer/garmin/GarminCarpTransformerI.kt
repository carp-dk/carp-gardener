package dk.carp.gardener.authentication.implementation.transformer.garmin

import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.authorization.devices.garmin.GarminDataTypeTransformer
import dk.carp.gardener.authentication.implementation.transformer.carp.CarpDataPointBuilder

/**
 * CARP specific [GarminDataTypeTransformer] implementation.
 */
class GarminCarpTransformerI : GarminDataTypeTransformer {

    override fun transformActivity(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformDailySummary(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformSleep(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformStress(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformBodyComposition(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

    override fun transformRespiration(data: ThirdPartyData): List<Any> {
        return CarpDataPointBuilder.fromThirdPartyData(data)
    }

}