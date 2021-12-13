package com.example.authenticationmodule.datasourceregistry

import com.example.authenticationmodule.base.OAuth2Test
import com.example.authenticationmodule.core.authorization.datasource.IDataSource
import com.example.authenticationmodule.core.authorization.datasourceregistry.DataSourceRegistryHost
import com.example.authenticationmodule.core.authorization.devices.fitbit.FitbitDataSource
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
