package com.example.authenticationmodule.mock

import com.example.authenticationmodule.core.authorization.authorizationstate.IAuthorizationStateService
import com.example.authenticationmodule.core.authorization.datasource.oauth1.OAuth1ClientSettings
import com.example.authenticationmodule.core.collection.IDataCollectionOperator
import com.example.authenticationmodule.core.collection.oauth1.IOAuth1DataCollectionOperatorBuilder
import com.example.authenticationmodule.core.common.accessparams.IAccessParamsService
import com.fasterxml.jackson.databind.JsonNode

/**
 * Mock [IOAuth1DataCollectionOperatorBuilder].
 */
class OAuth1DataCollectionOperatorBuilder(
    private val stateService: IAuthorizationStateService,
    private val accessParamsService: IAccessParamsService,
    private val garminAccessParams: JsonNode,
    private val garminStressData: JsonNode
) : IOAuth1DataCollectionOperatorBuilder {
    override fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth1ClientSettings): IDataCollectionOperator {
        return OAuth1Operator(stateService, accessParamsService, garminAccessParams = garminAccessParams, garminStressData = garminStressData)
    }
}