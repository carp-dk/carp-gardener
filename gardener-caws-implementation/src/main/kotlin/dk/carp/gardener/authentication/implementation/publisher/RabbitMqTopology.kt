package dk.carp.gardener.authentication.implementation.publisher

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Makes sure the exchange and queue Gardener publishes to exist on the broker.
 *
 * The broker may be shared with other services, so whatever already exists is used as is and only missing
 * pieces are created: a durable direct [exchange], a durable [queue] bound to it by its own name, and a
 * dead-letter exchange and queue (`<queue>.dlx` / `<queue>.dlq`) for rejected messages.
 */
class RabbitMqTopology(
    private val exchange: String,
    private val queue: String,
) {
    private val deadLetterExchange = "$queue.dlx"
    private val deadLetterQueue = "$queue.dlq"

    fun ensure(connection: Connection) {
        connection.createChannel().use { channel ->
            ensure(connection, "exchange", exchange, { it.exchangeDeclarePassive(exchange) }) {
                channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, true)
            }
            ensure(connection, "exchange", deadLetterExchange, { it.exchangeDeclarePassive(deadLetterExchange) }) {
                channel.exchangeDeclare(deadLetterExchange, BuiltinExchangeType.FANOUT, true)
            }
            ensure(connection, "queue", deadLetterQueue, { it.queueDeclarePassive(deadLetterQueue) }) {
                channel.queueDeclare(deadLetterQueue, true, false, false, null)
                channel.queueBind(deadLetterQueue, deadLetterExchange, "")
            }
            ensure(connection, "queue", queue, { it.queueDeclarePassive(queue) }) {
                channel.queueDeclare(queue, true, false, false, mapOf("x-dead-letter-exchange" to deadLetterExchange))
            }
            // Binding is idempotent, so an existing queue that lost its binding is reconnected too.
            channel.queueBind(queue, exchange, queue)
        }
    }

    private fun ensure(
        connection: Connection,
        kind: String,
        name: String,
        check: (Channel) -> Unit,
        create: () -> Unit,
    ) {
        if (exists(connection, check)) {
            LOGGER.info("Using existing RabbitMQ {} '{}'.", kind, name)
        } else {
            create()
            LOGGER.info("Created RabbitMQ {} '{}'.", kind, name)
        }
    }

    // A failed passive declare closes its channel, so each check gets a throwaway one; the failure just means "missing".
    @Suppress("SwallowedException")
    private fun exists(
        connection: Connection,
        check: (Channel) -> Unit,
    ): Boolean =
        try {
            connection.createChannel().use(check)
            true
        } catch (ex: IOException) {
            false
        }

    private companion object {
        private val LOGGER = LoggerFactory.getLogger(RabbitMqTopology::class.java)
    }
}
