package com.example.authenticationmodule.core.authorization.authorizationstate

class AuthorizationStateServiceHost(private val repository: IAuthorizationStateRepository) : IAuthorizationStateService {

    /**
     * Retrieves an [AuthorizationState] object by its ID.
     *
     * @param id The ID of the [AuthorizationState]
     *
     * @return [AuthorizationState] or null if it's not found.
     *
     * @throws IllegalArgumentException When no state entry is found with the given [id].
     */
    override fun getById(id: String): AuthorizationState  {
        return repository.findById(id) ?: throw IllegalArgumentException("State is not found with id $id!")
    }

    /**
     * Stores a newly created [AuthorizationState] or
     * updates it, if it is already present (by its ID).
     *
     * @param state The [AuthorizationState] object to store.
     */
    override fun save(state: AuthorizationState) {
        repository.upsert(state)
    }

    /**
     * Sets the successful flag to true on the given [AuthorizationState] object
     * and saves it.
     *
     * @param state The [AuthorizationState] object to update.
     */
    override fun setSuccessfulState(state: AuthorizationState) {
        state.success = true
        save(state)
    }

}