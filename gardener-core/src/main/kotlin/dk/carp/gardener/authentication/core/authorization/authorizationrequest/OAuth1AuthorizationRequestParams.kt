package dk.carp.gardener.authentication.core.authorization.authorizationrequest

import dk.carp.gardener.authentication.core.common.util.collections.RestrictedMap

/**
 * OAuth1 specific parameters class that declares collections that can be used
 * to customize the authorization process.
 */
class OAuth1AuthorizationRequestParams(
    /**
     * Additional key-value parameters that should be sent to the vendor
     * during the OAuth1 signed token retrieval phase.
     */
    additionalParamsForGrants: RestrictedMap<String, String> = RestrictedMap(),
    /**
     * Additional key-value parameters that should be sent to the vendor
     * during the OAuth1 long-lived token retrieval phase.
     */
    additionalParamsForTokens: RestrictedMap<String, String> = RestrictedMap(),
    /**
     * Additional key-value parameters that should be sent to the vendor
     * during the OAuth1 unsigned token retrieval phase.
     */
    val additionalParamsForUnsignedToken: RestrictedMap<String, String> = RestrictedMap(),
    /**
     * Application specific data.
     */
    applicationData: String? = null,
) : AuthorizationRequestParams(additionalParamsForGrants, additionalParamsForTokens, applicationData)
