package dk.carp.gardener.authentication.implementation.oauth1

import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1ClientSettings
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OAuth1DataCollectionOperatorBuilderTest {
    @Test
    fun `builder returns non-null operator for valid client settings`() {
        val clientSettings =
            OAuth1ClientSettings(
                consumerKey = "test-consumer",
                consumerSecret = "secret",
                signatureMethod = "HMAC-SHA1",
                version = "1.0",
                requestTokenUri = "https://example.com/request",
                accessTokenUri = "https://example.com/access",
                authorizationUri = "https://example.com/auth",
                dataUrl = "https://api.example.com",
                clientCallbackUri = "http://localhost/callback",
            )

        val builder = OAuth1DataCollectionOperatorBuilder()
        val op: IDataCollectionOperator = builder.createDataCollectionOperatorWithClientSettings(clientSettings)
        assertNotNull(op)
    }
}
