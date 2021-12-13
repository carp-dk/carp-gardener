package com.example.authenticationmodule.core.collection.publisher

import com.example.authenticationmodule.core.collection.data.TransformedData

interface IDataPublisher {

    /**
     * Publishes a processed [data] that was collected
     * from third-party APIs.
     */
    fun publishCollectedData(data: TransformedData)

}