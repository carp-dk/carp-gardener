package com.example.authenticationmodule.core.common.events.datacollection

import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.events.eventbus.DataSourceEvent
import com.example.authenticationmodule.core.common.events.eventbus.IntegrationEvent
import com.fasterxml.jackson.databind.JsonNode

/**
 * [DataSourceEvent] definition for data collection preparation.
 *
 * Fired when a notification is received signaling that data is available
 * for collecting on the third-party API.
 */
data class DataCollectionPreparationEvent(
    /**
     * ID of the data source where the data needs to be collected.
     */
    override val dataSourceId: String,
    /**
     * ID of the user the data belongs to.
     */
    val userId: String,
    /**
     * Type of the data.
     */
    val dataType: DataCollectionType,
    /**
     * Raw ping notification from the third-part API
     * that may contain necessary information for collection.
     */
    val rawPing: JsonNode
) : DataSourceEvent()