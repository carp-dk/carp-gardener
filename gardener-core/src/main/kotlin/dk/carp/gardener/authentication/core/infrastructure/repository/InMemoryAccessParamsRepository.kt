package dk.carp.gardener.authentication.core.infrastructure.repository

import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsRepository

/**
 * A simple [IAccessParamsRepository] implementation for testing purposes which
 * stores [AccessParams] in memory.
 *
 * The class is open due to mocking purposes during testing.
 */
open class InMemoryAccessParamsRepository : IAccessParamsRepository {

    private val accessParams: MutableMap<String, AccessParams> = mutableMapOf()

    override fun upsert(params: AccessParams) {
        accessParams[params.id] = params
    }

    override fun getLatestByInternalOrExternalUserIdAndDataSourceId(userId: String, dataSourceId: String): AccessParams? {
        return accessParams.values
            .filter { (it.internalUserId == userId || it.externalUserId == userId) && it.dataSourceId == dataSourceId }
            .maxByOrNull { it.createdAt }
    }

    override fun existsByInternalOrExternalUserIdAndDataSourceId(userId: String, dataSourceId: String): Boolean {
        val params: AccessParams? = getLatestByInternalOrExternalUserIdAndDataSourceId(userId, dataSourceId)
        return params != null
    }

}