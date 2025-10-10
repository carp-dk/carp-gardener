package dk.carp.gardener.authentication.core.common.util.uri

import com.fasterxml.jackson.databind.JsonNode

/**
 * Represents a URI.
 */
data class Uri(
    val method: HttpMethod,
    val uri: String,
    val queryParams: Map<String, String>? = null,
    val body: JsonNode? = null
)
