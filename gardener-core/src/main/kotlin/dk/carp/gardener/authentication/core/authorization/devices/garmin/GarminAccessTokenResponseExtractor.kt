package dk.carp.gardener.authentication.core.authorization.devices.garmin

import dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import java.util.regex.Pattern

class GarminAccessTokenResponseExtractor {

    companion object {

        private val OAUTH_TOKEN_REGEXP_PATTERN = Pattern.compile("oauth_token=([^&]+)")
        private val  OAUTH_TOKEN_SECRET_REGEXP_PATTERN = Pattern.compile("oauth_token_secret=([^&]*)")

        /**
         * Extracts [OAuth1AccessParams] from a Garmin specific OAuth1 Access parameters [rawResponse].
         *
         * @throws IllegalArgumentException When the access parameters cannot be extracted from the [rawResponse].
         */
        fun getAccessParamsFromResponse(
            userId: String,
            dataSourceId: String,
            rawResponse: String
        ): OAuth1AccessParams {
            val token = extract(rawResponse, OAUTH_TOKEN_REGEXP_PATTERN)
            val secret = extract(rawResponse, OAUTH_TOKEN_SECRET_REGEXP_PATTERN)
            val responseNode = ConfiguredObjectMapper.instance.createObjectNode().apply {
                put(OAuth1AccessParams.OAUTH_TOKEN_KEY, token)
                put(OAuth1AccessParams.OAUTH_TOKEN_SECRET_KEY, secret)
            }
            return OAuth1AccessParams(
                internalUserId = userId,
                dataSourceId = dataSourceId,
                params = responseNode,
                externalUserId = token
            )
        }

        private fun extract(response: String, p: Pattern): String? {
          val matcher = p.matcher(response)
          if (matcher.find() && matcher.groupCount() >= 1) {
            return matcher.group(1)
          } else {
            throw IllegalArgumentException(
              "Response body is incorrect. Can't extract token and secret from this: '"
                + response + "'", null
            )
          }
        }
    }

}
