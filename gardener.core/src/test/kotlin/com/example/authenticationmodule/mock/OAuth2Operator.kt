package com.example.authenticationmodule.mock

import com.example.authenticationmodule.base.TestProperties
import com.example.authenticationmodule.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import com.example.authenticationmodule.core.authorization.authorizationstate.IAuthorizationStateService
import com.example.authenticationmodule.core.authorization.datasource.oauth2.IOAuth2AuthorizationOperator
import com.example.authenticationmodule.core.authorization.datasource.oauth2.OAuth2TokenRefreshParams
import com.example.authenticationmodule.core.authorization.devices.dexcom.DexcomDataSource
import com.example.authenticationmodule.core.authorization.devices.fitbit.FitbitDataSource
import com.example.authenticationmodule.core.authorization.devices.withings.WithingsDataSource
import com.example.authenticationmodule.core.collection.IDataCollectionOperator
import com.example.authenticationmodule.core.common.accessparams.AccessParams
import com.example.authenticationmodule.core.common.accessparams.IAccessParamsService
import com.example.authenticationmodule.core.common.accessparams.OAuth2AccessParams
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.util.uri.Uri
import com.fasterxml.jackson.databind.JsonNode

/**
 * Mock [OAuth2Operator].
 */
class OAuth2Operator(
    private val stateService: IAuthorizationStateService,
    private val accessParamsService: IAccessParamsService,
    private val fitbitAccessParams: JsonNode,
    private val fitbitActivitiesData: JsonNode,
    private val withingsAccessParams: JsonNode,
    private val withingsActivitiesData: JsonNode,
    private val dexcomAccessParams: JsonNode,
    private val dexcomEgvsData: JsonNode
) : IOAuth2AuthorizationOperator, IDataCollectionOperator {

    override fun getCompleteAuthorizationUrlForState(
        stateId: String,
        requestedScopes: String,
        params: OAuth2AuthorizationRequestParams
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
        params: OAuth2AuthorizationRequestParams
    ): OAuth2AccessParams {
        val newParams =  when (dataSourceId) {
            FitbitDataSource.DATA_SOURCE_ID -> {
                OAuth2AccessParams(
                    internalUserId = userId,
                    dataSourceId = dataSourceId,
                    params = fitbitAccessParams,
                    externalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
                )
            }
            WithingsDataSource.DATA_SOURCE_ID -> {
                OAuth2AccessParams(
                    internalUserId = userId,
                    dataSourceId = dataSourceId,
                    params = withingsAccessParams,
                    externalUserId = TestProperties.WITHINGS_TEST_USER_EXTERNAL_ID
                )
            }
            DexcomDataSource.DATA_SOURCE_ID -> {
                OAuth2AccessParams(
                    internalUserId = userId,
                    dataSourceId = dataSourceId,
                    params = dexcomAccessParams,
                    externalUserId = TestProperties.DEXCOM_TEST_USER_EXTERNAL_ID
                )
            }
            else -> {
                OAuth2AccessParams(
                    internalUserId = userId,
                    dataSourceId = dataSourceId,
                    params = fitbitAccessParams,
                    externalUserId = TestProperties.FITBIT_TEST_USER_EXTERNAL_ID
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
        params: OAuth2TokenRefreshParams
    ): OAuth2AccessParams {
        return retrieveAccessParams(userId, dataSourceId, "", OAuth2AuthorizationRequestParams())
    }

    override fun executeRequest(
        uri: Uri,
        dataType: DataCollectionType,
        accessParams: AccessParams,
        callback: (String) -> Unit
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