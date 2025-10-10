package dk.carp.gardener.authentication.core.common.events.datacollection

import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1ClientSettings
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.common.events.eventbus.IntegrationEvent

/**
 * [IntegrationEvent] definitions for data collection.
 *
 * Fired when data needs to be collected for a user.
 */
sealed class DataCollectionExecutionEvent(
    /**
     * Access parameters for authentication.
     * Contains the user and data source IDs.
     */
    val accessParams: AccessParams,
    /**
     * Type of data that needs to be collected.
     */
    val dataIdentifier: DataCollectionType,
    /**
     * Where the data can be collected.
     */
    val uri: Uri
) : IntegrationEvent() {

    /**
     * OAuth1 specific [DataCollectionExecutionEvent].
     */
    class OAuth1ExecutionEvent(
        accessParams: OAuth1AccessParams,
        dataIdentifier: DataCollectionType,
        uri: Uri,
        val clientSettings: OAuth1ClientSettings
    ) : DataCollectionExecutionEvent(accessParams, dataIdentifier, uri)

    /**
     * OAuth2 specific [DataCollectionExecutionEvent].
     */
    class OAuth2ExecutionEvent(
        accessParams: OAuth2AccessParams,
        dataIdentifier: DataCollectionType,
        uri: Uri,
        val clientSettings: OAuth2ClientSettings
    ) : DataCollectionExecutionEvent(accessParams, dataIdentifier, uri)

}