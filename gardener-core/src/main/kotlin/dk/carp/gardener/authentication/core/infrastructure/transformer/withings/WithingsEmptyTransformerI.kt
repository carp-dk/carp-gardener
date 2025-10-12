package dk.carp.gardener.authentication.core.infrastructure.transformer.withings

import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataTypeTransformer
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData

/**
 * A simple [WithingsDataTypeTransformer] implementation for testing purposes which
 * does not perform any transformation, just returns the data.
 *
 * The class is open due to mocking purposes during testing.
 */
open class WithingsEmptyTransformerI : WithingsDataTypeTransformer {
    override fun transformActivities(data: ThirdPartyData): List<Any> = listOf(data)

    override fun transformHeartList(data: ThirdPartyData): List<Any> = listOf(data)

    override fun transformSleep(data: ThirdPartyData): List<Any> = listOf(data)
}
