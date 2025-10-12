package dk.carp.gardener.authentication.core.collection

import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.uri.Uri

interface IDataCollectionOperator {
    /**
     * Contacts the third-party APIs to collect data.
     *
     * @param uri The URI of the Web Server that needs to be contacted.
     * @param dataType The type of data that needs to be collected.
     * @param accessParams Access parameters for authentication.
     * @param callback A callback function that needs to be called upon successful data retrieval.
     *
     * @throws IllegalStateException When the data collection failed.
     */
    fun executeRequest(
        uri: Uri,
        dataType: DataCollectionType,
        accessParams: AccessParams,
        callback: (String) -> Unit,
    )
}
