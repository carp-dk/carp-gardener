package dk.carp.gardener.authentication.implementation.transformer.carp

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.io.Serializable

/**
 * CARP data point DTO Format (CARP Core v28)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CarpDataPointFormat(
    val namespace: String,
    var name: String,
) : Serializable
