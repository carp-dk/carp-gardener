package dk.carp.gardener.authentication.core.collection.data

import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import java.time.Instant

/**
 * Represents a processed third-party data.
 */
data class TransformedData(
    /**
     * From which data source the data came from.
     */
    val dataSourceId: String,
    /**
     * Whose user's data this is.
     */
    val userId: String,
    /**
     * What kind of data this is.
     */
    val dataType: DataCollectionType,
    /**
     * Collection timestamp.
     */
    val collectedAt: Instant,
    /**
     * The third-party data in the correct format.
     */
    val value: Any
)