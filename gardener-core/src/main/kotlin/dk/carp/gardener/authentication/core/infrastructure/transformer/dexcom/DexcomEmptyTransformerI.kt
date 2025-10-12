package dk.carp.gardener.authentication.core.infrastructure.transformer.dexcom

import dk.carp.gardener.authentication.core.authorization.devices.dexcom.DexcomDataTypeTransformer
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData

/**
 * A simple [DexcomDataTypeTransformer] implementation for testing purposes which
 * does not perform any transformation, just returns the data.
 *
 * The class is open due to mocking purposes during testing.
 */
open class DexcomEmptyTransformerI : DexcomDataTypeTransformer {
    override fun transformCalibrations(data: ThirdPartyData): List<Any> = listOf(data)

    override fun transformDataRange(data: ThirdPartyData): List<Any> = listOf(data)

    override fun transformEgvs(data: ThirdPartyData): List<Any> = listOf(data)

    override fun transformStatistics(data: ThirdPartyData): List<Any> = listOf(data)
}
