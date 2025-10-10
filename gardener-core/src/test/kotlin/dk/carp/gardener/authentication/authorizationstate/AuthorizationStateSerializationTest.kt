package dk.carp.gardener.authentication.authorizationstate

import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth1AuthorizationState
import dk.carp.gardener.authentication.core.authorization.authorizationstate.OAuth2AuthorizationState
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests the serialization of [AuthorizationState] objects.
 */
class AuthorizationStateSerializationTest {

    @Test
    fun oauth2StatesCanBeSerializedToJson() {
        val userId = "userId"
        val dataSourceId = "dataSourceId"
        val state = OAuth2AuthorizationState(
            userId,
            dataSourceId
        )

        val serialized = ConfiguredObjectMapper.instance.writeValueAsString(state)
        val deserializedState = ConfiguredObjectMapper.instance.readValue(serialized, OAuth2AuthorizationState::class.java)

        assertNotNull(deserializedState)
        assertEquals(state.id, deserializedState.id)
    }

    @Test
    fun oauth1StatesCanBeSerializedToJson() {
        val userId = "userId"
        val dataSourceId = "dataSourceId"
        val state = OAuth1AuthorizationState(
            userId,
            dataSourceId,
            "",
            ""
        )

        val serialized = ConfiguredObjectMapper.instance.writeValueAsString(state)
        val deserializedState = ConfiguredObjectMapper.instance.readValue(serialized, OAuth1AuthorizationState::class.java)

        assertNotNull(deserializedState)
        assertEquals(state.id, deserializedState.id)
    }

}