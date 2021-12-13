package com.example.authenticationmodule

import com.example.authenticationmodule.base.ImplementationTest
import com.example.authenticationmodule.base.TestProperties
import com.example.authenticationmodule.base.TestUtil.Companion.getResourceAsText
import com.example.authenticationmodule.core.authorization.devices.fitbit.FitbitDataSource
import com.example.authenticationmodule.core.common.accessparams.AccessParams
import com.example.authenticationmodule.core.common.accessparams.OAuth2AccessParams
import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper
import com.example.authenticationmodule.implementation.repository.MongoAccessParamsRepository
import com.fasterxml.jackson.databind.JsonNode
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
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
            externalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
        )
    }


}