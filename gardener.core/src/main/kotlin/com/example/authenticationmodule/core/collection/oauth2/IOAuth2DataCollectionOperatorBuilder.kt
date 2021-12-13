package com.example.authenticationmodule.core.collection.oauth2

import com.example.authenticationmodule.core.collection.IDataCollectionOperator
import com.example.authenticationmodule.core.authorization.datasource.oauth2.OAuth2ClientSettings

interface IOAuth2DataCollectionOperatorBuilder {

    /**
     * Returns an OAuth2 specific [IDataCollectionOperator] configured with the [clientSettings]
     */
    fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth2ClientSettings): IDataCollectionOperator

}