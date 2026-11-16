package dk.carp.gardener.authentication.implementation.oauth2

import com.sun.net.httpserver.HttpServer
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer
import dk.carp.gardener.authentication.core.common.util.uri.HttpMethod
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CoroutineOAuth2OperatorExecuteRequestTest {
    companion object {
        private lateinit var server: HttpServer
        private lateinit var executor: ExecutorService
        private const val PORT = 18085
        private const val WITHINGS_OK = """{"status":0,"body":{"activities":[]}}"""
        private const val WITHINGS_ERROR = """{"status":503,"error":"Invalid Params"}"""

        @BeforeAll
        @JvmStatic
        fun setup() {
            server = HttpServer.create(InetSocketAddress(PORT), 0)
            respond("/test", "hello-from-server")
            respond("/withings-ok", WITHINGS_OK)
            respond("/withings-error", WITHINGS_ERROR)
            executor = Executors.newSingleThreadExecutor()
            server.executor = executor
            server.start()
        }

        private fun respond(
            path: String,
            body: String,
        ) {
            server.createContext(path) { exchange ->
                val response = body.toByteArray()
                exchange.sendResponseHeaders(200, response.size.toLong())
                exchange.responseBody.use { it.write(response) }
            }
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            server.stop(0)
            executor.shutdownNow()
        }
    }

    private val clientSettings =
        OAuth2ClientSettings(
            clientId = "test",
            clientSecret = "secret",
            authorizationUri = "https://example.com/auth",
            accessUri = "https://example.com/token",
            dataUrl = "http://localhost:$PORT",
            callbackUri = "http://localhost/callback",
        )

    // The service is not used by executeRequest; a minimal instance is enough.
    private val operator =
        CoroutineOAuth2Operator(
            clientSettings,
            com.github.scribejava.core.builder.ServiceBuilder("x").build(
                object : com.github.scribejava.core.builder.api.DefaultApi20() {
                    override fun getAccessTokenEndpoint(): String = ""

                    override fun getAuthorizationBaseUrl(): String = ""
                },
            ),
        )

    private val dataType =
        object : DataCollectionType {
            override fun getIdentifier(): String = "id"

            override fun getNamespace(): String = "ns"

            override fun getEndpoint(): String = "/test"

            override fun getCustomName(): String = "name"

            override fun acceptTransformer(
                transformer: IDataTypeTransformer,
                data: ThirdPartyData,
            ): List<Any> = emptyList()
        }

    private suspend fun fetch(
        dataSourceId: String,
        path: String,
    ): String {
        val accessParams =
            OAuth2AccessParams("user1", dataSourceId, """{"access_token":"token", "token_type": "bearer"}""")
        return operator.executeRequest(Uri(HttpMethod.GET, "http://localhost:$PORT$path"), dataType, accessParams)
    }

    @Test
    fun `executeRequest retrieves body from HTTP endpoint`() =
        runBlocking {
            assertEquals("hello-from-server", fetch("fitbit", "/test"))
        }

    @Test
    fun `executeRequest returns Withings body with status 0`() =
        runBlocking {
            assertEquals(WITHINGS_OK, fetch("withings", "/withings-ok"))
        }

    @Test
    fun `executeRequest rejects Withings error status sent with HTTP 200`() {
        val error = assertThrows<IllegalStateException> { runBlocking { fetch("withings", "/withings-error") } }
        assertEquals(true, error.message?.contains("status 503"))
    }
}
