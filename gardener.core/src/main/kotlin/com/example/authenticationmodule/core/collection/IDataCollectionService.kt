package com.example.authenticationmodule.core.collection

import com.example.authenticationmodule.core.common.events.datacollection.DataCollectionExecutionEvent

interface IDataCollectionService {

    /**
     * Executes a data collection event.
     */
    fun executeDataCollectionRequest(event: DataCollectionExecutionEvent)

}