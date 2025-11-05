package dk.carp.gardener.authentication.implementation.repository

import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth1AuthorizationState
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.postgresql.util.PGobject
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Coroutine-friendly repository implementation backed by PostgreSQL for [AuthorizationState] entities.
 */
class CoroutinePostgresAuthorizationStateRepository(
    private val dataSource: DataSource,
) {
    private val mapper = ConfiguredObjectMapper.instance

    suspend fun findById(id: String): AuthorizationState? =
        withContext(Dispatchers.IO) {
            val uuid = id.toUuidOrNull() ?: return@withContext null
            dataSource.connection.use { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT payload
                        FROM authorization_states
                        WHERE id = ?
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setObject(1, uuid)
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                val json = resultSet.getString("payload")
                                mapper.readValue(json, AuthorizationState::class.java)
                            } else {
                                null
                            }
                        }
                    }
            }
        }

    suspend fun upsert(state: AuthorizationState) {
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection
                    .prepareStatement(
                        """
                        INSERT INTO authorization_states (
                            id,
                            type,
                            user_id,
                            data_source_id,
                            success,
                            application_data,
                            request_token,
                            token_secret,
                            payload,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET
                            type = EXCLUDED.type,
                            user_id = EXCLUDED.user_id,
                            data_source_id = EXCLUDED.data_source_id,
                            success = EXCLUDED.success,
                            application_data = EXCLUDED.application_data,
                            request_token = EXCLUDED.request_token,
                            token_secret = EXCLUDED.token_secret,
                            payload = EXCLUDED.payload,
                            created_at = EXCLUDED.created_at
                        """.trimIndent(),
                    ).use { statement ->
                        val uuid = state.id.toUuid()
                        val oauth1State = state as? OAuth1AuthorizationState
                        statement.setObject(1, uuid)
                        statement.setString(2, state.typeDiscriminator())
                        statement.setString(3, state.userId)
                        statement.setString(4, state.dataSourceId)
                        statement.setBoolean(5, state.success)
                        statement.setString(6, state.applicationData)
                        statement.setString(7, oauth1State?.requestToken)
                        statement.setString(8, oauth1State?.tokenSecret)
                        statement.setObject(9, state.payloadAsJsonb())
                        statement.setTimestamp(10, state.createdAt.toTimestamp())
                        statement.executeUpdate()
                    }
            }
        }
    }

    private fun AuthorizationState.payloadAsJsonb(): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = mapper.writeValueAsString(this@payloadAsJsonb)
        }

    private fun AuthorizationState.typeDiscriminator(): String =
        when (this) {
            is OAuth1AuthorizationState -> "oauth1"
            else -> "oauth2"
        }

    private fun String.toUuid(): UUID = UUID.fromString(this)

    private fun String.toUuidOrNull(): UUID? =
        runCatching { UUID.fromString(this) }.getOrNull()

    private fun Instant.toTimestamp(): Timestamp = Timestamp.from(this)
}
