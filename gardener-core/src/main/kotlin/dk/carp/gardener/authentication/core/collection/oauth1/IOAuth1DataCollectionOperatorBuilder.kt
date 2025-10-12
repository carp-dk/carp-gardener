package dk.carp.gardener.authentication.core.collection.oauth1

import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1ClientSettings
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator

interface IOAuth1DataCollectionOperatorBuilder {
    /**
     * Returns an OAuth1 specific [IDataCollectionOperator] configured with the [clientSettings]
     */
    fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth1ClientSettings): IDataCollectionOperator
}
