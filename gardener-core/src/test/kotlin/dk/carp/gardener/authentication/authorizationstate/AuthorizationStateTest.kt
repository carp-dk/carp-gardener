package dk.carp.gardener.authentication.authorizationstate

import dk.carp.gardener.authentication.base.CoreTest
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth2AuthorizationState
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the functionality of [IAuthorizationStateService]
 */
class AuthorizationStateTest : CoreTest() {
    @Test
    fun stateCanBeSaved() {
        val userId = "stateCanBeSaved_user"
        val newState = getNewOAuth2StateForUser(userId)
        authorizationStateService.save(newState)
    }

    @Test
    fun stateCanBeRetrieved() {
        val userId = "stateCanBeRetrieved_user"
        val actualState = getNewOAuth2StateForUser(userId)
        val distractionState = getNewOAuth2StateForUser("distraction")
        authorizationStateService.save(actualState)
        authorizationStateService.save(distractionState)

        val retrievedState = authorizationStateService.getById(actualState.id)
        assertEquals(actualState.id, retrievedState.id)
        assertEquals(actualState, retrievedState)
    }

    @Test
    fun successfulFlagCanBeSet() {
        val userId = "successfulFlagCanBeSet_user"
        val actualState = getNewOAuth2StateForUser(userId)
        val distractionState = getNewOAuth2StateForUser("distraction")
        authorizationStateService.save(actualState)
        authorizationStateService.save(distractionState)

        assertFalse { actualState.success }
        authorizationStateService.setSuccessfulState(actualState)
        val retrievedState = authorizationStateService.getById(actualState.id)
        assertEquals(actualState.id, retrievedState.id)
        assertTrue { retrievedState.success }
    }

    private fun getNewOAuth2StateForUser(userId: String): OAuth2AuthorizationState {
        val dataSourceId = FitbitDataSource.DATA_SOURCE_ID
        return OAuth2AuthorizationState(
            userId = userId,
            dataSourceId = dataSourceId,
        )
    }
}
