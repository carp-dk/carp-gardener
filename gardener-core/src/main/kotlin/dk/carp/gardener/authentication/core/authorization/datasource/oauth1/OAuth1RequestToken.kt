package dk.carp.gardener.authentication.core.authorization.datasource.oauth1

/**
 * Represents the OAuth1 Unsigned Token pair.
 */
data class OAuth1RequestToken(val requestToken: String, val tokenSecret: String)