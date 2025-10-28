package dk.carp.gardener.authentication.implementation.oauth2

import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.IOAuth2AuthorizationOperator
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2TokenRefreshParams
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth2AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AdapterOAuth2Operator(
    private val coroutineOperator: CoroutineOAuth2Operator,
) : IOAuth2AuthorizationOperator,
    IDataCollectionOperator {
    override fun getCompleteAuthorizationUrlForState(
        stateId: String,
        requestedScopes: String,
        params: OAuth2AuthorizationRequestParams,
    ): String =
        runBlocking {
            withContext(Dispatchers.Default) {
                coroutineOperator.getCompleteAuthorizationUrlForState(stateId, requestedScopes, params)
            }
        }

    override fun retrieveAccessParams(
        userId: String,
        dataSourceId: String,
        authorizationCode: String,
        params: OAuth2AuthorizationRequestParams,
    ): OAuth2AccessParams =
        runBlocking {
            withContext(Dispatchers.IO) { coroutineOperator.retrieveAccessParams(userId, dataSourceId, authorizationCode, params) }
        }

    override fun refreshTokens(
        userId: String,
        dataSourceId: String,
        refreshToken: String,
        params: OAuth2TokenRefreshParams,
    ): OAuth2AccessParams =
        runBlocking {
            withContext(Dispatchers.IO) { coroutineOperator.refreshTokens(userId, dataSourceId, refreshToken, params) }
        }

    override fun executeRequest(
        uri: Uri,
        dataType: DataCollectionType,
        accessParams: AccessParams,
        callback: (String) -> Unit,
    ) {
        runBlocking {
            val res = withContext(Dispatchers.IO) { coroutineOperator.executeRequest(uri, dataType, accessParams) }
            callback(res)
        }
    }
}
