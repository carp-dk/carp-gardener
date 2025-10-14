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
    ): IDataCollectionOperator =
        OAuth1Operator(clientSettings)
}
