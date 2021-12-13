package com.example.authenticationmodule

import com.example.authenticationmodule.base.ImplementationTest
import com.example.authenticationmodule.core.authorization.devices.fitbit.FitbitDataCollectionType
import com.example.authenticationmodule.core.collection.data.TransformedData
import com.example.authenticationmodule.implementation.publisher.RabbitMqDataPublisher
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Integration tests for [RabbitMqDataPublisher]
 */
class RabbitPublisherTest : ImplementationTest() {

    @Test
    fun dataGetsPublished(testContext: VertxTestContext) {
        val transformedData = TransformedData("test", "test", FitbitDataCollectionType.ACTIVITIES, Instant.now(), mapOf("test" to "test"))

        mainVerticle.publisher.publishCollectedData(transformedData)

        testContext.completeNow()
    }

}