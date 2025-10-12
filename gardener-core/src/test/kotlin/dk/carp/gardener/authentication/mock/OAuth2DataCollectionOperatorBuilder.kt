package dk.carp.gardener.authentication.mock

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.collection.oauth2.IOAuth2DataCollectionOperatorBuilder
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService

/**
 * Mock [IOAuth2DataCollectionOperatorBuilder].
 */
class OAuth2DataCollectionOperatorBuilder(
    private val stateService:
    dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService,
    private val accessParamsService: IAccessParamsService,
    private val fitbitAccessParams: JsonNode,
    private val fitbitActivitiesData: JsonNode,
    private val withingsAccessParams: JsonNode,
    private val withingsActivitiesData: JsonNode,
    private val dexcomAccessParams: JsonNode,
    private val dexcomEgvsData: JsonNode,
) : IOAuth2DataCollectionOperatorBuilder {
    override fun createDataCollectionOperatorWithClientSettings(
        clientSettings: dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings,
    ): IDataCollectionOperator =
        OAuth2Operator(
            stateService = stateService,
            accessParamsService = accessParamsService,
            fitbitAccessParams = fitbitAccessParams,
            fitbitActivitiesData = fitbitActivitiesData,
            withingsAccessParams = withingsAccessParams,
            withingsActivitiesData = withingsActivitiesData,
            dexcomAccessParams = dexcomAccessParams,
            dexcomEgvsData = dexcomEgvsData,
        )
}
