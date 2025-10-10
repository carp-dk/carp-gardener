package dk.carp.gardener.authentication.implementation.repository

import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsRepository
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import org.slf4j.LoggerFactory

/**
 * Provides a MongoDB implementation for [IAccessParamsRepository].
 */
class MongoAccessParamsRepository(private val client: MongoClient) : IAccessParamsRepository {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MongoAccessParamsRepository::class.java)
    }

    private val accessParamsTableName = "access_params"

    /**
     * Stores a newly created [AccessParams] or
     * updates it, if it is already present (by its ID).
     *
     * @param params The [AccessParams] object to store.
     */
    override fun upsert(params: AccessParams) {
        val serializedParams = ConfiguredObjectMapper.instance.writeValueAsString(params)
        client.save(accessParamsTableName, JsonObject(serializedParams).put("_id", params.id)) { res ->
            if (res.succeeded()) {
                LOGGER.info("Access params saved for ${params.dataSourceId}/${params.internalUserId}.")
            } else {
                LOGGER.info("Failed saving access params for ${params.dataSourceId}/${params.internalUserId}: ${res.cause().message}")
            }
        }
    }

    /**
     * Returns the most recent [AccessParams] entry
     * for the user and data source.
     *
     * @param userId Either internal or external user id.
     * @param dataSourceId ID of the data source
     *
     * @return [AccessParams] or null if there is no [AccessParams] for the user/data source.
     */
    override fun getLatestByInternalOrExternalUserIdAndDataSourceId(
        userId: String,
        dataSourceId: String
    ): AccessParams? {
        val query = "    {\n" +
                "        \"\$and\": [\n" +
                "            {\n" +
                "                \"\$or\": [\n" +
                "                    {\"internalUserId\": \"$userId\"},\n" +
                "                    {\"externalUserId\": \"$userId\"}\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"dataSourceId\": \"$dataSourceId\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }"

        val future = client.find(accessParamsTableName, JsonObject(query))
        while (future.result() == null) {}
        if (future.failed()) {
            LOGGER.info("Repository operation failed: ${future.cause().message}")
            return null
        }
        val results = future.result()
        if (results.size == 0) {
            LOGGER.info("Access params not found for $dataSourceId/$userId.")
            return null
        }
        LOGGER.info("Access params found for $dataSourceId/$userId.")

        return results.map { it.remove("_id") }
            .map { ConfiguredObjectMapper.instance.readValue(results[0].encode(), AccessParams::class.java) }
            .maxByOrNull { it.createdAt }!!
    }

    /**
     * Checks whether an [AccessParams] entry is present.
     *
     * @param userId Either internal or external user id.
     * @param dataSourceId ID of the data source
     *
     * @return True if it is present, false otherwise.
     */
    override fun existsByInternalOrExternalUserIdAndDataSourceId(userId: String, dataSourceId: String): Boolean {
        val result: AccessParams? = getLatestByInternalOrExternalUserIdAndDataSourceId(userId, dataSourceId)
        return result != null
    }


}