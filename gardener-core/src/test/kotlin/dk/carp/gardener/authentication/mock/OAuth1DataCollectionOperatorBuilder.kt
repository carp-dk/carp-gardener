package dk.carp.gardener.authentication.mock

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1ClientSettings
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.collection.oauth1.IOAuth1DataCollectionOperatorBuilder
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService

/**
 * Mock [IOAuth1DataCollectionOperatorBuilder].
 */
class OAuth1DataCollectionOperatorBuilder(
    private val stateService: IAuthorizationStateService,
    private val accessParamsService: IAccessParamsService,
    private val garminAccessParams: JsonNode,
    private val garminStressData: JsonNode,
) : IOAuth1DataCollectionOperatorBuilder {
    @Suppress("UnusedParameter", "ParameterListWrapping")
    override fun createDataCollectionOperatorWithClientSettings(clientSettings: OAuth1ClientSettings): IDataCollectionOperator =
        OAuth1Operator(
            stateService,
            accessParamsService,
            garminAccessParams = garminAccessParams,
            garminStressData = garminStressData,
        )
}
