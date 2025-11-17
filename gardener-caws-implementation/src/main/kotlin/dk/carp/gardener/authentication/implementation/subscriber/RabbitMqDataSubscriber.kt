package dk.carp.gardener.authentication.implementation.subscriber

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import dk.carp.gardener.authentication.ktor.PropertiesConfig
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.CountDownLatch

/**
 * Minimal RabbitMQ client that subscribes to the queue configured in [PropertiesConfig].
 *
 * The subscriber runs until it is interrupted (Ctrl+C) and logs each payload it receives.
 */
class RabbitMqDataSubscriber(
    private val properties: PropertiesConfig,
    private val messageListener: ((String) -> Unit)? = null,
) : Closeable {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(RabbitMqDataSubscriber::class.java)
        private const val DEFAULT_CONNECTION_TIMEOUT_MILLIS = 60_000
        private const val DEFAULT_REQUESTED_HEARTBEAT_SECONDS = 60
        private const val DEFAULT_HANDSHAKE_TIMEOUT_MILLIS = 60_000
        private const val DEFAULT_NETWORK_RECOVERY_INTERVAL_MILLIS = 5_000
    }

    private val queueName = properties.getProperty("rabbitmq.queue.name")
    private val host = properties.getProperty("rabbitmq.host")
    private val port = properties.getProperty("rabbitmq.port").toInt()

    private val shutdownLatch = CountDownLatch(1)
    private val connection: Connection
    private val channel: Channel

    init {
        val factory =
            ConnectionFactory().apply {
                host = this.host
                port = this.port
                username = properties.getProperty("rabbitmq.username")
                password = properties.getProperty("rabbitmq.password")
                connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MILLIS
                requestedHeartbeat = DEFAULT_REQUESTED_HEARTBEAT_SECONDS
                handshakeTimeout = DEFAULT_HANDSHAKE_TIMEOUT_MILLIS
                networkRecoveryInterval = DEFAULT_NETWORK_RECOVERY_INTERVAL_MILLIS.toLong()
                isAutomaticRecoveryEnabled = true
            }

        LOGGER.info("RabbitMQ connection establishment starting for {}:{}", host, port)
        LOGGER.info("RabbitMQ user {}", factory.username)
        connection = factory.newConnection("gardener-subscriber")
        channel = connection.createChannel()
        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }

    fun start() {
        LOGGER.info("Starting RabbitMQ subscriber for queue '{}' on {}:{}", queueName, host, port)
        try {
            val deliverCallback =
                DeliverCallback { _, delivery ->
                    val payload = delivery.body.decodeToString()
                    LOGGER.info(
                        "Received message (routingKey='{}', deliveryTag={}).",
                        delivery.envelope.routingKey,
                        delivery.envelope.deliveryTag,
                    )
                    LOGGER.debug("Payload: {}", payload)
                    channel.basicAck(delivery.envelope.deliveryTag, false)
                    messageListener?.invoke(payload)
                }
            val cancelCallback =
                CancelCallback { consumerTag ->
                    LOGGER.warn("Consumer '{}' has been cancelled by the broker.", consumerTag)
                }

            channel.basicQos(1)
            channel.queueDeclarePassive(queueName)
            channel.basicConsume(queueName, false, deliverCallback, cancelCallback)

            LOGGER.info("Listening for messages. Press Ctrl+C to stop.")
            shutdownLatch.await()
        } catch (ex: Exception) {
            LOGGER.error("Failed to start RabbitMQ subscriber: {}", ex.message, ex)
            close()
        }
    }

    override fun close() {
        LOGGER.info("Shutting down RabbitMQ subscriber.")
        try {
            if (channel.isOpen) {
                channel.close()
            }
            if (connection.isOpen) {
                connection.close()
            }
        } catch (ex: Exception) {
            LOGGER.warn("Error while closing RabbitMQ connection: {}", ex.message)
        } finally {
            shutdownLatch.countDown()
        }
    }
}

fun main() {
    val properties = PropertiesConfig()
    RabbitMqDataSubscriber(properties).start()
}
