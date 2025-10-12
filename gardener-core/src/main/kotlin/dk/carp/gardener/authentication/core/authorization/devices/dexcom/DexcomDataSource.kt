package dk.carp.gardener.authentication.core.authorization.devices.dexcom

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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Implementation of Dexcom data source.
 */
class DexcomDataSource(
    eventBus: IEventBus,
    stateService: IAuthorizationStateService,
    accessParamsService: IAccessParamsService,
    fitbitClientSettings: OAuth2ClientSettings,
    oauth2AuthorizationOperator: IOAuth2AuthorizationOperator,
) : OAuth2DataSource(eventBus, stateService, accessParamsService, fitbitClientSettings, oauth2AuthorizationOperator) {
    companion object {
        const val DATA_SOURCE_ID = "dexcom"

        // Dexcom date format pattern
        const val DATE_FORMAT = "yyyy-MM-dd"

        // Data collection Ping constants
        const val P_DATE = "date"
        const val P_START_DATE = "startdate"
        const val P_END_DATE = "enddate"

        // Data collection query params
        const val QP_START_DATE = "startDate"
        const val QP_END_DATE = "endDate"
    }

    /**
     * Returns the ID of the data source.
     */
    override fun getId(): String = DATA_SOURCE_ID

    /**
     * Constructs a string made out of the [scopes] according to the vendor's specification.
     * Dexcom only requires this scope to be present.
     */
    override fun constructScopeStringsForAuthorizationUrl(scopes: List<String>?): String = "offline_access"

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
        dataType as DexcomDataCollectionType
        val uriString = "${clientSettings.dataUrl}${dataType.getEndpoint()}"
        var queryParams: Map<String, String>? = null
        when (dataType) {
            DexcomDataCollectionType.CALIBRATIONS, DexcomDataCollectionType.EGVS, DexcomDataCollectionType.STATISTICS -> {
                val startDate: String
                val endDate: String
                if (rawPing.get(P_DATE) == null) {
                    startDate = rawPing.get(P_START_DATE).textValue()
                    endDate = rawPing.get(P_END_DATE).textValue()
                } else {
                    endDate = rawPing.get(P_DATE).textValue()
                    val localEndDate = LocalDate.parse(endDate, DateTimeFormatter.ofPattern(DATE_FORMAT))
                    val localStartDate = localEndDate.minusDays(1)
                    startDate = localStartDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT))
                }

                queryParams =
                    mapOf(
                        QP_START_DATE to startDate,
                        QP_END_DATE to endDate,
                    )
            }

            DexcomDataCollectionType.DATA_RANGE -> TODO()
        }

        return Uri(
            method = HttpMethod.GET,
            uri = uriString,
            queryParams = queryParams,
        )
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
    override fun getDataCollectionPreparationEventFromPing(notification: String): List<DataCollectionPreparationEvent> {
        val events: MutableList<DataCollectionPreparationEvent> = mutableListOf()

        try {
            val notificationNode = ConfiguredObjectMapper.instance.readTree(notification)
            val dataType =
                DexcomDataCollectionType.from(notificationNode.get("data_type").textValue())
                    ?: throw IllegalArgumentException("The requested Dexcom Data Type is not valid!")
            events.add(
                DataCollectionPreparationEvent(
                    dataSourceId = DATA_SOURCE_ID,
                    userId = notificationNode.get("user_id").textValue(),
                    dataType = dataType,
                    rawPing = notificationNode,
                ),
            )
        } catch (ex: Exception) {
            throw IllegalArgumentException("Notification extraction failed for Dexcom: ${ex.message}. \nSent notification: $notification")
        }

        return events
    }
}
