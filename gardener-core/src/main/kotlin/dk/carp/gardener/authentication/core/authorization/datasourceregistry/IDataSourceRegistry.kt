package dk.carp.gardener.authentication.core.authorization.datasourceregistry

import dk.carp.gardener.authentication.core.authorization.datasource.IDataSource


/**
 * Application service which stores and retrieves [IDataSource] instances.
 */
interface IDataSourceRegistry {

    /**
     * Returns an [IDataSource] instance with the given [id].
     *
     * @throws IllegalArgumentException When there is no Data Source
     * with the given id.
     */
    fun getDataSourceById(id: String): IDataSource

    /**
     * Inserts a new [dataSource].
     *
     * @throws IllegalArgumentException When the [dataSource] has already been activated.
     */
    fun activateDataSource(dataSource: IDataSource)
}