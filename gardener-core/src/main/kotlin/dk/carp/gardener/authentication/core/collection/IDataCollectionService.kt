package dk.carp.gardener.authentication.core.collection

import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionExecutionEvent

interface IDataCollectionService {

    /**
     * Executes a data collection event.
     */
    fun executeDataCollectionRequest(event: DataCollectionExecutionEvent)

}