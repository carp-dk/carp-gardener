package dk.carp.gardener.authentication.implementation.oauth1

import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.IOAuth1AuthorizationOperator
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1RequestToken
import dk.carp.gardener.authentication.core.collection.IDataCollectionOperator
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.OAuth1AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.util.uri.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AdapterOAuth1Operator(
    private val coroutineOperator: CoroutineOAuth1Operator,
) : IOAuth1AuthorizationOperator,
    IDataCollectionOperator {
    override fun getCompleteAuthorizationUrlForUser(
        stateId: String,
        requestToken: OAuth1RequestToken,
        params: OAuth1AuthorizationRequestParams,
    ): String =
        runBlocking {
            withContext(Dispatchers.Default) { coroutineOperator.getCompleteAuthorizationUrlForUser(stateId, requestToken, params) }
        }

    override fun acquireUnsignedRequestToken(params: OAuth1AuthorizationRequestParams): OAuth1RequestToken =
        runBlocking {
            withContext(Dispatchers.IO) { coroutineOperator.acquireUnsignedRequestToken(params) }
        }

    override fun acquireAccessToken(
        userId: String,
        dataSourceId: String,
        requestToken: OAuth1RequestToken,
        verifier: String,
        params: OAuth1AuthorizationRequestParams,
    ): OAuth1AccessParams =
        runBlocking {
            withContext(Dispatchers.IO) {
                coroutineOperator.acquireAccessToken(userId, dataSourceId, requestToken, verifier, params)
            }
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
