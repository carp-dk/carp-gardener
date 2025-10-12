package dk.carp.gardener.authentication

import dk.carp.gardener.authentication.base.ImplementationTest
import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth2AuthorizationState
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.implementation.repository.MongoAuthorizationStateRepository
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Integration tests for [MongoAuthorizationStateRepository]
 */
class MongoAuthorizationStateRepositoryTest : ImplementationTest() {
    @Test
    fun statesCanBeInserted(testContext: VertxTestContext) {
        val userId = "statesCanBeInserted"
        val state = getMockFitbitAuthorizationState(userId)
        val repo = mainVerticle.authorizationStateRepository

        repo.upsert(state)

        testContext.completeNow()
    }

    @Test
    fun statesCanBeRetrieved(testContext: VertxTestContext) {
        val userId = "statesCanBeRetrieved"
        val state = getMockFitbitAuthorizationState(userId)
        val repo = mainVerticle.authorizationStateRepository

        repo.upsert(state)
        repo.upsert(getMockFitbitAuthorizationState("dummy1"))
        repo.upsert(getMockFitbitAuthorizationState("dummy2"))
        Thread.sleep(300)

        val queried = repo.findById(state.id)
        assertNotNull(queried)
        assertEquals(state.id, queried!!.id)
        assertEquals(state.userId, queried.userId)
        assertEquals(state.dataSourceId, queried.dataSourceId)

        testContext.completeNow()
    }

    @Test
    fun nullIsReturnedWhenNotPresent(testContext: VertxTestContext) {
        val userId = "nullIsReturnedWhenNotPresent"
        val state = getMockFitbitAuthorizationState(userId)
        val repo = mainVerticle.authorizationStateRepository

        val queried = repo.findById(state.id)
        assertNull(queried)

        testContext.completeNow()
    }

    private fun getMockFitbitAuthorizationState(userId: String): AuthorizationState =
        OAuth2AuthorizationState(
            userId = userId,
            dataSourceId = FitbitDataSource.DATA_SOURCE_ID,
        )
}
