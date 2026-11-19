package dk.carp.gardener.authentication.ktor

import dk.carp.gardener.authentication.core.authorization.datasource.IDataSource
import dk.carp.gardener.authentication.core.authorization.datasourceregistry.DataSourceRegistryHost
import dk.carp.gardener.authentication.core.infrastructure.eventbus.SingleThreadedEventBus
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory

class RoutingTest {
    private val eventBus = SingleThreadedEventBus()
    private val registry = DataSourceRegistryHost(eventBus)

    @Test
    fun authorizeForInactiveDataSourceIsNotFound() =
        withRouting {
            assertEquals(HttpStatusCode.NotFound, client.get("/wearables/api/authorize/fitbit/user-1").status)
        }

    @Test
    fun callbackForInactiveDataSourceIsNotFound() =
        withRouting {
            assertEquals(HttpStatusCode.NotFound, client.get("/wearables/api/oauth/fitbit/callback?code=c&state=s").status)
        }

    @Test
    fun collectionForInactiveDataSourceIsNotFound() =
        withRouting {
            assertEquals(HttpStatusCode.NotFound, client.post("/wearables/api/collection/withings").status)
        }

    @Test
    fun collectionForActiveDataSourceIsAccepted() {
        registry.activateDataSource(mock<IDataSource> { on { getId() } doReturn "withings" })
        withRouting {
            assertEquals(HttpStatusCode.OK, client.post("/wearables/api/collection/withings").status)
        }
    }

    @Test
    fun activeDataSourceAnswersHeadOnCallbackAndNotificationUrls() {
        registry.activateDataSource(mock<IDataSource> { on { getId() } doReturn "withings" })
        withRouting {
            assertEquals(HttpStatusCode.OK, client.head("/wearables/api/oauth/withings/callback").status)
            assertEquals(HttpStatusCode.OK, client.head("/wearables/api/collection/withings").status)
        }
    }

    @Test
    fun headForInactiveDataSourceIsNotFound() =
        withRouting {
            assertEquals(HttpStatusCode.NotFound, client.head("/wearables/api/oauth/withings/callback").status)
            assertEquals(HttpStatusCode.NotFound, client.head("/wearables/api/collection/withings").status)
        }

    private fun withRouting(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureRouting(registry, eventBus, PropertiesConfig(), LoggerFactory.getLogger(RoutingTest::class.java))
            }
            block()
        }
}
