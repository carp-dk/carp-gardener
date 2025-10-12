package dk.carp.gardener.authentication.accessparams

import dk.carp.gardener.authentication.base.CoreTest
import dk.carp.gardener.authentication.base.TestProperties
import dk.carp.gardener.authentication.base.TestUtil.Companion.getResourceAsText
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.authorization.devices.garmin.GarminDataSource
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the functionality of [IAccessParamsService]
 */
class AccessParamsTest : CoreTest() {
    private val fitbitAccessParams = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/fitbit/fitbit_access_params.json"))
    private val garminAccessParams = ConfiguredObjectMapper.instance.readTree(getResourceAsText("/garmin/garmin_access_params.json"))

    @Test
    fun oauth2ParamsGetSaved() {
        val userId = "oauth2ParamsGetSaved"
        accessParamService.addParams(getOAuth2Params(userId))
    }

    @Test
    fun oauth1ParamsGetSaved() {
        val userId = "oauth1ParamsGetSaved"
        accessParamService.addParams(getOAuth1Params(userId))
    }

    @Test
    fun paramsCanBeRetrieved() {
        val userId = "paramsCanBeRetrieved"
        val savedParams: OAuth1AccessParams = getOAuth1Params(userId)
        accessParamService.addParams(savedParams)

        val retrievedParams =
            accessParamService.getCurrentForInternalUserIdAndDataSource(
                userId,
                GarminDataSource.DATA_SOURCE_ID,
            ) as OAuth1AccessParams
        assertEquals(savedParams, retrievedParams)
    }

    @Test
    fun exceptionIsThrownWhenNoParamsArePresent() {
        assertFailsWith<IllegalArgumentException> {
            accessParamService.getCurrentForInternalUserIdAndDataSource("fail", "fail")
        }
    }

    @Test
    fun recognisesWhenUserIsRegistered() {
        val userId = "oauth1ParamsGetSaved"
        accessParamService.addParams(getOAuth1Params(userId))

        assertTrue { accessParamService.isUserAlreadyRegistered(userId, GarminDataSource.DATA_SOURCE_ID) }
    }

    @Test
    fun recognisesWhenUserIsNotRegistered() {
        assertFalse { accessParamService.isUserAlreadyRegistered("fail", GarminDataSource.DATA_SOURCE_ID) }
    }

    private fun getOAuth1Params(userId: String): OAuth1AccessParams =
        OAuth1AccessParams(
            internalUserId = userId,
            dataSourceId = GarminDataSource.DATA_SOURCE_ID,
            params = garminAccessParams,
            externalUserId = TestProperties.GARMIN_TEST_USER_EXTERNAL_ID,
        )

    private fun getOAuth2Params(userId: String): OAuth2AccessParams =
        OAuth2AccessParams(
            internalUserId = userId,
            dataSourceId = FitbitDataSource.DATA_SOURCE_ID,
            params = fitbitAccessParams,
            externalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID,
        )
}
