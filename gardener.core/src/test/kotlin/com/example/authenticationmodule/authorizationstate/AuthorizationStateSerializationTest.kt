package com.example.authenticationmodule.authorizationstate

import com.example.authenticationmodule.core.authorization.authorizationstate.AuthorizationState
import com.example.authenticationmodule.core.authorization.authorizationstate.OAuth1AuthorizationState
import com.example.authenticationmodule.core.authorization.authorizationstate.OAuth2AuthorizationState
import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper
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
        val state = OAuth2AuthorizationState(userId, dataSourceId)

        val serialized = ConfiguredObjectMapper.instance.writeValueAsString(state)
        val deserializedState = ConfiguredObjectMapper.instance.readValue(serialized, OAuth2AuthorizationState::class.java)

        assertNotNull(deserializedState)
        assertEquals(state.id, deserializedState.id)
    }

    @Test
    fun oauth1StatesCanBeSerializedToJson() {
        val userId = "userId"
        val dataSourceId = "dataSourceId"
        val state = OAuth1AuthorizationState(userId, dataSourceId, "", "")

        val serialized = ConfiguredObjectMapper.instance.writeValueAsString(state)
        val deserializedState = ConfiguredObjectMapper.instance.readValue(serialized, OAuth1AuthorizationState::class.java)

        assertNotNull(deserializedState)
        assertEquals(state.id, deserializedState.id)
    }

}