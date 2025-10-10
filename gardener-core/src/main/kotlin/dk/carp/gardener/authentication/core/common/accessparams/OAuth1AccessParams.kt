package dk.carp.gardener.authentication.core.common.accessparams

import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import com.fasterxml.jackson.databind.JsonNode

/**
 * OAuth1 specific [AccessParams].
 */
class OAuth1AccessParams(
    internalUserId: String,
    dataSourceId: String,
    params: JsonNode,
    externalUserId: String? = null,
    applicationData: String? = null
) : AccessParams(internalUserId, dataSourceId, params, externalUserId, applicationData = applicationData) {

    companion object {
        const val OAUTH_TOKEN_KEY = "oauth_token"
        const val OAUTH_TOKEN_SECRET_KEY = "oauth_token_secret"
    }

    constructor(internalUserId: String, dataSourceId: String, rawResponse: String, externalUserId: String? = null, applicationData: String? = null)
            : this(internalUserId, dataSourceId, ConfiguredObjectMapper.instance.readTree(rawResponse), externalUserId, applicationData = applicationData)

    /**
     * Returns the oauth_token field.
     */
    override fun extractAccessToken(): String {
        return params.get(OAUTH_TOKEN_KEY).textValue()
    }

    /**
     * Returns the oauth_token_secret field.
     */
    fun extractTokenSecret(): String {
        return params.get(OAUTH_TOKEN_SECRET_KEY).textValue()
    }
}