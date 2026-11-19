package dk.carp.gardener.authentication.ktor

import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth2AuthorizationState
import dk.carp.gardener.authentication.core.authorization.datasource.AuthorizationType
import dk.carp.gardener.authentication.core.authorization.datasource.IDataSource
import dk.carp.gardener.authentication.core.authorization.datasourceregistry.DataSourceRegistryHost
import dk.carp.gardener.authentication.core.infrastructure.eventbus.SingleThreadedEventBus
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory

class RoutingTest {
    private val eventBus = SingleThreadedEventBus()
    private val registry = DataSourceRegistryHost(eventBus)
    private val connectedState =
        OAuth2AuthorizationState(userId = "user-1", dataSourceId = "withings").apply { success = true }
    private val stateService =
        mock<IAuthorizationStateService> {
            on { getById(connectedState.id) } doReturn connectedState
            on { getById("unknown") } doThrow IllegalArgumentException("State is not found")
        }

    @Test
    fun authorizeForInactiveDataSourceIsNotFound() =
        withRouting {
            assertEquals(HttpStatusCode.NotFound, client.get("/gardener/api/authorize/fitbit/user-1").status)
        }

    @Test
    fun callbackForInactiveDataSourceIsNotFound() =
        withRouting {
            assertEquals(HttpStatusCode.NotFound, client.get("/gardener/api/oauth/fitbit/callback?code=c&state=s").status)
        }

    @Test
    fun collectionForInactiveDataSourceIsNotFound() =
        withRouting {
            assertEquals(HttpStatusCode.NotFound, client.post("/gardener/api/collection/withings").status)
        }

    @Test
    fun collectionForActiveDataSourceIsAccepted() {
        registry.activateDataSource(mock<IDataSource> { on { getId() } doReturn "withings" })
        withRouting {
            assertEquals(HttpStatusCode.OK, client.post("/gardener/api/collection/withings").status)
        }
    }

    @Test
    fun activeDataSourceAnswersHeadOnCallbackAndNotificationUrls() {
        registry.activateDataSource(mock<IDataSource> { on { getId() } doReturn "withings" })
        withRouting {
            assertEquals(HttpStatusCode.OK, client.head("/gardener/api/oauth/withings/callback").status)
            assertEquals(HttpStatusCode.OK, client.head("/gardener/api/collection/withings").status)
        }
    }

    @Test
    fun headForInactiveDataSourceIsNotFound() =
        withRouting {
            assertEquals(HttpStatusCode.NotFound, client.head("/gardener/api/oauth/withings/callback").status)
            assertEquals(HttpStatusCode.NotFound, client.head("/gardener/api/collection/withings").status)
        }

    @Test
    fun callbackShowsConnectedPageWhenTheTokensWereStored() {
        activateOAuth2Withings()
        withRouting {
            val response = client.get("/gardener/api/oauth/withings/callback?code=c&state=${connectedState.id}")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Withings connected"))
        }
    }

    @Test
    fun callbackShowsFailurePageWhenConnectingFailed() {
        activateOAuth2Withings()
        withRouting {
            val response = client.get("/gardener/api/oauth/withings/callback?code=c&state=unknown")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Withings could not be connected"))
        }
    }

    private fun activateOAuth2Withings() =
        registry.activateDataSource(
            mock<IDataSource> {
                on { getId() } doReturn "withings"
                on { getAuthorizationType() } doReturn AuthorizationType.OAUTH2
                on { getEstablishedAuthorizationRequestParams() } doReturn OAuth2AuthorizationRequestParams()
            },
        )

    private fun withRouting(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureRouting(registry, stateService, eventBus, PropertiesConfig(), LoggerFactory.getLogger(RoutingTest::class.java))
            }
            block()
        }
}
