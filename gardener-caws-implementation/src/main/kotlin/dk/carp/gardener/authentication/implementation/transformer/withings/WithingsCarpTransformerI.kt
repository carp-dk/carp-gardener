package dk.carp.gardener.authentication.implementation.transformer.withings

import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataTypeTransformer
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.implementation.transformer.carp.CarpDataStreamBuilder

/**
 * CARP specific [WithingsDataTypeTransformer] implementation.
 */
class WithingsCarpTransformerI : WithingsDataTypeTransformer {
    override fun transformActivities(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformHeartList(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)

    override fun transformSleep(data: ThirdPartyData): List<Any> = CarpDataStreamBuilder.fromThirdPartyData(data)
}
