package dk.carp.gardener.authentication.core.authorization.devices.garmin

import com.fasterxml.jackson.databind.JsonNode
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.IOAuth1AuthorizationOperator
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1ClientSettings
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1DataSource
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionPreparationEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import dk.carp.gardener.authentication.core.common.util.uri.HttpMethod
import dk.carp.gardener.authentication.core.common.util.uri.Uri

/**
 * Implementation of Garmin data source.
 */
class GarminDataSource(
    eventBus: IEventBus,
    stateService: IAuthorizationStateService,
    accessParamsService: IAccessParamsService,
    garminClientSettings: OAuth1ClientSettings,
    oauth1AuthorizationOperator: IOAuth1AuthorizationOperator,
) : OAuth1DataSource(eventBus, stateService, accessParamsService, garminClientSettings, oauth1AuthorizationOperator) {
    companion object {
        const val DATA_SOURCE_ID = "garmin"

        // Additional parameter keys
        const val AP_API_URI = "callbackURL"
        const val AP_USER_TOKEN = "userAccessToken"
    }

    /**
     * Returns the ID of the data source.
     */
    override fun getId(): String = DATA_SOURCE_ID

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
    ): Uri = Uri(HttpMethod.GET, rawPing.get(AP_API_URI).textValue())

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
    override fun getDataCollectionPreparationEventFromPing(notification: String): List<DataCollectionPreparationEvent> {
        val events: MutableList<DataCollectionPreparationEvent> = mutableListOf()

        try {
            val notificationNode = ConfiguredObjectMapper.instance.readTree(notification)
            notificationNode.elements().forEach { element ->
                element.forEach { node ->
                    val apiUri = node.get(AP_API_URI).textValue()
                    val userToken = node.get(AP_USER_TOKEN).textValue()
                    val dataType =
                        GarminDataCollectionType.from(apiUri.substringBefore("?"))
                            ?: throw IllegalArgumentException("The requested Garmin Data Type is not valid!")

                    events.add(
                        DataCollectionPreparationEvent(
                            dataSourceId = DATA_SOURCE_ID,
                            userId = userToken,
                            dataType = dataType,
                            rawPing = node,
                        ),
                    )
                }
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException("Notification extraction failed for Garmin: ${ex.message}. \nSent notification: $notification")
        }

        return events
    }
}
