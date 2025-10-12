package dk.carp.gardener.authentication.core.common.transformer

class DataTypeTransformerRegistryHost : IDataTypeTransformerRegistry {
    private val transformers: MutableMap<String, IDataTypeTransformer> = mutableMapOf()

    /**
     * Registers a [transformer] to be used for the given data source
     * indicated by the [dataSourceId].
     */
    override fun registerTransformer(
        dataSourceId: String,
        transformer: IDataTypeTransformer,
    ) {
        transformers[dataSourceId] = transformer
    }

    /**
     * Returns a registered transformer for a data source
     * indicated by the [dataSourceId].
     *
     * @throws IllegalArgumentException When no [IDataTypeTransformer] is found for the [dataSourceId].
     */
    override fun getTransformerForDataSource(dataSourceId: String): IDataTypeTransformer {
        require(transformers.containsKey(dataSourceId)) {
            "No transformer is registered for datasource with id $dataSourceId"
        }
        return transformers[dataSourceId]!!
    }
}
