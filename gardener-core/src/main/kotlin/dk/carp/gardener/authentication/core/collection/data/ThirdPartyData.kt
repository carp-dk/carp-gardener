package dk.carp.gardener.authentication.core.collection.data

import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant

/**
 * Represents a data response from the third-party Web APIs
 */
data class ThirdPartyData(
    /**
     * Whose user's data this is.
     */
    val userId: String,
    /**
     * From which data source the data came from.
     */
    val dataSourceId: String,
    /**
     * What kind of data this is.
     */
    val dataIdentifier: DataCollectionType,
    /**
     * The unprocessed response from the Web API.
     */
    val rawResponse: JsonNode,
    /**
     * Application specific data.
     */
    val applicationData: String?,
    /**
     * Collection timestamp.
     */
    val collectedAt: Instant = Instant.now()
) {
    /**
     * Transforms the data into the configured format
     * according to the passed transformer.
     */
    fun transform(transformer: IDataTypeTransformer): List<TransformedData> {
        val transformedDataTypes = this.dataIdentifier.acceptTransformer(transformer, this)
        return transformedDataTypes.map { transformedDataType ->
            TransformedData(
                dataSourceId = dataSourceId,
                userId = userId,
                dataType = dataIdentifier,
                collectedAt = collectedAt,
                value = transformedDataType
            )
        }
    }
}