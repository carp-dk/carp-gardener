package dk.carp.gardener.authentication.implementation.oauth2

import com.github.scribejava.core.builder.api.DefaultApi20

/**
 * Custom implementation of scribejava's [DefaultApi20].
 */
class OAuth2ApiDefinition(
    private val accessTokenEndpoint: String,
    private val authorizationBaseUrl: String,
) : DefaultApi20() {
    override fun getAccessTokenEndpoint(): String = accessTokenEndpoint

    override fun getAuthorizationBaseUrl(): String = authorizationBaseUrl
}
