package dk.carp.gardener.authentication.implementation.repository

import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth2AuthorizationState
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.postgresql.util.PGobject
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

class CoroutinePostgresAuthorizationStateRepositoryTest {
    private val dataSource: DataSource = mock()
    private val connection: Connection = mock()
    private val statement: PreparedStatement = mock()
    private val resultSet: ResultSet = mock()

    private val repository = CoroutinePostgresAuthorizationStateRepository(dataSource)

    init {
        whenever(dataSource.connection).thenReturn(connection)
    }

    @Test
    fun `upsert stores authorization state`() {
        runBlocking {
            val state = OAuth2AuthorizationState("user", "withings").apply { success = true }

            whenever(connection.prepareStatement(any())).thenReturn(statement)
            whenever(statement.executeUpdate()).thenReturn(1)

            repository.upsert(state)

            verify(statement).setObject(eq(1), eq(UUID.fromString(state.id)))
            verify(statement).setString(eq(2), eq("oauth2"))
            verify(statement).setString(eq(3), eq(state.userId))
            verify(statement).setString(eq(4), eq(state.dataSourceId))
            verify(statement).setBoolean(eq(5), eq(true))
            verify(statement).setString(eq(6), eq(state.applicationData))

            val payloadCaptor = argumentCaptor<Any>()
            verify(statement).setObject(eq(9), payloadCaptor.capture())
            val payload = payloadCaptor.firstValue as PGobject
            assertEquals("jsonb", payload.type)
            val decoded = ConfiguredObjectMapper.instance.readValue(payload.value, OAuth2AuthorizationState::class.java)
            assertEquals(state.id, decoded.id)

            verify(statement).executeUpdate()
            verify(statement).close()
            verify(connection).close()
        }
    }

    @Test
    fun `findById returns state when present`() {
        runBlocking {
            val state = OAuth2AuthorizationState("user", "withings")
            val json = ConfiguredObjectMapper.instance.writeValueAsString(state)

            whenever(connection.prepareStatement(any())).thenReturn(statement)
            whenever(statement.executeQuery()).thenReturn(resultSet)
            whenever(resultSet.next()).thenReturn(true, false)
            whenever(resultSet.getString("payload")).thenReturn(json)

            val result = repository.findById(state.id)

            assertNotNull(result)
            assertEquals(state.id, result!!.id)
            assertEquals(state.userId, result.userId)
            verify(statement).setObject(eq(1), eq(UUID.fromString(state.id)))
            verify(resultSet).close()
            verify(statement).close()
            verify(connection).close()
        }
    }

    @Test
    fun `findById returns null for invalid uuid`() {
        runBlocking {
            val result = repository.findById("not-a-uuid")

            assertNull(result)
            verify(connection, org.mockito.kotlin.never()).prepareStatement(any())
        }
    }
}
