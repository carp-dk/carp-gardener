package dk.carp.gardener.authentication.implementation.publisher

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import dk.carp.gardener.authentication.core.collection.data.TransformedData
import dk.carp.gardener.authentication.core.collection.publisher.IDataPublisher
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import dk.carp.gardener.authentication.ktor.PropertiesConfig
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Provides a RabbitMQ implementation for [IDataPublisher].
 */
class RabbitMqDataPublisher(
    private val properties: PropertiesConfig,
) : IDataPublisher {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(RabbitMqDataPublisher::class.java)
        private const val DEFAULT_CONNECTION_TIMEOUT_MILLIS = 60_000
        private const val DEFAULT_REQUESTED_HEARTBEAT_SECONDS = 60
        private const val DEFAULT_HANDSHAKE_TIMEOUT_MILLIS = 60_000
        private const val DEFAULT_NETWORK_RECOVERY_INTERVAL_MILLIS = 5_000
    }

    private val channel: Channel
    private val connection: Connection

    init {
        val factory =
            ConnectionFactory().apply {
                host = properties.getProperty("rabbitmq.host")
                port = properties.getProperty("rabbitmq.port").toInt()
                username = properties.getProperty("rabbitmq.username")
                password = properties.getProperty("rabbitmq.password")
                connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MILLIS
                requestedHeartbeat = DEFAULT_REQUESTED_HEARTBEAT_SECONDS
                handshakeTimeout = DEFAULT_HANDSHAKE_TIMEOUT_MILLIS
                networkRecoveryInterval = DEFAULT_NETWORK_RECOVERY_INTERVAL_MILLIS.toLong()
            }

        LOGGER.info("RabbitMQ connection establishment starting for ${factory.host}:${factory.port}")
        connection = factory.newConnection()
        channel = connection.createChannel()
    }

    /**
     * Publishes a processed [data] that was collected
     * from third-party APIs.
     */
    override fun publishCollectedData(data: TransformedData) {
        val serializedData = ConfiguredObjectMapper.instance.writeValueAsString(data.value)
        try {
            channel.basicPublish(
                properties.getProperty("rabbitmq.exchange.name"),
                properties.getProperty("rabbitmq.queue.name"),
                null,
                serializedData.toByteArray(),
            )
            LOGGER.info("New data stream payload successfully published to ${properties.getProperty("rabbitmq.queue.name")}.")
        } catch (ex: IOException) {
            LOGGER.info("Exception thrown while trying to publish data into RabbitMQ stream: $ex")
        }
    }
}
