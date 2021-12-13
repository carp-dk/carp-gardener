package com.example.authenticationmodule.implementation.oauth1

import com.example.authenticationmodule.core.authorization.datasource.oauth1.OAuth1ClientSettings
import com.example.authenticationmodule.core.collection.IDataCollectionOperator
import com.example.authenticationmodule.core.collection.oauth1.IOAuth1DataCollectionOperatorBuilder

/**
 * Provides an implementation for [IOAuth1DataCollectionOperatorBuilder].
 */
class OAuth1DataCollectionOperatorBuilder : IOAuth1DataCollectionOperatorBuilder {

    /**
     * Returns an OAuth1 specific [IDataCollectionOperator] configured with the [clientSettings]
     */
    override fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth1ClientSettings): IDataCollectionOperator {
        return OAuth1Operator(clientSettings)
    }

}