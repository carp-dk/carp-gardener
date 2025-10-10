package dk.carp.gardener.authentication.core.collection.publisher

import dk.carp.gardener.authentication.core.collection.data.TransformedData

interface IDataPublisher {

    /**
     * Publishes a processed [data] that was collected
     * from third-party APIs.
     */
    fun publishCollectedData(data: TransformedData)

}