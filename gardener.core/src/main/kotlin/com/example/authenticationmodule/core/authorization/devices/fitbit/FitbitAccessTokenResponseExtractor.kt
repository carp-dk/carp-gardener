package com.example.authenticationmodule.core.authorization.devices.fitbit

import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper
import com.example.authenticationmodule.core.common.accessparams.OAuth2AccessParams

class FitbitAccessTokenResponseExtractor {

    companion object {
        /**
         * Extracts [OAuth2AccessParams] from a Fitbit specific OAuth2 Access parameters [rawResponse].
         *
         * @throws IllegalArgumentException When the access parameters cannot be extracted from the [rawResponse].
         */
        fun getAccessParamsFromResponse(userId: String, dataSourceId: String, rawResponse: String): OAuth2AccessParams {
            val node = ConfiguredObjectMapper.instance.readTree(rawResponse)
            if (node.get("access_token") == null) {
                throw IllegalArgumentException("Access parameters cannot be extracted for $dataSourceId/$userId: ${node.toPrettyString()}")
            }

            return OAuth2AccessParams(
                internalUserId = userId,
                dataSourceId = dataSourceId,
                params = node,
                externalUserId = node.get("user_id").textValue()
            )
        }
    }

}