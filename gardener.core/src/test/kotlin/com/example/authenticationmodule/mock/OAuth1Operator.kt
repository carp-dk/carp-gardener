package com.example.authenticationmodule.mock

import com.example.authenticationmodule.base.TestProperties
import com.example.authenticationmodule.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import com.example.authenticationmodule.core.authorization.authorizationstate.IAuthorizationStateService
import com.example.authenticationmodule.core.authorization.datasource.oauth1.IOAuth1AuthorizationOperator
import com.example.authenticationmodule.core.authorization.datasource.oauth1.OAuth1RequestToken
import com.example.authenticationmodule.core.collection.IDataCollectionOperator
import com.example.authenticationmodule.core.common.accessparams.AccessParams
import com.example.authenticationmodule.core.common.accessparams.IAccessParamsService
import com.example.authenticationmodule.core.common.accessparams.OAuth1AccessParams
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.util.uri.Uri
import com.fasterxml.jackson.databind.JsonNode

/**
 * Mock [OAuth1Operator].
 */
class OAuth1Operator(
    private val stateService: IAuthorizationStateService,
    private val accessParamsService: IAccessParamsService,
    private val garminAccessParams: JsonNode,
    private val garminStressData: JsonNode
) : IOAuth1AuthorizationOperator, IDataCollectionOperator {

    override fun getCompleteAuthorizationUrlForUser(
        stateId: String,
        requestToken: OAuth1RequestToken,
        params: OAuth1AuthorizationRequestParams
    ): String {
        return "url"
    }

    override fun acquireUnsignedRequestToken(params: OAuth1AuthorizationRequestParams): OAuth1RequestToken {
        return OAuth1RequestToken("requestToken", "tokenSecret")
    }

    override fun acquireAccessToken(
        userId: String,
        dataSourceId: String,
        requestToken: OAuth1RequestToken,
        verifier: String,
        params: OAuth1AuthorizationRequestParams
    ): OAuth1AccessParams {
        val newParams = OAuth1AccessParams(
            internalUserId = userId,
            dataSourceId = dataSourceId,
            params = garminAccessParams,
            externalUserId = TestProperties.GARMIN_TEST_USER_EXTERNAL_ID
        )
        accessParamsService.addParams(newParams)
        return newParams
    }

    override fun executeRequest(
        uri: Uri,
        dataType: DataCollectionType,
        accessParams: AccessParams,
        callback: (String) -> Unit
    ) {
        callback(garminStressData.toString())
    }

}