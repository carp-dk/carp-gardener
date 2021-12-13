package com.example.authenticationmodule.core.authorization.devices.withings

import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper
import com.example.authenticationmodule.core.common.accessparams.OAuth2AccessParams

class WithingsAccessTokenResponseExtractor {

    companion object {
        /**
         * Extracts [OAuth2AccessParams] from a Withings specific OAuth2 Access parameters [rawResponse].
         *
         * @throws IllegalArgumentException When the access parameters cannot be extracted from the [rawResponse].
         */
        fun getAccessParamsFromResponse(userId: String, dataSourceId: String, rawResponse: String): OAuth2AccessParams {
            val node = ConfiguredObjectMapper.instance.readTree(rawResponse).get("body")
            if (node.get("access_token") == null) {
                throw IllegalArgumentException("Access parameters cannot be extracted for $dataSourceId/$userId: ${node.toPrettyString()}")
            }

            return OAuth2AccessParams(
                internalUserId = userId,
                dataSourceId = dataSourceId,
                params = node,
                externalUserId = node.get("userid").textValue()
            )
        }
    }
}