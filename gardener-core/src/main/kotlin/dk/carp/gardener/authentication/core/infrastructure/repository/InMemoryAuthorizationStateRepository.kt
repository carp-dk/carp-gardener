package dk.carp.gardener.authentication.core.infrastructure.repository

import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateRepository

/**
 * A simple [IAuthorizationStateRepository] implementation for testing purposes which
 * stores [AuthorizationState] in memory.
 *
 * The class is open due to mocking purposes during testing.
 */
open class InMemoryAuthorizationStateRepository :
    IAuthorizationStateRepository {

    private val states: MutableMap<String, AuthorizationState> = mutableMapOf()

    override fun findById(id: String): AuthorizationState? = states[id]


    override fun upsert(state: AuthorizationState) {
        states[state.id] = state
        println("New authorization state is saved for ${state.dataSourceId}/${state.userId}.")
    }

}