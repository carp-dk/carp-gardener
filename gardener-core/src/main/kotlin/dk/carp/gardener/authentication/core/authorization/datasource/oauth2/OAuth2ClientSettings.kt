package dk.carp.gardener.authentication.core.authorization.datasource.oauth2

/**
 * Contains OAuth2 client specific information about
 * the third-party vendor.
 */
data class OAuth2ClientSettings(
    /**
     * Client ID
     */
    val clientId: String,
    /**
     * Client Secret
     */
    val clientSecret: String,
    /**
     * The OAuth2 Authorization URI of the vendor.
     */
    val authorizationUri: String,
    /**
     * The OAuth2 Token management endpoint of the vendor.
     */
    val accessUri: String,
    /**
     * The base URI of the vendor where the data
     * can be collected from. Not data type specific.
     */
    val dataUrl: String,
    /**
     * The application callback URI the vendor can call
     * during the authorization process.
     */
    val callbackUri: String,
)
