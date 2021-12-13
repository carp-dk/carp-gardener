package com.example.authenticationmodule.core.common.transformer

/**
 * Handles [IDataTypeTransformer]s.
 */
interface IDataTypeTransformerRegistry {

    /**
     * Registers a [transformer] to be used for the given data source
     * indicated by the [dataSourceId].
     */
    fun registerTransformer(dataSourceId: String, transformer: IDataTypeTransformer)

    /**
     * Returns a registered transformer for a data source
     * indicated by the [dataSourceId].
     *
     * @throws IllegalArgumentException When no [IDataTypeTransformer] is found for the [dataSourceId].
     */
    fun getTransformerForDataSource(dataSourceId: String): IDataTypeTransformer

}