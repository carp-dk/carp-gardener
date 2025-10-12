package dk.carp.gardener.authentication.core.common.events.datacollection

import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.events.eventbus.IntegrationEvent
import java.time.Instant

/**
 * [IntegrationEvent] definitions for successful collection.
 *
 * Fired when data is successfully collected for a user.
 */
data class DataSuccessfullyCollectedEvent(
    /**
     * ID of the data source where the data was collected from.
     */
    val dataSourceId: String,
    /**
     * ID of the user the data belongs to.
     */
    val userId: String,
    /**
     * The type of data that was collected.
     */
    val dataType: DataCollectionType,
    /**
     * Time of collection.
     */
    val collectedAt: Instant,
) : IntegrationEvent()
