package dk.carp.gardener.authentication.core.authorization.devices.fitbit

import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper

object FitbitAccessTokenResponseExtractor {
    /**
     * Extracts [OAuth2AccessParams] from a Fitbit specific OAuth2 Access parameters [rawResponse].
     *
     * @throws IllegalArgumentException When the access parameters cannot be extracted from the [rawResponse].
     */
    fun getAccessParamsFromResponse(
        userId: String,
        dataSourceId: String,
        rawResponse: String,
    ): OAuth2AccessParams {
        val node = ConfiguredObjectMapper.instance.readTree(rawResponse)

        require(node.get("user_id") != null) {
            "External user ID (user_id) is missing in the response: ${node.toPrettyString()}"
        }

        return OAuth2AccessParams(
            internalUserId = userId,
            dataSourceId = dataSourceId,
            params = node,
            externalUserId = node.get("user_id").textValue(),
        )
    }
}
