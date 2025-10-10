package dk.carp.gardener.authentication.core.collection.oauth2

import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings

interface IOAuth2DataCollectionOperatorBuilder {

    /**
     * Returns an OAuth2 specific [IDataCollectionOperator] configured with the [clientSettings]
     */
    fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth2ClientSettings): IDataCollectionOperator

}