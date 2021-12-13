package com.example.authenticationmodule.core.authorization.datasourceregistry

import com.example.authenticationmodule.core.authorization.datasource.IDataSource
import com.example.authenticationmodule.core.authorization.datasource.oauth1.IOAuth1AuthorizationOperator
import com.example.authenticationmodule.core.authorization.datasource.oauth1.OAuth1ClientSettings
import com.example.authenticationmodule.core.authorization.datasource.oauth1.OAuth1DataSource
import com.example.authenticationmodule.core.authorization.datasource.oauth2.IOAuth2AuthorizationOperator
import com.example.authenticationmodule.core.authorization.datasource.oauth2.OAuth2ClientSettings
import com.example.authenticationmodule.core.authorization.datasource.oauth2.OAuth2DataSource
import com.example.authenticationmodule.core.common.events.datasourceregistry.DataSourceActivatedEvent


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