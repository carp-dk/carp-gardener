package dk.carp.gardener.authentication.implementation.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import java.util.UUID

/**
 * Runs [PostgresDatabaseInitializer] against a real server. Opt-in, because it creates and drops databases:
 * set GARDENER_IT_POSTGRES_URL (any database on the server, e.g. jdbc:postgresql://localhost:5432/postgres)
 * and, if needed, GARDENER_IT_POSTGRES_USERNAME / GARDENER_IT_POSTGRES_PASSWORD.
 */
class PostgresDatabaseInitializerIntegrationTest {
    private val serverUrl: String? = System.getenv("GARDENER_IT_POSTGRES_URL")
    private val dbUser: String? = System.getenv("GARDENER_IT_POSTGRES_USERNAME")
    private val dbPassword: String? = System.getenv("GARDENER_IT_POSTGRES_PASSWORD")

    private val database = "gardener_it_${System.nanoTime()}"
    private val dataSources = mutableListOf<HikariDataSource>()

    private val databaseUrl: String
        get() = PostgresDatabaseInitializer.withDatabase(serverUrl!!, database)

    @BeforeEach
    fun requireServer() {
        assumeTrue(serverUrl != null) { "Set GARDENER_IT_POSTGRES_URL to run the PostgreSQL integration tests." }
    }

    @AfterEach
    fun dropDatabase() {
        if (serverUrl == null) return
        dataSources.forEach { it.close() }
        connect(PostgresDatabaseInitializer.withDatabase(serverUrl, "postgres")).use { connection ->
            connection.createStatement().use { it.execute("DROP DATABASE IF EXISTS \"$database\" WITH (FORCE)") }
        }
    }

    @Test
    fun `creates missing database, schema and tables`() {
        initialize()

        connect(databaseUrl).use { connection ->
            assertEquals(setOf("access_params", "authorization_states"), tablesIn(connection, SCHEMA))
        }
    }

    @Test
    fun `keeps existing tables and their data`() {
        initialize()
        // Unqualified table name, as the repositories use: resolved through the pool's schema setting.
        dataSources.last().connection.use { connection ->
            connection.createStatement().use {
                it.execute(
                    """
                    INSERT INTO access_params (id, type, internal_user_id, data_source_id, payload)
                    VALUES ('${UUID.randomUUID()}', 'oauth2', 'user', 'fitbit', '{}')
                    """.trimIndent(),
                )
            }
        }

        initialize()

        connect(databaseUrl).use { connection ->
            connection.createStatement().use {
                it.executeQuery("SELECT count(*) FROM $SCHEMA.access_params").use { resultSet ->
                    resultSet.next()
                    assertEquals(1, resultSet.getInt(1))
                }
            }
        }
    }

    @Test
    fun `ignores same-named tables of other projects in the shared database`() {
        connect(PostgresDatabaseInitializer.withDatabase(serverUrl!!, "postgres")).use { connection ->
            connection.createStatement().use { it.execute("CREATE DATABASE \"$database\"") }
        }
        connect(databaseUrl).use { connection ->
            connection.createStatement().use { it.execute("CREATE TABLE public.access_params (unrelated int)") }
        }

        initialize()

        connect(databaseUrl).use { connection ->
            assertEquals(setOf("access_params", "authorization_states"), tablesIn(connection, SCHEMA))
            assertEquals(setOf("access_params"), tablesIn(connection, "public"))
        }
    }

    private fun initialize() {
        val initializer = PostgresDatabaseInitializer(databaseUrl, dbUser, dbPassword, SCHEMA)
        initializer.ensureDatabaseExists()
        val dataSource =
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = databaseUrl
                    schema = SCHEMA
                    dbUser?.let { username = it }
                    dbPassword?.let { password = it }
                    maximumPoolSize = 1
                },
            )
        dataSources += dataSource
        initializer.ensureTables(dataSource)
    }

    private fun tablesIn(
        connection: Connection,
        schema: String,
    ): Set<String> =
        connection.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema = ?").use { statement ->
            statement.setString(1, schema)
            statement.executeQuery().use { resultSet ->
                buildSet { while (resultSet.next()) add(resultSet.getString(1)) }
            }
        }

    private fun connect(url: String): Connection =
        DriverManager.getConnection(
            url,
            Properties().apply {
                dbUser?.let { setProperty("user", it) }
                dbPassword?.let { setProperty("password", it) }
            },
        )

    private companion object {
        const val SCHEMA = "gardener"
    }
}
