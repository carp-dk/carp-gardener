package dk.carp.gardener.authentication.core.collection.oauth2

import dk.carp.gardener.authentication.core.collection.DataCollectionService
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.collection.publisher.IDataPublisher
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionExecutionEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.events.eventbus.IntegrationEvent
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformerRegistry

/**
 * OAuth2 specific collection service that
 * handles the data collection from third-party APIs.
 */
class OAuth2DataCollectionService(
    eventBus: IEventBus,
    publisher: IDataPublisher,
    transformerRegistry: IDataTypeTransformerRegistry,
    private val operator: IOAuth2DataCollectionOperatorBuilder,
) : DataCollectionService(eventBus, publisher, transformerRegistry) {
    init {
        @Suppress("UNCHECKED_CAST")
        eventBus.subscribe(
            subscriber = this::class,
            eventType = DataCollectionExecutionEvent.OAuth2ExecutionEvent::class,
            handler =
                { event: DataCollectionExecutionEvent.OAuth2ExecutionEvent ->
                    executeDataCollectionRequest(event)
                } as (IntegrationEvent) -> Unit,
        )
    }

    /**
     * Returns an authorization protocol specific [IDataCollectionOperator].
     */
    override fun getOperator(event: DataCollectionExecutionEvent): IDataCollectionOperator {
        event as DataCollectionExecutionEvent.OAuth2ExecutionEvent
        return operator.createDataCollectionOperatorWithClientSettings(event.clientSettings)
    }
}
