package dk.carp.gardener.authentication.implementation.repository

import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties
import javax.sql.DataSource

/**
 * Prepares the PostgreSQL database used by Gardener on startup.
 *
 * The database may be shared with other services, so Gardener keeps its tables in its own [schema].
 * Whatever already exists (database, schema, tables) is used as is; anything missing is created.
 */
class PostgresDatabaseInitializer(
    private val jdbcUrl: String,
    private val username: String?,
    private val password: String?,
    private val schema: String,
) {
    /**
     * Connects to the database named in the JDBC URL and creates it when the server reports it missing.
     */
    fun ensureDatabaseExists() {
        val database = databaseName(jdbcUrl)
        try {
            connect(jdbcUrl).use { }
            LOGGER.info("Using existing PostgreSQL database '{}'.", database)
        } catch (ex: PSQLException) {
            if (ex.sqlState != INVALID_CATALOG_NAME) throw ex
            createDatabase(database)
        }
    }

    /**
     * Creates the schema and any missing tables and indexes; existing ones are left untouched.
     *
     * Runs in one transaction under an advisory lock so that instances starting together do not race.
     */
    fun ensureTables(dataSource: DataSource) {
        // Resolved here: inside the Connection receiver below, `schema` would mean Connection.getSchema().
        val quotedSchema = quoteIdentifier(schema)
        val tablesBefore =
            dataSource.connection.use { connection ->
                connection.inTransaction {
                    execute("SELECT pg_advisory_xact_lock($ADVISORY_LOCK_KEY)")
                    execute("CREATE SCHEMA IF NOT EXISTS $quotedSchema")
                    execute("SET LOCAL search_path TO $quotedSchema")
                    existingTables(this).also { execute(loadSchemaSql()) }
                }
            }
        TABLES.forEach { table ->
            if (table in tablesBefore) {
                LOGGER.info("Using existing table '{}.{}'.", schema, table)
            } else {
                LOGGER.info("Created table '{}.{}'.", schema, table)
            }
        }
    }

    private fun createDatabase(database: String) {
        LOGGER.info("PostgreSQL database '{}' does not exist; creating it.", database)
        connect(withDatabase(jdbcUrl, MAINTENANCE_DATABASE)).use { connection ->
            try {
                connection.execute("CREATE DATABASE ${quoteIdentifier(database)}")
                LOGGER.info("Created PostgreSQL database '{}'.", database)
            } catch (ex: PSQLException) {
                when (ex.sqlState) {
                    DUPLICATE_DATABASE -> LOGGER.info("PostgreSQL database '{}' was created concurrently; using it.", database)
                    INSUFFICIENT_PRIVILEGE -> throw IllegalStateException(
                        "PostgreSQL database '$database' does not exist and the configured user may not create it. " +
                            "Create the database or grant the user CREATEDB.",
                        ex,
                    )
                    else -> throw ex
                }
            }
        }
    }

    private fun existingTables(connection: Connection): Set<String> =
        connection
            .prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema = ?")
            .use { statement ->
                statement.setString(1, schema)
                statement.executeQuery().use { resultSet ->
                    buildSet { while (resultSet.next()) add(resultSet.getString("table_name")) }
                }
            }

    private fun connect(url: String): Connection {
        val connectionProperties =
            Properties().apply {
                username?.takeIf { it.isNotBlank() }?.let { setProperty("user", it) }
                password?.takeIf { it.isNotBlank() }?.let { setProperty("password", it) }
            }
        return DriverManager.getConnection(url, connectionProperties)
    }

    private fun Connection.execute(sql: String) {
        createStatement().use { it.execute(sql) }
    }

    private fun <T> Connection.inTransaction(block: Connection.() -> T): T {
        autoCommit = false
        try {
            return block().also { commit() }
        } catch (ex: SQLException) {
            rollback()
            throw ex
        } finally {
            autoCommit = true
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PostgresDatabaseInitializer::class.java)

        private const val SCHEMA_RESOURCE = "/db/postgres-schema.sql"
        private const val MAINTENANCE_DATABASE = "postgres"
        private val TABLES = listOf("authorization_states", "access_params")

        // Fixed key shared by all Gardener instances ("carpgard" in ASCII).
        private const val ADVISORY_LOCK_KEY = 0x6361_7270_6761_7264L

        private const val INVALID_CATALOG_NAME = "3D000"
        private const val DUPLICATE_DATABASE = "42P04"
        private const val INSUFFICIENT_PRIVILEGE = "42501"

        private val JDBC_URL_PATTERN = Regex("^(?<server>jdbc:postgresql://[^/]*/)(?<database>[^?]+)(?<parameters>.*)$")

        internal fun databaseName(jdbcUrl: String): String = parse(jdbcUrl).getValue("database")

        internal fun withDatabase(
            jdbcUrl: String,
            database: String,
        ): String {
            val parts = parse(jdbcUrl)
            return parts.getValue("server") + database + parts.getValue("parameters")
        }

        private fun parse(jdbcUrl: String): Map<String, String> {
            val match =
                requireNotNull(JDBC_URL_PATTERN.matchEntire(jdbcUrl)) { "JDBC URL '$jdbcUrl' does not name a database." }
            return listOf("server", "database", "parameters").associateWith { match.groups[it]?.value.orEmpty() }
        }

        private fun quoteIdentifier(identifier: String): String = "\"" + identifier.replace("\"", "\"\"") + "\""

        private fun loadSchemaSql(): String =
            PostgresDatabaseInitializer::class.java
                .getResource(SCHEMA_RESOURCE)
                ?.readText()
                ?: error("Schema resource $SCHEMA_RESOURCE is missing from the classpath.")
    }
}
