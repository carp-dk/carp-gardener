package dk.carp.gardener.authentication.implementation.repository

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document

/**
 * Coroutine-friendly wrapper around the synchronous MongoDB Java client for AccessParams operations.
 */
class CoroutineMongoAccessParamsRepository(
    private val client: MongoClient,
    private val databaseName: String,
) {
    private val mapper = ConfiguredObjectMapper.instance
    private val collection get() = client.getDatabase(databaseName).getCollection("access_params")

    suspend fun upsert(params: AccessParams) {
        val document = Document.parse(mapper.writeValueAsString(params)).apply { put("_id", params.id) }
        withContext(Dispatchers.IO) {
            collection.replaceOne(Filters.eq("_id", params.id), document, ReplaceOptions().upsert(true))
        }
    }

    suspend fun getLatestByInternalOrExternalUserIdAndDataSourceId(
        userId: String,
        dataSourceId: String,
    ): AccessParams? =
        withContext(Dispatchers.IO) {
            val filter =
                Filters.and(
                    Filters.or(Filters.eq("internalUserId", userId), Filters.eq("externalUserId", userId)),
                    Filters.eq("dataSourceId", dataSourceId),
                )

            val document =
                collection
                    .find(filter)
                    .sort(Sorts.descending("createdAt"))
                    .first()
            document?.let {
                it.remove("_id")
                mapper.readValue(it.toJson(), AccessParams::class.java)
            }
        }

    suspend fun existsByInternalOrExternalUserIdAndDataSourceId(
        userId: String,
        dataSourceId: String,
    ): Boolean = getLatestByInternalOrExternalUserIdAndDataSourceId(userId, dataSourceId) != null
}
