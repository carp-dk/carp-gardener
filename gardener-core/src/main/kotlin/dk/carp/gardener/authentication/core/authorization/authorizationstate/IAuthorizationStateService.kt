package dk.carp.gardener.authentication.core.authorization.authorizationstate

/**
 * Allows creating and manipulating [AuthorizationState] objects.
 */
interface IAuthorizationStateService {

    /**
     * Retrieves an [AuthorizationState] object by its ID.
     *
     * @param id The ID of the [AuthorizationState]
     *
     * @return [AuthorizationState] or null if it's not found.
     *
     * @throws IllegalArgumentException When no state entry is found with the given [id].
     */
    fun getById(id: String): AuthorizationState

    /**
     * Stores a newly created [AuthorizationState] or
     * updates it, if it is already present (by its ID).
     *
     * @param state The [AuthorizationState] object to store.
     */
    fun save(state: AuthorizationState)

    /**
     * Sets the successful flag to true on the given [AuthorizationState] object
     * and saves it.
     *
     * @param state The [AuthorizationState] object to update.
     */
    fun setSuccessfulState(state: AuthorizationState)

}