package dk.carp.gardener.authentication.implementation.oauth2

import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.collection.oauth2.IOAuth2DataCollectionOperatorBuilder
import dk.carp.gardener.authentication.verticles.PropertiesConfig
import io.vertx.core.Vertx

/**
 * Provides an implementation for [IOAuth2DataCollectionOperatorBuilder].
 */
class OAuth2DataCollectionOperatorBuilder(
    private val vertx: Vertx,
    private val properties: PropertiesConfig,
) : IOAuth2DataCollectionOperatorBuilder {
    /**
     * Returns an OAuth2 specific [IDataCollectionOperator] configured with the [clientSettings]
     */
    override fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth2ClientSettings): IDataCollectionOperator =
        OAuth2Operator(clientSettings, vertx, properties)
}
