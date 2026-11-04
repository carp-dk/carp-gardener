package dk.carp.gardener.authentication.implementation.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PostgresDatabaseInitializerTest {
    @Test
    fun `databaseName reads the database from the url`() {
        assertEquals("gardener", PostgresDatabaseInitializer.databaseName("jdbc:postgresql://localhost:5432/gardener"))
        assertEquals("gardener", PostgresDatabaseInitializer.databaseName("jdbc:postgresql://db/gardener?sslmode=require"))
    }

    @Test
    fun `databaseName rejects a url without database`() {
        assertThrows<IllegalArgumentException> { PostgresDatabaseInitializer.databaseName("jdbc:postgresql://localhost:5432/") }
        assertThrows<IllegalArgumentException> { PostgresDatabaseInitializer.databaseName("jdbc:mysql://localhost/gardener") }
    }

    @Test
    fun `withDatabase swaps only the database and keeps parameters`() {
        assertEquals(
            "jdbc:postgresql://db:5432/postgres?sslmode=require&user=x",
            PostgresDatabaseInitializer.withDatabase("jdbc:postgresql://db:5432/gardener?sslmode=require&user=x", "postgres"),
        )
    }
}
