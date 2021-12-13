package com.example.authenticationmodule.authorizationrequest

import com.example.authenticationmodule.core.authorization.authorizationrequest.AuthorizationRequestParams
import com.example.authenticationmodule.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import com.example.authenticationmodule.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import com.example.authenticationmodule.core.common.util.collections.RestrictedList
import com.example.authenticationmodule.core.common.util.collections.RestrictedMap
import com.example.authenticationmodule.core.common.util.serializer.ConfiguredObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests the serialization of [AuthorizationRequestParams] objects.
 */
class AuthorizationRequestSerializationTest {

    @Test
    fun oauth2StatesCanBeSerializedToJson() {
        val oauth2Params = OAuth2AuthorizationRequestParams(
            additionalParamsForGrants = RestrictedMap(mutableMapOf("hello" to "hello")),
            additionalParamsForTokens = RestrictedMap(mutableMapOf("hello" to "hello")),
            scopes = RestrictedList(mutableListOf("hello"))
        )

        val serialized = ConfiguredObjectMapper.instance.writeValueAsString(oauth2Params)
        val deserializedState = ConfiguredObjectMapper.instance.readValue(serialized, OAuth2AuthorizationRequestParams::class.java)

        assertNotNull(deserializedState)
        assertEquals(oauth2Params.scopes.getList(), deserializedState.scopes.getList())
    }

    @Test
    fun oauth1StatesCanBeSerializedToJson() {
        val oauth1Params = OAuth1AuthorizationRequestParams(
            additionalParamsForGrants = RestrictedMap(mutableMapOf("hello" to "hello")),
            additionalParamsForTokens = RestrictedMap(mutableMapOf("hello" to "hello")),
            additionalParamsForUnsignedToken = RestrictedMap(mutableMapOf("hello" to "hello"))
        )

        val serialized = ConfiguredObjectMapper.instance.writeValueAsString(oauth1Params)
        val deserializedState = ConfiguredObjectMapper.instance.readValue(serialized, OAuth1AuthorizationRequestParams::class.java)

        assertNotNull(deserializedState)
        assertEquals(oauth1Params.additionalParamsForUnsignedToken.getMap(), deserializedState.additionalParamsForUnsignedToken.getMap())
    }


}