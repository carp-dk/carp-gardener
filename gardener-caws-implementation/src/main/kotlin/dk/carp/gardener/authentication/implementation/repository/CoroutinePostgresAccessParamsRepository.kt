package dk.carp.gardener.authentication.implementation.repository

import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.postgresql.util.PGobject
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Coroutine-friendly repository implementation backed by PostgreSQL for [AccessParams] entities.
 */
class CoroutinePostgresAccessParamsRepository(
    private val dataSource: DataSource,
) {
    private val mapper = ConfiguredObjectMapper.instance

    suspend fun upsert(params: AccessParams) {
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection
                    .prepareStatement(
                        """
                        INSERT INTO access_params (
                            id,
                            type,
                            internal_user_id,
                            external_user_id,
                            data_source_id,
                            payload,
                            application_data,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET
                            type = EXCLUDED.type,
                            internal_user_id = EXCLUDED.internal_user_id,
                            external_user_id = EXCLUDED.external_user_id,
                            data_source_id = EXCLUDED.data_source_id,
                            payload = EXCLUDED.payload,
                            application_data = EXCLUDED.application_data,
                            created_at = EXCLUDED.created_at,
                            updated_at = EXCLUDED.updated_at
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setObject(1, params.id.toUuid())
                        statement.setString(2, params.typeDiscriminator())
                        statement.setString(3, params.internalUserId)
                        statement.setString(4, params.externalUserId)
                        statement.setString(5, params.dataSourceId)
                        statement.setObject(6, params.payloadAsJsonb())
                        statement.setString(7, params.applicationData)
                        statement.setTimestamp(8, params.createdAt.toTimestamp())
                        statement.setTimestamp(9, params.updatedAt.toTimestamp())
                        statement.executeUpdate()
                    }
            }
        }
    }

    suspend fun getLatestByInternalOrExternalUserIdAndDataSourceId(
        userId: String,
        dataSourceId: String,
    ): AccessParams? =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT payload
                        FROM access_params
                        WHERE data_source_id = ?
                          AND (internal_user_id = ? OR external_user_id = ?)
                        ORDER BY created_at DESC
                        LIMIT 1
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setString(1, dataSourceId)
                        statement.setString(2, userId)
                        statement.setString(3, userId)
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                val json = resultSet.getString("payload")
                                mapper.readValue(json, AccessParams::class.java)
                            } else {
                                null
                            }
                        }
                    }
            }
        }

    suspend fun existsByInternalOrExternalUserIdAndDataSourceId(
        userId: String,
        dataSourceId: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT 1
                        FROM access_params
                        WHERE data_source_id = ?
                          AND (internal_user_id = ? OR external_user_id = ?)
                        LIMIT 1
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setString(1, dataSourceId)
                        statement.setString(2, userId)
                        statement.setString(3, userId)
                        statement.executeQuery().use { resultSet -> resultSet.next() }
                    }
            }
        }

    private fun AccessParams.payloadAsJsonb(): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = mapper.writeValueAsString(this@payloadAsJsonb)
        }

    private fun AccessParams.typeDiscriminator(): String =
        when (this) {
            is dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams -> "oauth1"
            is dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams -> "oauth2"
            else -> error("Unsupported access params type: ${this::class.simpleName}")
        }

    private fun String.toUuid(): UUID = UUID.fromString(this)

    private fun Instant.toTimestamp(): Timestamp = Timestamp.from(this)
}
