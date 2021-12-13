package com.example.authenticationmodule.implementation.publisher

import com.example.authenticationmodule.core.collection.data.TransformedData
import com.example.authenticationmodule.core.collection.publisher.IDataPublisher
import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper
import com.example.authenticationmodule.implementation.repository.MongoAccessParamsRepository
import com.example.authenticationmodule.verticles.PropertiesConfig
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Provides a RabbitMQ implementation for [IDataPublisher].
 */
class RabbitMqDataPublisher(private val properties: PropertiesConfig) : IDataPublisher {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RabbitMqDataPublisher::class.java)
    }

    private val channel: Channel
    private val connection: Connection

    init {
        val factory = ConnectionFactory().apply {
            host = properties.getProperty("rabbitmq.host")
            port = properties.getProperty("rabbitmq.port").toInt()
            username = properties.getProperty("rabbitmq.username")
            password = properties.getProperty("rabbitmq.password")
            connectionTimeout = 60000
            requestedHeartbeat = 60
            handshakeTimeout = 60000
            networkRecoveryInterval = 5000
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
                serializedData.toByteArray()
            )
            LOGGER.info("New datapoint successfully published to ${properties.getProperty("rabbitmq.queue.name")}.")
        } catch(ex: IOException) {
            LOGGER.info("Exception thrown while trying to publish data into RabbitMQ stream: $ex")
        }
    }

}