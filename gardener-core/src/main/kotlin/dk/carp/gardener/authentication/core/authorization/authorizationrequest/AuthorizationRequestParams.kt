package dk.carp.gardener.authentication.core.authorization.authorizationrequest

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dk.carp.gardener.authentication.core.common.util.collections.RestrictedMap

/**
 * A superclass that declares collections that can be used
 * to customize the authorization process.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = OAuth1AuthorizationRequestParams::class, name = "oauth1"),
    JsonSubTypes.Type(value = OAuth2AuthorizationRequestParams::class, name = "oauth2")
)
abstract class AuthorizationRequestParams(
    /**
     * Additional key-value parameters that should be sent to the vendor
     * during the token granting phase.
     */
    val additionalParamsForGrants: RestrictedMap<String, String>,

    /**
     * Additional key-value parameters that should be sent to the vendor
     * during the access token retrieval phase.
     */
    val additionalParamsForTokens: RestrictedMap<String, String>,

    /**
     * Application specific data.
     */
    var applicationData: String? = null
)