package com.example.authenticationmodule.implementation.oauth2

import com.example.authenticationmodule.core.authorization.datasource.oauth2.OAuth2ClientSettings
import com.example.authenticationmodule.core.collection.IDataCollectionOperator
import com.example.authenticationmodule.core.collection.oauth2.IOAuth2DataCollectionOperatorBuilder
import com.example.authenticationmodule.verticles.PropertiesConfig
import io.vertx.core.Vertx

/**
 * Provides an implementation for [IOAuth2DataCollectionOperatorBuilder].
 */
class OAuth2DataCollectionOperatorBuilder(private val vertx: Vertx, private val properties: PropertiesConfig) : IOAuth2DataCollectionOperatorBuilder {

    /**
     * Returns an OAuth2 specific [IDataCollectionOperator] configured with the [clientSettings]
     */
    override fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth2ClientSettings): IDataCollectionOperator {
        return OAuth2Operator(clientSettings, vertx, properties)
    }

}