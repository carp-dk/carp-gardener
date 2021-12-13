package com.example.authenticationmodule.implementation.oauth1

import com.github.scribejava.core.builder.api.DefaultApi10a

/**
 * Custom implementation of scribejava's [DefaultApi10a].
 */
class OAuth1ApiDefinition(
    private val requestTokenUri: String,
    private val accessTokenUri: String,
    private val authorizationUri: String
) : DefaultApi10a() {

    override fun getRequestTokenEndpoint(): String {
        return requestTokenUri
    }

    override fun getAccessTokenEndpoint(): String {
        return accessTokenUri
    }

    override fun getAuthorizationBaseUrl(): String {
        return authorizationUri
    }

}