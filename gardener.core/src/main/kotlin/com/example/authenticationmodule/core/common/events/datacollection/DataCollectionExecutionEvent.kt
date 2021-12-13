package com.example.authenticationmodule.core.common.events.datacollection

import com.example.authenticationmodule.core.common.accessparams.AccessParams
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.util.uri.Uri
import com.example.authenticationmodule.core.common.accessparams.OAuth1AccessParams
import com.example.authenticationmodule.core.authorization.datasource.oauth1.OAuth1ClientSettings
import com.example.authenticationmodule.core.common.accessparams.OAuth2AccessParams
import com.example.authenticationmodule.core.authorization.datasource.oauth2.OAuth2ClientSettings
import com.example.authenticationmodule.core.common.events.eventbus.IntegrationEvent

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