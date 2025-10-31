package dk.carp.gardener.authentication.implementation.repository

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.ReplaceOptions
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
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

class CoroutineMongoAccessParamsRepositoryTest {
    private val client: MongoClient = mock()
    private val database: MongoDatabase = mock()
    private val collection: MongoCollection<Document> = mock()

    private val repository = CoroutineMongoAccessParamsRepository(client, "test-db")

    init {
        whenever(client.getDatabase("test-db")).thenReturn(database)
        whenever(database.getCollection("access_params")).thenReturn(collection)
    }

    @Test
    fun `upsert stores document in collection`() =
        runBlocking {
            val params = sampleAccessParams()

            repository.upsert(params)

            val documentCaptor = argumentCaptor<Document>()
            verify(collection).replaceOne(any(), documentCaptor.capture(), any<ReplaceOptions>())
            assertEquals(params.id, documentCaptor.firstValue.getString("_id"))
        }

    @Test
    fun `getLatestByInternal returns most recent access params`() =
        runBlocking {
            val params = sampleAccessParams()
            val document = documentFrom(params)

            val iterable: FindIterable<Document> = mock()
            whenever(collection.find(any<org.bson.conversions.Bson>())).thenReturn(iterable)
            whenever(iterable.sort(any())).thenReturn(iterable)
            whenever(iterable.first()).thenReturn(document)

            val result = repository.getLatestByInternalOrExternalUserIdAndDataSourceId("user", "withings")

            assertNotNull(result)
            assertEquals(params.internalUserId, result!!.internalUserId)
            assertEquals(params.dataSourceId, result.dataSourceId)
        }

    private fun sampleAccessParams(): OAuth2AccessParams =
        OAuth2AccessParams(
            internalUserId = "user",
            dataSourceId = "withings",
            params = ConfiguredObjectMapper.instance.readTree("""{"access_token":"token"}"""),
        )

    private fun documentFrom(params: OAuth2AccessParams): Document {
        val json = ConfiguredObjectMapper.instance.writeValueAsString(params)
        return Document.parse(json).apply { append("_id", params.id) }
    }
}
