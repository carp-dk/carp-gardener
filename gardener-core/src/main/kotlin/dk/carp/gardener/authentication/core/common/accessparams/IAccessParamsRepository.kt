package dk.carp.gardener.authentication.core.common.accessparams

/**
 * Repository to manage [AccessParams] instances.
 */
interface IAccessParamsRepository {

    /**
     * Stores a newly created [AccessParams] or
     * updates it, if it is already present (by its ID).
     *
     * @param params The [AccessParams] object to store.
     */
    fun upsert(params: AccessParams)

    /**
     * Returns the most recent [AccessParams] entry
     * for the user and data source.
     *
     * @param userId Either internal or external user id.
     * @param dataSourceId ID of the data source
     *
     * @return [AccessParams] or null if there is no [AccessParams] for the user/data source.
     */
    fun getLatestByInternalOrExternalUserIdAndDataSourceId(userId: String, dataSourceId: String): AccessParams?

    /**
     * Checks whether an [AccessParams] entry is present.
     *
     * @param userId Either internal or external user id.
     * @param dataSourceId ID of the data source
     *
     * @return True if it is present, false otherwise.
     */
    fun existsByInternalOrExternalUserIdAndDataSourceId(userId: String, dataSourceId: String): Boolean

}