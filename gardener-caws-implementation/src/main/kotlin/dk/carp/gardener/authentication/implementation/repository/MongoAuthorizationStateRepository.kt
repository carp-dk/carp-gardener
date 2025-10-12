package dk.carp.gardener.authentication.implementation.repository

import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateRepository
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import org.slf4j.LoggerFactory

/**
 * Provides a MongoDB implementation for [IAuthorizationStateRepository].
 */
class MongoAuthorizationStateRepository(
    private val client: MongoClient,
) : IAuthorizationStateRepository {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(MongoAuthorizationStateRepository::class.java)
    }

    private val authorizationStateTableName = "authorization_states"

    /**
     * Retrieves an [AuthorizationState] object by its ID.
     *
     * @param id The ID of the [AuthorizationState]
     *
     * @return [AuthorizationState] or null if it's not found.
     */
    override fun findById(id: String): AuthorizationState? {
        val query =
            JsonObject().apply {
                put("id", id)
            }

        val future = client.find(authorizationStateTableName, query)
        while (future.result() == null) {}
        if (future.failed()) {
            LOGGER.info("Repository operation failed: ${future.cause().message}")
            return null
        }
        val results = future.result()
        if (results.size == 0) {
            LOGGER.info("Authorization state not found for id $id.")
            return null
        }
        LOGGER.info("Authorization state found for id $id.")

        return results
            .map { it.remove("_id") }
            .map { ConfiguredObjectMapper.instance.readValue(results[0].encode(), AuthorizationState::class.java) }
            .maxByOrNull { it.createdAt }!!
    }

    /**
     * Stores a newly created [AuthorizationState] or
     * updates it, if it is already present (by its ID).
     *
     * @param state The [AuthorizationState] object to store.
     */
    override fun upsert(state: AuthorizationState) {
        val serializedState = ConfiguredObjectMapper.instance.writeValueAsString(state)
        client.save(authorizationStateTableName, JsonObject(serializedState).put("_id", state.id)) { res ->
            if (res.succeeded()) {
                LOGGER.info("Authorization state saved for ${state.dataSourceId}/${state.userId}.")
            } else {
                LOGGER.info("Failed saving access params for ${state.dataSourceId}/${state.userId}: ${res.cause().message}")
            }
        }
    }
}
