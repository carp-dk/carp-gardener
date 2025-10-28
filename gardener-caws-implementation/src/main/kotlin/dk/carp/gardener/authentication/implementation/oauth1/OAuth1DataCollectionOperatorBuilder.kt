package dk.carp.gardener.authentication.implementation.oauth1

import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1ClientSettings
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.collection.oauth1.IOAuth1DataCollectionOperatorBuilder

/**
 * Provides an implementation for [IOAuth1DataCollectionOperatorBuilder].
 */
class OAuth1DataCollectionOperatorBuilder : IOAuth1DataCollectionOperatorBuilder {
    /**
     * Returns an OAuth1 specific [IDataCollectionOperator] configured with the [clientSettings]
     */
    override fun createDataCollectionOperatorWithClientSettings(
        clientSettings: OAuth1ClientSettings
    ): IDataCollectionOperator {
        // Build Scribe service and return adapter backed by coroutine operator
        val service =
            com.github.scribejava.core.builder
                .ServiceBuilder(clientSettings.consumerKey)
                .apiSecret(clientSettings.consumerSecret)
                .build(OAuth1ApiDefinition(clientSettings.requestTokenUri, clientSettings.accessTokenUri, clientSettings.authorizationUri))
        val coroutineOp = CoroutineOAuth1Operator(clientSettings, service)
        return AdapterOAuth1Operator(coroutineOp)
    }
}
