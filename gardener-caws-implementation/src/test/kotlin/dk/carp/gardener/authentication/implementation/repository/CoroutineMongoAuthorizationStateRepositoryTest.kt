package dk.carp.gardener.authentication.implementation.repository

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.ReplaceOptions
import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth2AuthorizationState
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CoroutineMongoAuthorizationStateRepositoryTest {
    private val client: MongoClient = mock()
    private val database: MongoDatabase = mock()
    private val collection: MongoCollection<Document> = mock()

    private val repository = CoroutineMongoAuthorizationStateRepository(client, "test-db")

    init {
        whenever(client.getDatabase("test-db")).thenReturn(database)
        whenever(database.getCollection("authorization_states")).thenReturn(collection)
    }

    @Test
    fun `upsert stores authorization state`() = runBlocking {
        val state = OAuth2AuthorizationState("user", "withings")

        repository.upsert(state)

        val documentCaptor = argumentCaptor<Document>()
        verify(collection).replaceOne(any(), documentCaptor.capture(), any<ReplaceOptions>())
        assertEquals(state.id, documentCaptor.firstValue.getString("_id"))
    }

    @Test
    fun `findById returns latest state`() = runBlocking {
        val state = OAuth2AuthorizationState("user", "withings")
        val document = documentFrom(state)

        val iterable: FindIterable<Document> = mock()
        whenever(collection.find(any<org.bson.conversions.Bson>())).thenReturn(iterable)
        whenever(iterable.sort(any())).thenReturn(iterable)
        whenever(iterable.first()).thenReturn(document)

        val result = repository.findById(state.id)

        assertNotNull(result)
        assertEquals(state.userId, result!!.userId)
        assertEquals(state.dataSourceId, result.dataSourceId)
    }

    private fun documentFrom(state: OAuth2AuthorizationState): Document {
        val json = ConfiguredObjectMapper.instance.writeValueAsString(state)
        return Document.parse(json).apply { append("_id", state.id) }
    }
}
