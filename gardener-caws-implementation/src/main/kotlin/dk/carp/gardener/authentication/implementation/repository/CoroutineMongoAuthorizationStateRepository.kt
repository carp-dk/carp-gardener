package dk.carp.gardener.authentication.implementation.repository

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document

/**
 * Coroutine-friendly wrapper around the synchronous MongoDB Java client for AuthorizationState operations.
 */
class CoroutineMongoAuthorizationStateRepository(
    private val client: MongoClient,
    private val databaseName: String,
) {
    private val mapper = ConfiguredObjectMapper.instance
    private val collection get() = client.getDatabase(databaseName).getCollection("authorization_states")

    suspend fun findById(id: String): AuthorizationState? =
        withContext(Dispatchers.IO) {
            val document =
                collection
                    .find(Filters.eq("id", id))
                    .sort(Sorts.descending("createdAt"))
                    .first()
            document?.let {
                it.remove("_id")
                mapper.readValue(it.toJson(), AuthorizationState::class.java)
            }
        }

    suspend fun upsert(state: AuthorizationState) {
        val document = Document.parse(mapper.writeValueAsString(state)).apply { put("_id", state.id) }
        withContext(Dispatchers.IO) {
            collection.replaceOne(Filters.eq("_id", state.id), document, ReplaceOptions().upsert(true))
        }
    }
}
