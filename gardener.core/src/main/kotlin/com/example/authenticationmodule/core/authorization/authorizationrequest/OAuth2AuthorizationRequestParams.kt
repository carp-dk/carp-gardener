package com.example.authenticationmodule.core.authorization.authorizationrequest

import com.example.authenticationmodule.core.common.util.collections.RestrictedList
import com.example.authenticationmodule.core.common.util.collections.RestrictedMap

/**
 * OAuth2 specific parameters class that declares collections that can be used
 * to customize the authorization process.
 */
class OAuth2AuthorizationRequestParams(
    /**
     * Additional key-value parameters that should be sent to the vendor
     * during the OAuth2 authorization code grant phase.
     */
    additionalParamsForGrants: RestrictedMap<String, String> = RestrictedMap(),

    /**
     * Additional key-value parameters that should be sent to the vendor
     * during the OAuth2 access token retrieval phase.
     */
    additionalParamsForTokens: RestrictedMap<String, String> = RestrictedMap(),

    /**
     * Contains the scopes the client application requests from the vendor during
     * the authorization process.
     */
    val scopes: RestrictedList<String> = RestrictedList(),

    /**
     * Application specific data.
     */
    applicationData: String? = null
) : AuthorizationRequestParams(additionalParamsForGrants, additionalParamsForTokens, applicationData)