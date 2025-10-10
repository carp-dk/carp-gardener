package dk.carp.gardener.authentication.core.common.accessparams

/**
 * Manages [AccessParams].
 */
interface IAccessParamsService {

    /**
     * Stores a newly created [AccessParams] or
     * updates it, if it is already present (by its ID).
     */
    fun addParams(accessParams: AccessParams)

    /**
     * Returns the most recent [AccessParams] entry
     * for the user and data source.
     *
     * @param userId Either internal or external user id.
     * @param dataSourceId ID of the data source
     *
     * @return [AccessParams]
     *
     * @throws IllegalArgumentException When no params are found for the user and data source.
     */
    fun getCurrentForInternalUserIdAndDataSource(userId: String, dataSourceId: String): AccessParams

    /**
     * Checks whether an [AccessParams] entry is present.
     *
     * @param userId Either internal or external user id.
     * @param dataSourceId ID of the data source
     *
     * @return True if it is present, false otherwise.
     */
    fun isUserAlreadyRegistered(userId: String, dataSourceId: String): Boolean

}