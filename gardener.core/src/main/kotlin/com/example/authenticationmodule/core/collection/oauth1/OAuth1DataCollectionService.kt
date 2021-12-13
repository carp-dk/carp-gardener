package com.example.authenticationmodule.core.collection.oauth1

import com.example.authenticationmodule.core.collection.DataCollectionService
import com.example.authenticationmodule.core.collection.IDataCollectionOperator
import com.example.authenticationmodule.core.collection.publisher.IDataPublisher
import com.example.authenticationmodule.core.common.events.datacollection.DataCollectionExecutionEvent
import com.example.authenticationmodule.core.common.events.eventbus.IEventBus
import com.example.authenticationmodule.core.common.events.eventbus.IntegrationEvent
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformerRegistry

/**
 * OAuth1 specific collection service that
 * handles the data collection from third-party APIs.
 */
class OAuth1DataCollectionService(
    eventBus: IEventBus,
    publisher: IDataPublisher,
    transformerRegistry: IDataTypeTransformerRegistry,
    private val operator: IOAuth1DataCollectionOperatorBuilder
) : DataCollectionService(eventBus, publisher, transformerRegistry) {

    init {
        @Suppress("UNCHECKED_CAST")
        eventBus.subscribe(
            subscriber = this::class,
            eventType = DataCollectionExecutionEvent.OAuth1ExecutionEvent::class,
            handler = {
                    event: DataCollectionExecutionEvent.OAuth1ExecutionEvent -> executeDataCollectionRequest(event)
            } as (IntegrationEvent) -> Unit
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