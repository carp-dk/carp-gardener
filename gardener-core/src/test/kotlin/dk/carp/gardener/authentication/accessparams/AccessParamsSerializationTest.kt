package dk.carp.gardener.authentication.accessparams

import dk.carp.gardener.authentication.base.TestProperties
import dk.carp.gardener.authentication.base.TestUtil.Companion.getResourceAsText
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.authorization.devices.garmin.GarminDataSource
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests the serialization of [AccessParams] objects.
 */
class AccessParamsSerializationTest {

    private val fitbitAccessParams = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/fitbit/fitbit_access_params.json"))
    private val garminAccessParams = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/garmin/garmin_access_params.json"))

    @Test
    fun oauth2SAccessParamsCanBeSerializedToJson() {
        val oAuth2AccessParams = OAuth2AccessParams(
            internalUserId = "userId",
            dataSourceId = FitbitDataSource.DATA_SOURCE_ID,
            params = fitbitAccessParams,
            externalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
        )

        val serialized = ConfiguredObjectMapper.instance.writeValueAsString(oAuth2AccessParams)
        val deserializedParams = ConfiguredObjectMapper.instance.readValue(serialized, OAuth2AccessParams::class.java)

        assertNotNull(deserializedParams)
        assertEquals(oAuth2AccessParams.id, deserializedParams.id)
    }

    @Test
    fun oauth1AccessParamsCanBeSerializedToJson() {
        val oAuth1AccessParams = OAuth1AccessParams(
            internalUserId = "userId",
            dataSourceId = GarminDataSource.DATA_SOURCE_ID,
            params = garminAccessParams,
            externalUserId = TestProperties.GARMIN_TEST_USER_EXTERNAL_ID
        )

        val serialized = ConfiguredObjectMapper.instance.writeValueAsString(oAuth1AccessParams)
        val deserializedParams = ConfiguredObjectMapper.instance.readValue(serialized, OAuth1AccessParams::class.java)

        assertNotNull(deserializedParams)
        assertEquals(oAuth1AccessParams.id, deserializedParams.id)
    }

}