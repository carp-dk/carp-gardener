package dk.carp.gardener.authentication.core.authorization.datasource

import dk.carp.gardener.authentication.core.authorization.authorizationrequest.AuthorizationRequestParams
import dk.carp.gardener.authentication.core.common.accessparams.AccessParams
import dk.carp.gardener.authentication.core.common.datatype.DataCollectionType
import dk.carp.gardener.authentication.core.common.events.datacollection.DataCollectionPreparationEvent

/**
 * Encapsulates common behaviour of data sources
 * regardless of their authorization process.
 */
interface IDataSource {

    /**
     * Returns the ID of the data source.
     */
    fun getId(): String

    /**
     * Returns the [AuthorizationType] of the data source,
     * corresponding to the authorization mechanism it uses.
     */
    fun getAuthorizationType(): AuthorizationType

    /**
     * Returns an [AuthorizationRequestParams] object that already contains
     * vendor specific necessary parameters for the authorization flow.
     */
    fun getEstablishedAuthorizationRequestParams(): AuthorizationRequestParams

    /**
     * Creates a new authorization state for the authorization process and returns
     * the completed authorization URL parametrized with the state id
     * where the user should be redirected to.
     *
     * @param userId The ID of the user that wants to authorize.
     * @param dataSourceId The ID of the data source where the authorization needs to be done.
     * @param params Additional parameters that need to be considered while making the authorization URL.
     *
     * @return A new [AuthorizationRequest] object containing the newly created state for the user and the URL.
     *
     * @throws IllegalArgumentException When the user is already authorized with the given data source.
     */
    fun initiateUserAuthorization(userId: String, dataSourceId: String, params: AuthorizationRequestParams): AuthorizationRequest

    /**
     * Return the current [AccessParams] for the user for the given data source.
     *
     * @param userId The ID of the user.
     *
     * @throws IllegalArgumentException When no access parameters were found for the user
     * for this data source.
     */
    fun getAccessParametersFor(userId: String): AccessParams

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
    fun getDataCollectionPreparationEventFromPing(notification: String): List<DataCollectionPreparationEvent>

}
