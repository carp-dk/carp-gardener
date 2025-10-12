package dk.carp.gardener.authentication.core.authorization.authorizationstate

/**
 * OAuth1 specific [AuthorizationState].
 * Allows storing [requestToken]s and [tokenSecret]s in the state
 * during the OAuth1 authorization flow.
 */
class OAuth1AuthorizationState(
    userId: String,
    dataSourceId: String,
    val requestToken: String,
    val tokenSecret: String,
    applicationData: String? = null,
) : AuthorizationState(userId = userId, dataSourceId = dataSourceId, applicationData = applicationData)
