package com.example.authenticationmodule.core.collection

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.collection.publisher.IDataPublisher
import com.example.authenticationmodule.core.common.accessparams.AccessParams
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper
import com.example.authenticationmodule.core.common.events.datacollection.DataCollectionExecutionEvent
import com.example.authenticationmodule.core.common.events.datacollection.DataSuccessfullyCollectedEvent
import com.example.authenticationmodule.core.common.events.eventbus.IEventBus
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformerRegistry

/**
 * Super-class that
 * handles the data collection from third-party APIs.
 */
abstract class DataCollectionService(
    private val eventBus: IEventBus,
    private val publisher: IDataPublisher,
    private val transformerRegistry: IDataTypeTransformerRegistry
) : IDataCollectionService {

    /**
     * Executes a data collection event.
     */
    override fun executeDataCollectionRequest(event: DataCollectionExecutionEvent) {
        getOperator(event).executeRequest(
            uri = event.uri,
            dataType = event.dataIdentifier,
            accessParams = event.accessParams,
            callback = { apiResponse: String -> processFetchedData(event.accessParams, event.dataIdentifier, apiResponse) }
        )
    }

    /**
     * Returns an authorization protocol specific [IDataCollectionOperator].
     */
    protected abstract fun getOperator(event: DataCollectionExecutionEvent): IDataCollectionOperator

    /**
     * Transforms the raw [result] that was collected from the Web APIs
     * into the configured format and publishes the result.
     *
     * @param accessParams Access params that was used to contact the API.
     * @param dataType The type of the data that was collected.
     * @param result Raw response from the Web API.
     */
    private fun processFetchedData(accessParams: AccessParams, dataType: DataCollectionType, result: String) {
        val resultNode = ConfiguredObjectMapper.instance.readTree(result)
        val fetchedData = ThirdPartyData(
            userId = accessParams.internalUserId,
            dataSourceId = accessParams.dataSourceId,
            dataIdentifier = dataType,
            rawResponse = resultNode,
            applicationData = accessParams.applicationData
        )

        val transformer = transformerRegistry.getTransformerForDataSource(fetchedData.dataSourceId)
        val transformedData = fetchedData.transform(transformer)

        transformedData.forEach { data -> publisher.publishCollectedData(data) }
        eventBus.publish(this::class, DataSuccessfullyCollectedEvent(
            dataSourceId = fetchedData.dataSourceId,
            userId = fetchedData.userId,
            dataType = fetchedData.dataIdentifier,
            collectedAt = fetchedData.collectedAt
        ))
    }

}