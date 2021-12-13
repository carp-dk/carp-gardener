package com.example.authenticationmodule.implementation.transformer.carp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * CARP data point DTO (CARP Core v28)
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class CarpDataPoint(
    var carpHeader: CarpDataPointHeader? = null,
    var carpBody: JsonNode? = null
)