package dk.carp.gardener.authentication.core.authorization.devices.fitbit

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.IOAuth2AuthorizationOperator
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2DataSource
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionPreparationEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import dk.carp.gardener.authentication.core.common.util.uri.HttpMethod
import dk.carp.gardener.authentication.core.common.util.uri.Uri

/**
 * Implementation of Fitbit data source.
 */
class FitbitDataSource(
    eventBus: IEventBus,
    stateService: IAuthorizationStateService,
    accessParamsService: IAccessParamsService,
    fitbitClientSettings: OAuth2ClientSettings,
    oauth2AuthorizationOperator: IOAuth2AuthorizationOperator,
) : OAuth2DataSource(eventBus, stateService, accessParamsService, fitbitClientSettings, oauth2AuthorizationOperator) {
    companion object {
        const val DATA_SOURCE_ID = "fitbit"

        // Additional parameter keys
        const val AP_DATE_KEY = "date"
        const val AP_USER_KEY = "ownerId"
        const val AP_DATA_TYPE_KEY = "collectionType"
    }

    /**
     * Supported Fitbit scopes.
     */
    enum class Scopes(
        val key: String,
    ) {
        ACTIVITY("activity"),
        HEART_RATE("heartrate"),
        WEIGHT("weight"),
        SLEEP("sleep"),
        NUTRITION("nutrition"),
        PROFILE("profile"),
        SETTINGS("settings"),
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
     * Constructs a string made out of the [scopes] according to the vendor's specification.
     * Fitbit requires the scopes to be separated by whitespaces.
     *
     * @throws IllegalArgumentException When any of the requested [scopes] is not valid or supported.
     */
    override fun constructScopeStringsForAuthorizationUrl(scopes: List<String>?): String {
        if (scopes != null) {
            scopes.forEach {
                require(Scopes.isValid(it)) { "The requested Fitbit scope $it is not valid!" }
            }
            return scopes.joinToString(" ")
        }
        return Scopes.entries
            .map { it.key }
            .toList()
            .joinToString(" ")
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
        dataType as FitbitDataCollectionType
        val uriString: String =
            when (dataType) {
                FitbitDataCollectionType.ACTIVITIES ->
                    "${clientSettings.dataUrl}/${dataType.version}/user/-/${dataType.getEndpoint()}/date/${rawPing.get(
                        AP_DATE_KEY,
                    ).textValue()}.json"
                FitbitDataCollectionType.HEART_RATE ->
                    "${clientSettings.dataUrl}/${dataType.version}/user/-/${dataType.getEndpoint()}/date/today/1d.json"
                FitbitDataCollectionType.BODY ->
                    "${clientSettings.dataUrl}/${dataType.version}/user/-/${dataType.getEndpoint()}/date/${rawPing.get(
                        AP_DATE_KEY,
                    ).textValue()}.json"
                FitbitDataCollectionType.SLEEP ->
                    "${clientSettings.dataUrl}/${dataType.version}/user/-/${dataType.getEndpoint()}/date/${rawPing.get(
                        AP_DATE_KEY,
                    ).textValue()}.json"
                FitbitDataCollectionType.FOOD ->
                    "${clientSettings.dataUrl}/${dataType.version}/user/-/${dataType.getEndpoint()}/date/${rawPing.get(
                        AP_DATE_KEY,
                    ).textValue()}.json"
            }
        return Uri(HttpMethod.GET, uriString)
    }

    /**
     * Converts a raw notification from the third-party vendor to the
     * application specific [DataCollectionPreparationEvent].
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
        val eventList: MutableList<DataCollectionPreparationEvent> = mutableListOf()
        try {
            val notificationNode = ConfiguredObjectMapper.instance.readTree(notification)
            notificationNode.forEach { ping ->
                val dataType =
                    FitbitDataCollectionType.from(ping.get(AP_DATA_TYPE_KEY).textValue())
                        ?: throw IllegalArgumentException("The requested Fitbit Data Type is not valid!")
                eventList.add(
                    DataCollectionPreparationEvent(
                        dataSourceId = DATA_SOURCE_ID,
                        userId = ping.get(AP_USER_KEY).textValue(),
                        dataType = dataType,
                        rawPing = ping,
                    ),
                )
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException(
                "Notification extraction failed for Fitbit: ${ex.message}. \nSent notification: $notification",
                ex,
            )
        }

        return eventList
    }
}
