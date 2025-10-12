package dk.carp.gardener.authentication.core.common.events.datasourceregistry

import dk.carp.gardener.authentication.core.common.events.eventbus.DataSourceEvent

/**
 * [DataSourceEvent] definition for data source activation.
 *
 * Fired when a data source is activated.
 */
data class DataSourceActivatedEvent(
    override val dataSourceId: String,
) : DataSourceEvent()
