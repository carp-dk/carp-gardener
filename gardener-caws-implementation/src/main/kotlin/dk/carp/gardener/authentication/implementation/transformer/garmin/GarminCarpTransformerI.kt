package dk.carp.gardener.authentication.implementation.transformer.garmin

import dk.carp.gardener.authentication.core.authorization.devices.garmin.GarminDataTypeTransformer
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.implementation.transformer.carp.CarpDataStreamBuilder

/**
 * CARP specific [GarminDataTypeTransformer] implementation.
 */
class GarminCarpTransformerI : GarminDataTypeTransformer {
    override fun transformActivity(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformDailySummary(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformSleep(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformStress(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformBodyComposition(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformRespiration(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)
}
