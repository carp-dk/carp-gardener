package dk.carp.gardener.authentication.mock

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.base.TestProperties
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.IOAuth2AuthorizationOperator
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2TokenRefreshParams
import dk.carp.gardener.authentication.core.authorization.devices.dexcom.DexcomDataSource
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataSource
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.uri.Uri

/**
 * Mock [OAuth2Operator].
 */
@Suppress("LongParameterList")
class OAuth2Operator(
    private val stateService: IAuthorizationStateService,
    private val accessParamsService: IAccessParamsService,
    private val fitbitAccessParams: JsonNode,
    private val fitbitActivitiesData: JsonNode,
    private val withingsAccessParams: JsonNode,
    private val withingsActivitiesData: JsonNode,
    private val dexcomAccessParams: JsonNode,
    @Suppress("UnusedPrivateProperty") private val dexcomEgvsData: JsonNode,
) : IOAuth2AuthorizationOperator,
    IDataCollectionOperator {
    override fun getCompleteAuthorizationUrlForState(
        stateId: String,
        requestedScopes: String,
        params: OAuth2AuthorizationRequestParams,
    ): String {
        val state = stateService.getById(stateId)
        return when (state.dataSourceId) {
            FitbitDataSource.DATA_SOURCE_ID -> {
                TestProperties.FITBIT_AUTHORIZATION_URI
            }
            WithingsDataSource.DATA_SOURCE_ID -> {
                TestProperties.WITHINGS_AUTHORIZATION_URI
            }
            DexcomDataSource.DATA_SOURCE_ID -> {
                TestProperties.DEXCOM_AUTHORIZATION_URI
            }
            else -> "dummy_url"
        }
    }

    override fun retrieveAccessParams(
        userId: String,
        dataSourceId: String,
        authorizationCode: String,
        params: OAuth2AuthorizationRequestParams,
    ): OAuth2AccessParams {
        val newParams =
            when (dataSourceId) {
                FitbitDataSource.DATA_SOURCE_ID -> {
                    OAuth2AccessParams(
                        internalUserId = userId,
                        dataSourceId = dataSourceId,
                        params = fitbitAccessParams,
                        externalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID,
                    )
                }
                WithingsDataSource.DATA_SOURCE_ID -> {
                    OAuth2AccessParams(
                        internalUserId = userId,
                        dataSourceId = dataSourceId,
                        params = withingsAccessParams,
                        externalUserId = TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID,
                    )
                }
                DexcomDataSource.DATA_SOURCE_ID -> {
                    OAuth2AccessParams(
                        internalUserId = userId,
                        dataSourceId = dataSourceId,
                        params = dexcomAccessParams,
                        externalUserId = TestProperties.DEXCOM_TEST_USER_EXTERNAL_ID,
                    )
                }
                else -> {
                    OAuth2AccessParams(
                        internalUserId = userId,
                        dataSourceId = dataSourceId,
                        params = fitbitAccessParams,
                        externalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID,
                    )
                }
            }
        accessParamsService.addParams(newParams)
        return newParams
    }

    override fun refreshTokens(
        userId: String,
        dataSourceId: String,
        refreshToken: String,
        params: OAuth2TokenRefreshParams,
    ): OAuth2AccessParams =
        retrieveAccessParams(
            userId,
            dataSourceId,
            "",
            OAuth2AuthorizationRequestParams(),
        )

    override fun executeRequest(
        uri: Uri,
        dataType: DataCollectionType,
        accessParams: AccessParams,
        callback: (String) -> Unit,
    ) {
        when (accessParams.dataSourceId) {
            FitbitDataSource.DATA_SOURCE_ID -> {
                callback(fitbitActivitiesData.toString())
            }
            WithingsDataSource.DATA_SOURCE_ID -> {
                callback(withingsActivitiesData.toString())
            }
            else -> callback(fitbitActivitiesData.toString())
        }
    }
}
