package dk.carp.gardener.authentication.core.collection.oauth1

import dk.carp.gardener.authentication.core.collection.DataCollectionService
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.collection.publisher.IDataPublisher
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionExecutionEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.events.eventbus.IntegrationEvent
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformerRegistry

/**
 * OAuth1 specific collection service that
 * handles the data collection from third-party APIs.
 */
class OAuth1DataCollectionService(
    eventBus: IEventBus,
    publisher: IDataPublisher,
    transformerRegistry: IDataTypeTransformerRegistry,
    private val operator: IOAuth1DataCollectionOperatorBuilder,
) : DataCollectionService(eventBus, publisher, transformerRegistry) {
    init {
        @Suppress("UNCHECKED_CAST")
        eventBus.subscribe(
            subscriber = this::class,
            eventType = DataCollectionExecutionEvent.OAuth1ExecutionEvent::class,
            handler =
                { event: DataCollectionExecutionEvent.OAuth1ExecutionEvent ->
                    executeDataCollectionRequest(event)
                } as (IntegrationEvent) -> Unit,
        )
    }

    /**
     * Returns an authorization protocol specific [IDataCollectionOperator].
     */
    override fun getOperator(event: DataCollectionExecutionEvent): IDataCollectionOperator {
        event as DataCollectionExecutionEvent.OAuth1ExecutionEvent
        return operator.createDataCollectionOperatorWithClientSettings(event.clientSettings)
    }
}
