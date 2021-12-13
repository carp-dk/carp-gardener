package com.example.authenticationmodule.mock

import com.example.authenticationmodule.core.authorization.authorizationstate.IAuthorizationStateService
import com.example.authenticationmodule.core.authorization.datasource.oauth2.OAuth2ClientSettings
import com.example.authenticationmodule.core.collection.IDataCollectionOperator
import com.example.authenticationmodule.core.collection.oauth2.IOAuth2DataCollectionOperatorBuilder
import com.example.authenticationmodule.core.common.accessparams.IAccessParamsService
import com.fasterxml.jackson.databind.JsonNode

/**
 * Mock [IOAuth2DataCollectionOperatorBuilder].
 */
class OAuth2DataCollectionOperatorBuilder(
    private val stateService: IAuthorizationStateService,
    private val accessParamsService: IAccessParamsService,
    private val fitbitAccessParams: JsonNode,
    private val fitbitActivitiesData: JsonNode,
    private val withingsAccessParams: JsonNode,
    private val withingsActivitiesData: JsonNode,
    private val dexcomAccessParams: JsonNode,
    private val dexcomEgvsData: JsonNode
) : IOAuth2DataCollectionOperatorBuilder {
    override fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth2ClientSettings): IDataCollectionOperator {
        return OAuth2Operator(
            stateService = stateService,
            accessParamsService = accessParamsService,
            fitbitAccessParams = fitbitAccessParams,
            fitbitActivitiesData = fitbitActivitiesData,
            withingsAccessParams = withingsAccessParams,
            withingsActivitiesData = withingsActivitiesData,
            dexcomAccessParams = dexcomAccessParams,
            dexcomEgvsData = dexcomEgvsData
        )
    }
}