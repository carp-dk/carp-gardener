package dk.carp.gardener.authentication.implementation.repository

import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Adapter that implements the synchronous `IAccessParamsRepository` API by delegating
 * to a coroutine-based implementation. Uses `runBlocking` on the IO dispatcher to bridge.
 */
class AdapterAccessParamsRepository(
    private val coroutineRepo: CoroutineMongoAccessParamsRepository,
) : IAccessParamsRepository {
    override fun upsert(params: AccessParams) {
        runBlocking {
            withContext(Dispatchers.IO) { coroutineRepo.upsert(params) }
        }
    }

    override fun getLatestByInternalOrExternalUserIdAndDataSourceId(
        userId: String,
        dataSourceId: String,
    ): AccessParams? =
        runBlocking {
            withContext(Dispatchers.IO) { coroutineRepo.getLatestByInternalOrExternalUserIdAndDataSourceId(userId, dataSourceId) }
        }

    override fun existsByInternalOrExternalUserIdAndDataSourceId(
        userId: String,
        dataSourceId: String,
    ): Boolean =
        runBlocking {
            withContext(Dispatchers.IO) { coroutineRepo.existsByInternalOrExternalUserIdAndDataSourceId(userId, dataSourceId) }
        }
}
