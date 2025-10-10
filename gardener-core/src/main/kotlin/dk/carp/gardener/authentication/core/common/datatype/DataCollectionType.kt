package dk.carp.gardener.authentication.core.common.datatype

import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformer

/**
 * Represents a third-part data that can be collected
 * from the Web APIs.
 */
interface DataCollectionType {

    /**
     * The id of the [DataCollectionType].
     * It is associated with the vendor's data name.
     */
    fun getIdentifier(): String

    /**
     * Uniquely identifies the data.
     * Chosen by the developer.
     */
    fun getNamespace(): String

    /**
     * The Web API endpoint where the data can be collected.
     */
    fun getEndpoint(): String

    /**
     * A Human-readable description of the data.
     */
    fun getCustomName(): String

    /**
     * Transform the [data] into the configured format
     * using the passed [transformer].
     *
     * @return A transformed third-party data.
     */
    fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any>
}