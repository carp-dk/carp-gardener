package dk.carp.gardener.authentication.ktor

import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.datasource.AuthorizationType
import dk.carp.gardener.authentication.core.authorization.datasourceregistry.IDataSourceRegistry
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.events.oauth1.OAuth1Event
import dk.carp.gardener.authentication.core.common.events.oauth2.OAuth2Event
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.slf4j.Logger

fun Application.configureRouting(
    dataSourceRegistry: IDataSourceRegistry,
    eventBus: IEventBus,
    properties: PropertiesConfig,
    logger: Logger,
) {
    routing {
        get("/wearables/api/authorize/{dataSourceId}/{userId}") {
            val dataSourceId = call.parameters["dataSourceId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val deploymentId = call.request.queryParameters["deploymentId"]
            logger.info("Authorization request initiated for {}/{}", dataSourceId, userId)

            val dataSource = dataSourceRegistry.getDataSourceById(dataSourceId)
            val request =
                if (dataSource.getAuthorizationType() == AuthorizationType.OAUTH2) {
                    val scopes = call.request.queryParameters["scopes"] ?: ""
                    val parsedScopes = if (scopes.isBlank()) emptyList() else scopes.split(",")
                    val params = dataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
                    params.applicationData = deploymentId
                    params.scopes.addAll(parsedScopes)
                    dataSource.initiateUserAuthorization(userId, dataSourceId, params)
                } else {
                    val params = dataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams
                    params.applicationData = deploymentId
                    dataSource.initiateUserAuthorization(userId, dataSourceId, params)
                }

            logger.info("Redirecting {}/{} to: {}", dataSourceId, userId, request.authorizationUrl)
            call.respondRedirect(request.authorizationUrl)
        }

        get("/wearables/api/oauth/{dataSourceId}/callback") {
            val dataSourceId = call.parameters["dataSourceId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val dataSource = dataSourceRegistry.getDataSourceById(dataSourceId)
            logger.info("Authorization Callback called for {}", dataSourceId)

            if (dataSource.getAuthorizationType() == AuthorizationType.OAUTH2) {
                val code = call.request.queryParameters["code"] ?: ""
                val stateId = call.request.queryParameters["state"] ?: ""
                eventBus.publish(
                    this::class,
                    OAuth2Event.AuthorizationCodeAcquired(
                        code = code,
                        stateId = stateId,
                        dataSourceId = dataSourceId,
                        params = dataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams,
                    ),
                )
            } else {
                val stateId = call.request.queryParameters["state"] ?: ""
                val token = call.request.queryParameters["oauth_token"] ?: ""
                val verifier = call.request.queryParameters["oauth_verifier"] ?: ""
                eventBus.publish(
                    this::class,
                    OAuth1Event.AuthorizedTokenAcquired(
                        stateId = stateId,
                        requestToken = token,
                        tokenVerifier = verifier,
                        dataSourceId = dataSourceId,
                        params = dataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams,
                    ),
                )
            }

            call.respond(HttpStatusCode.OK)
        }

        post("/wearables/api/collection/{dataSourceId}") {
            val dataSourceId = call.parameters["dataSourceId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val dataSource = dataSourceRegistry.getDataSourceById(dataSourceId)
            logger.info("Data collection callback called for {}", dataSourceId)

            val payload = runCatching { call.receiveText() }.getOrDefault("")

            if (dataSourceId == FitbitDataSource.DATA_SOURCE_ID) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.OK)
            }

            if (payload.isBlank()) {
                logger.info("Payload is null or empty. request aborted.")
                return@post
            }

            logger.info("Data Collection Payload for {}: {}", dataSourceId, payload)
            val events = dataSource.getDataCollectionPreparationEventFromPing(payload)
            events.forEach { event -> eventBus.publish(this::class, event) }
        }

        get("/wearables/api/collection/fitbit") {
            logger.info("Fitbit subscriber verification endpoint called.")
            val verificationCode = properties.getProperty("fitbit.client.subscription.code")
            val verifyParam = call.request.queryParameters["verify"]

            when (verifyParam) {
                null -> call.respond(HttpStatusCode.BadRequest)
                verificationCode -> {
                    logger.info("Correct fitbit verification code received.")
                    call.respond(HttpStatusCode.NoContent)
                }
                else -> {
                    logger.info("Incorrect fitbit verification code received.")
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
