package dk.carp.gardener.authentication

import dk.carp.gardener.authentication.base.ImplementationTest
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataCollectionType
import dk.carp.gardener.authentication.core.collection.data.TransformedData
import dk.carp.gardener.authentication.implementation.publisher.RabbitMqDataPublisher
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Integration tests for [RabbitMqDataPublisher]
 */
class RabbitPublisherTest : ImplementationTest() {
    @Test
    fun dataGetsPublished(testContext: VertxTestContext) {
        val transformedData =
            TransformedData("test", "test", FitbitDataCollectionType.ACTIVITIES, Instant.now(), mapOf("test" to "test"))

        mainVerticle.publisher.publishCollectedData(transformedData)

        testContext.completeNow()
    }
}
