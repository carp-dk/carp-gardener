package dk.carp.gardener.authentication.datasourceregistry

import dk.carp.gardener.authentication.base.OAuth2Test
import dk.carp.gardener.authentication.core.authorization.datasource.IDataSource
import dk.carp.gardener.authentication.core.authorization.datasourceregistry.DataSourceRegistryHost
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests the functionality of [DataSourceRegistryHost].
 */
class DataSourceRegistryTest : OAuth2Test() {
    @Test
    fun dataSourceCanBeSaved() {
        saveDataSource(fitbitDataSource)
    }

    @Test
    fun dataSourceCanBeRetrieved() {
        saveDataSource(fitbitDataSource)
        val retrieved = dataSourceRegistry.getDataSourceById(FitbitDataSource.DATA_SOURCE_ID)
        assertEquals(FitbitDataSource.DATA_SOURCE_ID, retrieved.getId())
    }

    @Test
    fun dataSourcesCannotBeActivatedTwice() {
        saveDataSource(fitbitDataSource)
        assertFailsWith<IllegalArgumentException> { saveDataSource(fitbitDataSource) }
    }

    fun saveDataSource(dataSource: IDataSource) {
        dataSourceRegistry.activateDataSource(dataSource)
    }
}
