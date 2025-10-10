package dk.carp.gardener.authentication.core.authorization.authorizationstate

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import java.util.*

/**
 * Used to store state information in-between calls
 * during the authorization process.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = OAuth1AuthorizationState::class, name = "oauth1"),
    JsonSubTypes.Type(value = OAuth2AuthorizationState::class, name = "oauth2")
)
abstract class AuthorizationState(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Instant = Instant.now(),
    var success: Boolean = false,
    val userId: String,
    val dataSourceId: String,
    var applicationData: String? = null
)