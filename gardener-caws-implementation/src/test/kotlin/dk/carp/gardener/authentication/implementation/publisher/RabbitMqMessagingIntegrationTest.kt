package dk.carp.gardener.authentication.implementation.publisher

import com.fasterxml.jackson.databind.JsonNode
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.collection.data.TransformedData
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import dk.carp.gardener.authentication.implementation.subscriber.RabbitMqDataSubscriber
import dk.carp.gardener.authentication.ktor.PropertiesConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RabbitMqMessagingIntegrationTest {
    private lateinit var config: PropertiesConfig
    private lateinit var connectionFactory: ConnectionFactory

    @BeforeAll
    fun verifyRabbitMqAvailability() {
        config = PropertiesConfig()
        connectionFactory =
            ConnectionFactory().apply {
                host = config.getProperty("rabbitmq.host")
                port = config.getProperty("rabbitmq.port").toInt()
                username = config.getProperty("rabbitmq.username")
                password = config.getProperty("rabbitmq.password")
            }
        assumeBrokerAvailable()
        ensureTopologyExists()
    }

    @Test
    fun publisherSendsPayloadToRabbitMqQueue() {
        val tapQueue = createTapQueue()
        val publisher = RabbitMqDataPublisher(PropertiesConfig())
        val payload = mapOf<String, Any>("status" to "ok", "value" to 42)
        val transformedData =
            TransformedData(
                dataSourceId = "test-source",
                userId = "test-user",
                dataType = DummyDataCollectionType,
                collectedAt = Instant.parse("2024-01-01T00:00:00Z"),
                value = payload,
            )

        try {
            publisher.publishCollectedData(transformedData)

            val message = consumeSingleMessage(tapQueue)
            val mapper = ConfiguredObjectMapper.instance
            val expected: JsonNode = mapper.valueToTree(payload)
            val actual: JsonNode = mapper.readTree(message)
            assertEquals(expected, actual, "Publisher did not deliver the expected payload to the queue.")
        } finally {
            deleteTapQueue(tapQueue)
        }
    }

    @Test
    fun subscriberConsumesPublishedMessages() {
        purgeQueue()
        val messageConsumed = CountDownLatch(1)
        val subscriber =
            RabbitMqDataSubscriber(PropertiesConfig()) {
                messageConsumed.countDown()
            }
        val executor = Executors.newSingleThreadExecutor()
        val started = CountDownLatch(1)

        try {
            executor.submit {
                started.countDown()
                subscriber.start()
            }
            assertTrue(started.await(5, TimeUnit.SECONDS), "Subscriber thread did not start in time.")
            Thread.sleep(500) // Allow the consumer to register before publishing.

            publishRawMessage("""{"message":"from-test"}""")
            assertTrue(messageConsumed.await(5, TimeUnit.SECONDS), "Subscriber did not consume the published message.")
        } finally {
            subscriber.close()
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun purgeQueue() {
        withChannel { channel ->
            channel.queuePurge(config.getProperty("rabbitmq.queue.name"))
        }
    }

    private fun ensureTopologyExists() {
        withChannel { channel ->
            channel.exchangeDeclarePassive(config.getProperty("rabbitmq.exchange.name"))
            channel.queueDeclarePassive(config.getProperty("rabbitmq.queue.name"))
        }
    }

    private fun consumeSingleMessage(
        tapQueue: String,
        timeout: Duration = 5.seconds,
    ): String =
        withChannel { channel ->
            val deadline = System.nanoTime() + timeout.inWholeNanoseconds
            while (System.nanoTime() < deadline) {
                val result = channel.basicGet(tapQueue, true)
                if (result != null) {
                    return@withChannel String(result.body)
                }
                Thread.sleep(100)
            }
            throw IllegalArgumentException(
                "No message found on routing key ${config.getProperty("rabbitmq.queue.name")} within ${timeout.inWholeSeconds} seconds.",
            )
        }

    private fun createTapQueue(): String =
        withChannel { channel ->
            val queue = channel.queueDeclare("", false, false, true, emptyMap()).queue
            channel.queueBind(
                queue,
                config.getProperty("rabbitmq.exchange.name"),
                config.getProperty("rabbitmq.queue.name"),
            )
            queue
        }

    private fun deleteTapQueue(queue: String) {
        withChannel { channel ->
            channel.queueUnbind(
                queue,
                config.getProperty("rabbitmq.exchange.name"),
                config.getProperty("rabbitmq.queue.name"),
            )
            channel.queueDelete(queue)
        }
    }

    private fun publishRawMessage(payload: String) {
        withChannel { channel ->
            channel.basicPublish(
                config.getProperty("rabbitmq.exchange.name"),
                config.getProperty("rabbitmq.queue.name"),
                null,
                payload.toByteArray(),
            )
        }
    }

    private fun assumeBrokerAvailable() {
        val available =
            try {
                connectionFactory.newConnection().use { }
                true
            } catch (ex: Exception) {
                false
            }
        assumeTrue(available) {
            "RabbitMQ broker not reachable at ${config.getProperty(
                "rabbitmq.host",
            )}:${config.getProperty("rabbitmq.port")}. Start the local container before running this test."
        }
    }

    private fun <T> withChannel(block: (Channel) -> T): T =
        connectionFactory.newConnection().use { connection ->
            connection.createChannel().use { channel -> block(channel) }
        }

    private object DummyDataCollectionType : DataCollectionType {
        override fun getIdentifier(): String = "dummy"

        override fun getNamespace(): String = "testing"

        override fun getEndpoint(): String = "/dummy"

        override fun getCustomName(): String = "Dummy data"

        override fun acceptTransformer(
            transformer: IDataTypeTransformer,
            data: ThirdPartyData,
        ): List<Any> = emptyList()
    }
}
