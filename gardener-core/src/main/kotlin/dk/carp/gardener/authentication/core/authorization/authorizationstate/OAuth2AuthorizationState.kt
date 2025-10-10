package dk.carp.gardener.authentication.core.authorization.authorizationstate

/**
 * OAuth2 specific [AuthorizationState].
 */
class OAuth2AuthorizationState(
    userId: String,
    dataSourceId: String,
    applicationData: String? = null
) : AuthorizationState(userId = userId, dataSourceId = dataSourceId, applicationData = applicationData)