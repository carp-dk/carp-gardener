package dk.carp.gardener.authentication.implementation.transformer.fitbit

import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataTypeTransformer
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.implementation.transformer.carp.CarpDataStreamBuilder

/**
 * CARP specific [FitbitDataTypeTransformer] implementation.
 */
class FitbitCarpTransformerI : FitbitDataTypeTransformer {
    override fun transformActivities(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformHeartRate(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformBody(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformSleep(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformFood(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)
}
