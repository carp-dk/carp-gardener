package com.example.authenticationmodule.core.infrastructure.publisher

import com.example.authenticationmodule.core.collection.data.TransformedData
import com.example.authenticationmodule.core.collection.publisher.IDataPublisher
import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper

/**
 * A simple [IDataPublisher] implementation for testing purposes which serializes the data
 * and writes it to the console.
 *
 * The class is open due to mocking purposes during testing.
 */
open class InMemoryDataPublisher : IDataPublisher {

    override fun publishCollectedData(data: TransformedData) {
        val serializedPayload = ConfiguredObjectMapper.instance.writeValueAsString(data.value)
        println("Data successfully published: $serializedPayload")
    }

}