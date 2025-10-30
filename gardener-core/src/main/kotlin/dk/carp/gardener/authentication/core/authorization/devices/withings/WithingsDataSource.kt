package dk.carp.gardener.authentication.core.authorization.devices.withings

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.IOAuth2AuthorizationOperator
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2DataSource
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2TokenRefreshParams
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionPreparationEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.util.collections.RestrictedMap
import dk.carp.gardener.authentication.core.common.util.uri.HttpMethod
import dk.carp.gardener.authentication.core.common.util.uri.Uri

/**
 * Implementation of Withings data source.
 */
class WithingsDataSource(
    eventBus: IEventBus,
    stateService: IAuthorizationStateService,
    accessParamsService: IAccessParamsService,
    fitbitClientSettings: OAuth2ClientSettings,
    oauth2AuthorizationOperator: IOAuth2AuthorizationOperator,
) : OAuth2DataSource(eventBus, stateService, accessParamsService, fitbitClientSettings, oauth2AuthorizationOperator) {
    companion object {
        const val DATA_SOURCE_ID = "withings"

        // Data collection Ping constants
        const val P_DATE = "date"
        const val P_START_DATE = "startdate"
        const val P_END_DATE = "enddate"

        // Data collection query params
        const val QP_ACTION = "action"
        const val QP_LAST_UPDATE = "lastupdate"
        const val QP_START_DATE = "startdate"
        const val QP_END_DATE = "enddate"
    }

    /**
     * Supported Withings scopes.
     */
    enum class Scopes(
        val key: String,
    ) {
        USER_ACTIVITY("user.activity"),
        USER_METRICS("user.metrics"),
        ;

        companion object {
            fun isValid(scope: String): Boolean = entries.firstOrNull { it.key == scope } != null
        }
    }

    /**
     * Returns the ID of the data source.
     */
    override fun getId(): String = DATA_SOURCE_ID

    /**
     * Returns an [AuthorizationRequestParams] object that already contains
     * vendor specific necessary parameters for the authorization flow.
     */
    override fun getEstablishedAuthorizationRequestParams(): AuthorizationRequestParams {
        val additionalParams: MutableMap<String, String> = mutableMapOf("action" to "requesttoken")
        return OAuth2AuthorizationRequestParams(
            additionalParamsForTokens = RestrictedMap(additionalParams),
        )
    }

    /**
     * Returns an [OAuth2TokenRefreshParams] object that already contains
     * vendor specific necessary parameters for the OAuth2 token refresh flow.
     */
    override fun getEstablishedTokenRefreshParams(): OAuth2TokenRefreshParams {
        val additionalParams: MutableMap<String, String> = mutableMapOf("action" to "requesttoken")
        return OAuth2TokenRefreshParams(
            RestrictedMap(additionalParams),
        )
    }

    /**
     * Constructs a string made out of the [scopes] according to the vendor's specification.
     * Withings reuqires the scopes to be separated by ',' characters.
     *
     * @throws IllegalArgumentException When any of the requested [scopes] is not valid or supported.
     */
    override fun constructScopeStringsForAuthorizationUrl(scopes: List<String>?): String {
        if (scopes != null) {
            scopes.forEach {
                require(Scopes.isValid(it)) {
                    IllegalArgumentException("The requested Withings scope $it is not valid!")
                }
            }
            return scopes.joinToString(",")
        }
        return FitbitDataSource.Scopes.entries
            .map { it.key }
            .toList()
            .joinToString(",")
    }

    /**
     * Creates a completed [Uri], which can be used to collect the [dataType].
     *
     * @param accessParams The current access parameters for the data source/user.
     * @param dataType A [DataCollectionType] noting what kind of data should be collected.
     * @param rawPing A Json representation of the notification received from the vendor.
     * @return The completed [Uri].
     */
    override fun assembleDataCollectionUri(
        accessParams: AccessParams,
        dataType: DataCollectionType,
        rawPing: JsonNode,
    ): Uri {
        dataType as WithingsDataCollectionType
        val uriString: String
        val queryParams: MutableMap<String, String> = mutableMapOf()
        when (dataType) {
            WithingsDataCollectionType.DAILY_ACTIVITY -> {
                uriString = "${clientSettings.dataUrl}${dataType.getEndpoint()}"
                queryParams[QP_ACTION] = dataType.action
                queryParams[QP_LAST_UPDATE] = rawPing.get(P_DATE).asInt().toString()
            }
            WithingsDataCollectionType.HEART_LIST -> {
                uriString = "${clientSettings.dataUrl}${dataType.getEndpoint()}"
                queryParams[QP_ACTION] = dataType.action
                queryParams[QP_START_DATE] = rawPing.get(P_START_DATE).textValue()
                queryParams[QP_END_DATE] = rawPing.get(P_END_DATE).textValue()
            }
            WithingsDataCollectionType.SLEEP -> {
                uriString = "${clientSettings.dataUrl}${dataType.getEndpoint()}"
                queryParams[QP_ACTION] = dataType.action
                queryParams[QP_START_DATE] = rawPing.get(P_START_DATE).textValue()
                queryParams[QP_END_DATE] = rawPing.get(P_END_DATE).textValue()
            }
        }
        return Uri(method = HttpMethod.GET, uri = uriString, queryParams = queryParams)
    }

    /**
     * Converts a raw notification from the third-party vendor to the
     * application specific [DataCollectionPreparationEvent].
     * Withings does not send a JSON notification, it sends key value pairs
     * separated by '&' chars.
     *
     * @param notification A raw payload from the HTTP request the vendor sent.
     *
     * @return A list of events extracted from the [notification].
     *
     * @throws IllegalArgumentException When:
     *  - The notification is not formatted according to the vendor's specification and the parameters cannot be extracted.
     *  - The requested [DataCollectionType] is not valid.
     */
    @Suppress("TooGenericExceptionCaught")
    override fun getDataCollectionPreparationEventFromPing(notification: String): List<DataCollectionPreparationEvent> {
        val node: ObjectNode = JsonNodeFactory.instance.objectNode()
        try {
            notification.split("&").forEach { value ->
                val keyAndValue = value.split("=")
                if (keyAndValue.size == 2) {
                    node.put(keyAndValue[0], keyAndValue[1])
                }
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException(
                "Notification extraction failed for Withings: ${ex.message}. \nSent notification: $notification",
                ex,
            )
        }

        val event: DataCollectionPreparationEvent
        try {
            val userId = node.get("userid").textValue()
            val dataType =
                WithingsDataCollectionType.from(node.get("appli").textValue())
                    ?: throw IllegalArgumentException("The requested Withings Data Type is not valid!")
            event =
                DataCollectionPreparationEvent(
                    dataSourceId = DATA_SOURCE_ID,
                    userId = userId,
                    dataType = dataType,
                    rawPing = node,
                )
        } catch (ex: Exception) {
            throw IllegalArgumentException(
                "Notification extraction failed for Withings: ${ex.message}. \nSent notification: $node",
                ex,
            )
        }

        return listOf(event)
    }
}
