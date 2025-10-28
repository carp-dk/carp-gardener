package dk.carp.gardener.authentication.implementation.oauth2

import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OAuth2DataCollectionOperatorBuilderTest {
    @Test
    fun `builder returns non-null operator for valid client settings`() {
        val clientSettings =
            OAuth2ClientSettings(
                clientId = "test-client",
                clientSecret = "secret",
                authorizationUri = "https://example.com/auth",
                accessUri = "https://example.com/token",
                dataUrl = "https://api.example.com",
                callbackUri = "http://localhost/callback",
            )

        val builder = OAuth2DataCollectionOperatorBuilder()
        val op: IDataCollectionOperator = builder.createDataCollectionOperatorWithClientSettings(clientSettings)
        assertNotNull(op)
    }
}
