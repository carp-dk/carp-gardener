package dk.carp.gardener.authentication.core.authorization.datasourceregistry

import dk.carp.gardener.authentication.core.authorization.datasource.IDataSource
import dk.carp.gardener.authentication.core.common.events.datasourceregistry.DataSourceActivatedEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus

/**
 * Keeps track of configured data sources.
 */
class DataSourceRegistryHost(private val eventBus: IEventBus) :
    IDataSourceRegistry {

    /**
     * Holds the instantiated [IDataSource] instances with their ID as key.
     */
    private val dataSources: MutableMap<String, IDataSource> = mutableMapOf()

    /**
     * Returns an [IDataSource] instance with the given [id].
     *
     * @throws IllegalArgumentException When there is no Data Source
     * with the given id.
     */
    override fun getDataSourceById(id: String): IDataSource {
        return dataSources[id] ?: throw IllegalArgumentException("Data source with id $id is not found.")
    }

    /**
     * Inserts the [dataSource] to the [dataSources] collection
     * and publishes a [DataSourceActivatedEvent].
     *
     * @throws IllegalArgumentException When the [dataSource] has already been activated.
     */
    override fun activateDataSource(dataSource: IDataSource) {
        if (isDataSourceActivated(dataSource.getId())) {
            throw IllegalArgumentException("Data source with id ${dataSource.getId()} has already been activated!")
        }
        dataSources[dataSource.getId()] = dataSource
        eventBus.publish(DataSourceRegistryHost::class, DataSourceActivatedEvent(dataSource.getId()))
    }

    /**
     * Checks if the [dataSources] collection contains an instance
     * with the given [dataSourceId].
     */
    private fun isDataSourceActivated(dataSourceId: String): Boolean {
        return dataSources.containsKey(dataSourceId)
    }

}