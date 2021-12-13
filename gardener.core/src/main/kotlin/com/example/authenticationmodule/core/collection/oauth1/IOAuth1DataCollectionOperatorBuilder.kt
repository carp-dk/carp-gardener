package com.example.authenticationmodule.core.collection.oauth1

import com.example.authenticationmodule.core.collection.IDataCollectionOperator
import com.example.authenticationmodule.core.authorization.datasource.oauth1.OAuth1ClientSettings

interface IOAuth1DataCollectionOperatorBuilder {

    /**
     * Returns an OAuth1 specific [IDataCollectionOperator] configured with the [clientSettings]
     */
    fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth1ClientSettings): IDataCollectionOperator

}