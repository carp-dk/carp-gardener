package dk.carp.gardener.authentication.core.common.accessparams

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import java.time.Instant

/**
 * OAuth2 specific [AccessParams].
 */
class OAuth2AccessParams(
    internalUserId: String,
    dataSourceId: String,
    params: JsonNode,
    externalUserId: String? = null,
    applicationData: String? = null,
) : AccessParams(internalUserId, dataSourceId, params, externalUserId, applicationData = applicationData) {
    companion object {
        const val ACCESS_TOKEN_KEY = "access_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val TOKEN_TYPE_KEY = "token_type"
        const val EXPIRES_IN_KEY = "expires_in"
        const val SCOPES_KEY = "scope"
        private const val TOKEN_EXPIRY_GRACE_SECONDS = 180L
    }

    constructor(
        internalUserId: String,
        dataSourceId: String,
        rawJsonResponse: String,
        externalUserId: String? = null,
        applicationData: String? = null,
    ) : this(
        internalUserId,
        dataSourceId,
        ConfiguredObjectMapper.instance.readTree(rawJsonResponse),
        externalUserId,
        applicationData = applicationData,
    )

    /**
     * Returns the access_token field.
     *
     * This will always be present in the access token response.
     * (Required field according to the IETF)
     */
    override fun extractAccessToken(): String = params.get(ACCESS_TOKEN_KEY).textValue()

    /**
     * Returns the token_type field.
     *
     * This will always be present in the access token response.
     * (Required field according to the IETF)
     */
    fun extractTokenType(): String = params.get(TOKEN_TYPE_KEY).textValue()

    /**
     * Returns the expires_in field.
     */
    fun extractExpiresIn(): Long? = params.get(EXPIRES_IN_KEY)?.asLong()

    /**
     * Returns the refresh_token field.
     */
    fun extractRefreshToken(): String? = params.get(REFRESH_TOKEN_KEY)?.textValue()

    /**
     * Returns the scope field.
     */
    fun extractScopes(): String? = params.get(SCOPES_KEY)?.textValue()

    /**
     * Determines whether a token is expired or not.
     * It calculates with a minus offset of 3 minutes to ensure token validity.
     */
    fun determineExpiration(): Boolean {
        val expiresIn = extractExpiresIn() ?: return true
        return updatedAt.plusSeconds(expiresIn - TOKEN_EXPIRY_GRACE_SECONDS) < Instant.now()
    }
}
