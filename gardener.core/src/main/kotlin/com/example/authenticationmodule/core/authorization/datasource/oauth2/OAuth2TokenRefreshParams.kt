package com.example.authenticationmodule.core.authorization.datasource.oauth2

import com.example.authenticationmodule.core.common.util.collections.RestrictedMap

/**
 * Data class used to store additional information
 * for the OAuth2 token refresh processes.
 */
data class OAuth2TokenRefreshParams(
    val additionalParams: RestrictedMap<String, String> = RestrictedMap()
)