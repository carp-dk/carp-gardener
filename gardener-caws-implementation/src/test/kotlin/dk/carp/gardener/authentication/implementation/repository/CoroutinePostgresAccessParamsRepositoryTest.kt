package dk.carp.gardener.authentication.implementation.repository

import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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

class CoroutinePostgresAccessParamsRepositoryTest {
    private val dataSource: DataSource = mock()
    private val connection: Connection = mock()
    private val statement: PreparedStatement = mock()
    private val resultSet: ResultSet = mock()

    private val repository = CoroutinePostgresAccessParamsRepository(dataSource)

    init {
        whenever(dataSource.connection).thenReturn(connection)
    }

    @Test
    fun `upsert stores access params`() {
        runBlocking {
            val params = sampleAccessParams()

            whenever(connection.prepareStatement(any())).thenReturn(statement)
            whenever(statement.executeUpdate()).thenReturn(1)

            repository.upsert(params)

            verify(statement).setObject(eq(1), eq(UUID.fromString(params.id)))
            verify(statement).setString(eq(2), eq("oauth2"))
            verify(statement).setString(eq(3), eq(params.internalUserId))
            verify(statement).setString(eq(4), eq(params.externalUserId))
            verify(statement).setString(eq(5), eq(params.dataSourceId))

            val payloadCaptor = argumentCaptor<Any>()
            verify(statement).setObject(eq(6), payloadCaptor.capture())
            val payload = payloadCaptor.firstValue as PGobject
            assertEquals("jsonb", payload.type)

            val decoded =
                ConfiguredObjectMapper.instance.readValue(payload.value, OAuth2AccessParams::class.java)
            assertEquals(params.id, decoded.id)

            verify(statement).executeUpdate()
            verify(statement).close()
            verify(connection).close()
        }
    }

    @Test
    fun `getLatest returns params when available`() {
        runBlocking {
            val params = sampleAccessParams()
            val json = ConfiguredObjectMapper.instance.writeValueAsString(params)

            whenever(connection.prepareStatement(any())).thenReturn(statement)
            whenever(statement.executeQuery()).thenReturn(resultSet)
            whenever(resultSet.next()).thenReturn(true, false)
            whenever(resultSet.getString("payload")).thenReturn(json)

            val result = repository.getLatestByInternalOrExternalUserIdAndDataSourceId("user", "withings")

            assertNotNull(result)
            assertEquals(params.id, result!!.id)
            assertEquals(params.internalUserId, result.internalUserId)
            verify(statement).setString(eq(1), eq("withings"))
            verify(statement).setString(eq(2), eq("user"))
            verify(statement).setString(eq(3), eq("user"))
            verify(statement).close()
            verify(connection).close()
        }
    }

    @Test
    fun `exists returns false when no records`() {
        runBlocking {
            whenever(connection.prepareStatement(any())).thenReturn(statement)
            whenever(statement.executeQuery()).thenReturn(resultSet)
            whenever(resultSet.next()).thenReturn(false)

            val exists = repository.existsByInternalOrExternalUserIdAndDataSourceId("user", "withings")

            assertTrue(!exists)
            verify(statement).setString(eq(1), eq("withings"))
            verify(statement).setString(eq(2), eq("user"))
            verify(statement).setString(eq(3), eq("user"))
            verify(statement).close()
            verify(connection).close()
        }
    }

    @Test
    fun `exists returns true when record present`() {
        runBlocking {
            whenever(connection.prepareStatement(any())).thenReturn(statement)
            whenever(statement.executeQuery()).thenReturn(resultSet)
            whenever(resultSet.next()).thenReturn(true)

            val exists = repository.existsByInternalOrExternalUserIdAndDataSourceId("user", "withings")

            assertTrue(exists)
        }
    }

    private fun sampleAccessParams(): OAuth2AccessParams =
        OAuth2AccessParams(
            internalUserId = "user",
            dataSourceId = "withings",
            params = ConfiguredObjectMapper.instance.readTree("""{"access_token":"token"}"""),
        )
}
