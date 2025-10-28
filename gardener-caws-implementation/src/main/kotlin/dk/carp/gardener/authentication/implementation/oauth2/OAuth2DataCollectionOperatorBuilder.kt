package dk.carp.gardener.authentication.implementation.oauth2

import com.github.scribejava.core.oauth.OAuth20Service
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.collection.oauth2.IOAuth2DataCollectionOperatorBuilder

/**
 * Provides an implementation for [IOAuth2DataCollectionOperatorBuilder].
 */
class OAuth2DataCollectionOperatorBuilder : IOAuth2DataCollectionOperatorBuilder {
    /**
     * Returns an OAuth2 specific [IDataCollectionOperator] configured with the [clientSettings]
     */
    override fun createDataCollectionOperatorWithClientSettings(
        clientSettings: OAuth2ClientSettings
    ): IDataCollectionOperator {
        // Construct the underlying blocking OAuth20Service for ScribeJava
        val api = OAuth2ApiDefinition(clientSettings.accessUri, clientSettings.authorizationUri)
        val service: OAuth20Service =
            com.github.scribejava.core.builder
                .ServiceBuilder(clientSettings.clientId)
                .apiSecret(clientSettings.clientSecret)
                .callback(clientSettings.callbackUri)
                .build(api)

        val coroutineOp = CoroutineOAuth2Operator(clientSettings, service)
        return AdapterOAuth2Operator(coroutineOp)
    }
}
