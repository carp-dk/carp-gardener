package dk.carp.gardener.authentication.core.common.accessparams

class AccessParamsServiceHost(private val repository: IAccessParamsRepository) : IAccessParamsService {

    /**
     * Stores a newly created [AccessParams] or
     * updates it, if it is already present (by its ID).
     */
    override fun addParams(accessParams: AccessParams) {
        repository.upsert(accessParams)
    }

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
    override fun getCurrentForInternalUserIdAndDataSource(userId: String, dataSourceId: String): AccessParams {
        return repository.getLatestByInternalOrExternalUserIdAndDataSourceId(userId, dataSourceId) ?:
            throw IllegalArgumentException("Access params not found for $dataSourceId/$userId")
    }

    /**
     * Checks whether an [AccessParams] entry is present.
     *
     * @param userId Either internal or external user id.
     * @param dataSourceId ID of the data source
     *
     * @return True if it is present, false otherwise.
     */
    override fun isUserAlreadyRegistered(userId: String, dataSourceId: String): Boolean {
        return repository.existsByInternalOrExternalUserIdAndDataSourceId(userId, dataSourceId)
    }
}