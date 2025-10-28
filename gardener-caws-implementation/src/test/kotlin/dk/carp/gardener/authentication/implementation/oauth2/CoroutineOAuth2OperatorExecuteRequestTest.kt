package dk.carp.gardener.authentication.implementation.oauth2

import com.sun.net.httpserver.HttpServer
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CoroutineOAuth2OperatorExecuteRequestTest {
    companion object {
        private lateinit var server: HttpServer
        private lateinit var executor: ExecutorService
        private const val PORT = 18085

        @BeforeAll
        @JvmStatic
        fun setup() {
            server = HttpServer.create(InetSocketAddress(PORT), 0)
            server.createContext("/test") { exchange ->
                val response = "hello-from-server".toByteArray()
                exchange.sendResponseHeaders(200, response.size.toLong())
                exchange.responseBody.use { it.write(response) }
            }
            executor = Executors.newSingleThreadExecutor()
            server.executor = executor
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            server.stop(0)
            executor.shutdownNow()
        }
    }

    @Test
    fun `executeRequest retrieves body from HTTP endpoint`() =
        runBlocking {
            val clientSettings =
                OAuth2ClientSettings(
                    clientId = "test",
                    clientSecret = "secret",
                    authorizationUri = "https://example.com/auth",
                    accessUri = "https://example.com/token",
                    dataUrl = "http://localhost:$PORT",
                    callbackUri = "http://localhost/callback",
                )

            // service not used for executeRequest; create a minimal service instance
            val dummyService =
                com.github.scribejava.core.builder.ServiceBuilder("x").build(
                    object : com.github.scribejava.core.builder.api.DefaultApi20() {
                        override fun getAccessTokenEndpoint(): String = ""

                        override fun getAuthorizationBaseUrl(): String = ""
                    },
                )

            val op = CoroutineOAuth2Operator(clientSettings, dummyService)

            val rawJson = "{\"access_token\":\"token\", \"token_type\": \"bearer\"}"
            val accessParams = OAuth2AccessParams("user1", "withings", rawJson)

            val uri =
                Uri(dk.carp.gardener.authentication.core.common.util.uri.HttpMethod.GET, "http://localhost:$PORT/test")
            val result =
                op.executeRequest(
                    uri,
                    object : DataCollectionType {
                        override fun getIdentifier(): String = "id"

                        override fun getNamespace(): String = "ns"

                        override fun getEndpoint(): String = "/test"

                        override fun getCustomName(): String = "name"

                        override fun acceptTransformer(
                            transformer: dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer,
                            data: dk.carp.gardener.authentication.core.collection.data.ThirdPartyData,
                        ): List<Any> = emptyList()
                    },
                    accessParams,
                )

            assertEquals("hello-from-server", result)
        }
}
