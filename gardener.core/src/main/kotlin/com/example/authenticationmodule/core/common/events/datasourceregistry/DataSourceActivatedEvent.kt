package com.example.authenticationmodule.core.common.events.datasourceregistry

import com.example.authenticationmodule.core.common.events.eventbus.DataSourceEvent

/**
 * [DataSourceEvent] definition for data source activation.
 *
 * Fired when a data source is activated.
 */
data class DataSourceActivatedEvent(override val dataSourceId: String): DataSourceEvent()