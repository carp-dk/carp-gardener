package dk.carp.gardener.authentication.implementation.repository

import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Adapter that implements the synchronous `IAuthorizationStateRepository` API by delegating
 * to a coroutine-based implementation. Uses `runBlocking` on the IO dispatcher to bridge.
 */
class AdapterAuthorizationStateRepository(
    private val coroutineRepo: CoroutinePostgresAuthorizationStateRepository,
) : IAuthorizationStateRepository {
    override fun findById(id: String): AuthorizationState? =
        runBlocking {
            withContext(Dispatchers.IO) { coroutineRepo.findById(id) }
        }

    override fun upsert(state: AuthorizationState) {
        runBlocking { withContext(Dispatchers.IO) { coroutineRepo.upsert(state) } }
    }
}
