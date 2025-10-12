package dk.carp.gardener.authentication.core.authorization.authorizationstate

/**
 * Manipulates [AuthorizationState] objects.
 */
interface IAuthorizationStateRepository {
    /**
     * Retrieves an [AuthorizationState] object by its ID.
     *
     * @param id The ID of the [AuthorizationState]
     *
     * @return [AuthorizationState] or null if it's not found.
     */
    fun findById(id: String): AuthorizationState?

    /**
     * Stores a newly created [AuthorizationState] or
     * updates it, if it is already present (by its ID).
     *
     * @param state The [AuthorizationState] object to store.
     */
    fun upsert(state: AuthorizationState)
}
