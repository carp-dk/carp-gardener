package dk.carp.gardener.authentication

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.base.ImplementationTest
import dk.carp.gardener.authentication.base.TestProperties
import dk.carp.gardener.authentication.base.TestUtil.Companion.getResourceAsText
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import dk.carp.gardener.authentication.implementation.repository.MongoAccessParamsRepository
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Integration tests for [MongoAccessParamsRepository]
 */
class MongoAccessParamRepositoryTest : ImplementationTest() {
    @Test
    fun paramsCanBeInserted(testContext: VertxTestContext) {
        val userId = "upsertTest"
        val accessParams = getMockFitbitAccessParams(userId)
        val repository = mainVerticle.accessParamsRepository

        repository.upsert(accessParams)
        repository.upsert(getMockFitbitAccessParams("dummy1"))
        repository.upsert(getMockFitbitAccessParams("dummy2"))

        testContext.completeNow()
    }

    @Test
    fun paramsCanBeRetrieved(testContext: VertxTestContext) {
        val userId = "paramsCanBeRetrieved"
        val accessParams = getMockFitbitAccessParams(userId) as OAuth2AccessParams
        val repository = mainVerticle.accessParamsRepository

        repository.upsert(accessParams)
        repository.upsert(getMockFitbitAccessParams("dummy1"))
        repository.upsert(getMockFitbitAccessParams("dummy2"))
        Thread.sleep(300)

        val queriedParams = repository.getLatestByInternalOrExternalUserIdAndDataSourceId(userId, FitbitDataSource.DATA_SOURCE_ID)
        assertNotNull(queriedParams)
        queriedParams as OAuth2AccessParams
        assertEquals(accessParams.id, queriedParams.id)
        assertEquals(accessParams.internalUserId, queriedParams.internalUserId)
        assertEquals(accessParams.params, queriedParams.params)
        assertEquals(accessParams.dataSourceId, queriedParams.dataSourceId)
        assertEquals(accessParams.externalUserId, queriedParams.externalUserId)

        testContext.completeNow()
    }

    @Test
    fun nullIsReturnedWhenNotPresent(testContext: VertxTestContext) {
        val userId = "paramsCanBeRetrieved"
        val accessParams = getMockFitbitAccessParams(userId) as OAuth2AccessParams
        val repository = mainVerticle.accessParamsRepository

        repository.upsert(accessParams)
        Thread.sleep(300)

        val queriedParams = repository.getLatestByInternalOrExternalUserIdAndDataSourceId("dummy", FitbitDataSource.DATA_SOURCE_ID)
        assertNull(queriedParams)

        testContext.completeNow()
    }

    @Test
    fun paramsCanBeQueriedForExistence(testContext: VertxTestContext) {
        val userId = "paramsCanBeQueriedForExistence"
        val accessParams = getMockFitbitAccessParams(userId) as OAuth2AccessParams
        val repository = mainVerticle.accessParamsRepository

        repository.upsert(accessParams)
        repository.upsert(getMockFitbitAccessParams("dummy1"))
        repository.upsert(getMockFitbitAccessParams("dummy2"))
        Thread.sleep(300)

        val exists = repository.existsByInternalOrExternalUserIdAndDataSourceId(userId, FitbitDataSource.DATA_SOURCE_ID)
        assertTrue(exists)

        testContext.completeNow()
    }

    @Test
    fun notExistingParamsReturnFalseForExistence(testContext: VertxTestContext) {
        val userId = "paramsCanBeQueriedForExistence"
        val accessParams = getMockFitbitAccessParams(userId) as OAuth2AccessParams
        val repository = mainVerticle.accessParamsRepository

        repository.upsert(accessParams)
        Thread.sleep(300)

        val exists = repository.existsByInternalOrExternalUserIdAndDataSourceId("dummy", FitbitDataSource.DATA_SOURCE_ID)
        assertFalse(exists)

        testContext.completeNow()
    }

    private fun getMockFitbitAccessParams(internalUserId: String): AccessParams {
        val accessParams: JsonNode = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/fitbit/fitbit_access_params.json"))
        return OAuth2AccessParams(
            internalUserId = internalUserId,
            dataSourceId = FitbitDataSource.DATA_SOURCE_ID,
            params = accessParams,
            externalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID,
        )
    }
}
