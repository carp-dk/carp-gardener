package dk.carp.gardener.authentication.core.common.accessparams

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

/**
 * Holds access parameters for users and data sources.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = OAuth1AccessParams::class, name = "oauth1"),
    JsonSubTypes.Type(value = OAuth2AccessParams::class, name = "oauth2"),
)
@Suppress("LongParameterList")
abstract class AccessParams(
    /**
     * The application specific identifier of the user.
     * The name the user authorized themselves with.
     */
    val internalUserId: String,
    /**
     * The ID of the data source the params belong to.
     */
    val dataSourceId: String,
    /**
     * The raw response from the third-party API upon
     * successful authorization.
     */
    params: JsonNode,
    /**
     * The ID of the user that is used by the third-party API
     * to identify.
     */
    val externalUserId: String? = null,
    /**
     * Unique ID of the object.
     */
    val id: String = UUID.randomUUID().toString(),
    /**
     * When the object was created.
     */
    val createdAt: Instant = Instant.now(),
    /**
     * When the object was updated.
     */
    updatedAt: Instant = Instant.now(),
    /**
     * Application specific data to store arbitrary data.
     */
    var applicationData: String? = null,
) {
    var updatedAt: Instant = updatedAt
        private set

    var params: JsonNode = params
        set(value) {
            field = value
            this.updatedAt = Instant.now()
        }

    /**
     * Returns the access token.
     */
    abstract fun extractAccessToken(): String

    /**
     * Return the value of a [key] if present.
     */
    fun getParamValueFor(key: String): String =
        params.get(key)?.textValue()
            ?: error("Key $key is not present in the additional parameters field.")
}
