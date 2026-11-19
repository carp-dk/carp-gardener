package dk.carp.gardener.authentication.ktor

import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.datasource.AuthorizationType
import dk.carp.gardener.authentication.core.authorization.datasourceregistry.IDataSourceRegistry
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.events.oauth1.OAuth1Event
import dk.carp.gardener.authentication.core.common.events.oauth2.OAuth2Event
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.slf4j.Logger

@Suppress("LongMethod", "CyclomaticComplexMethod")
fun Application.configureRouting(
    dataSourceRegistry: IDataSourceRegistry,
    stateService: IAuthorizationStateService,
    eventBus: IEventBus,
    properties: PropertiesConfig,
    logger: Logger,
) {
    routing {
        get("/gardener/api/authorize/{dataSourceId}/{userId}") {
            val dataSourceId = call.parameters["dataSourceId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val deploymentId = call.request.queryParameters["deploymentId"]
            val deviceRoleName = call.request.queryParameters["deviceRoleName"]
            val applicationData = buildApplicationData(deploymentId, deviceRoleName)
            logger.info("Authorization request initiated for {}/{}", dataSourceId, userId)

            val dataSource =
                dataSourceRegistry.findDataSourceById(dataSourceId)
                    ?: return@get call.respondUnknownDataSource(dataSourceId, logger)
            val request =
                if (dataSource.getAuthorizationType() == AuthorizationType.OAUTH2) {
                    val scopes = call.request.queryParameters["scopes"] ?: ""
                    val parsedScopes = if (scopes.isBlank()) emptyList() else scopes.split(",")
                    val params = dataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
                    params.applicationData = applicationData
                    params.scopes.addAll(parsedScopes)
                    dataSource.initiateUserAuthorization(userId, dataSourceId, params)
                } else {
                    val params = dataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams
                    params.applicationData = applicationData
                    dataSource.initiateUserAuthorization(userId, dataSourceId, params)
                }

            logger.info("Redirecting {}/{} to: {}", dataSourceId, userId, request.authorizationUrl)
            call.respondRedirect(request.authorizationUrl)
        }

        get("/gardener/api/oauth/{dataSourceId}/callback") {
            val dataSourceId = call.parameters["dataSourceId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val dataSource =
                dataSourceRegistry.findDataSourceById(dataSourceId)
                    ?: return@get call.respondUnknownDataSource(dataSourceId, logger)
            logger.info("Authorization Callback called for {}", dataSourceId)

            val stateId = call.request.queryParameters["state"] ?: ""
            if (dataSource.getAuthorizationType() == AuthorizationType.OAUTH2) {
                val code = call.request.queryParameters["code"] ?: ""
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

            // The event is handled synchronously; its handler marks the state successful once the tokens are stored.
            val connected = runCatching { stateService.getById(stateId).success }.getOrDefault(false)
            if (!connected) logger.warn("Connecting {} failed for authorization state '{}'.", dataSourceId, stateId)
            call.respondConnectionPage(dataSourceId, connected)
        }

        // Withings accepts a callback or notification URL only if it answers a HEAD request with 200.
        listOf("/gardener/api/oauth/{dataSourceId}/callback", "/gardener/api/collection/{dataSourceId}").forEach { path ->
            head(path) {
                val dataSourceId = call.parameters["dataSourceId"] ?: return@head call.respond(HttpStatusCode.BadRequest)
                dataSourceRegistry.findDataSourceById(dataSourceId)
                    ?: return@head call.respondUnknownDataSource(dataSourceId, logger)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/gardener/api/collection/{dataSourceId}") {
            val dataSourceId = call.parameters["dataSourceId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val dataSource =
                dataSourceRegistry.findDataSourceById(dataSourceId)
                    ?: return@post call.respondUnknownDataSource(dataSourceId, logger)
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

        get("/gardener/api/collection/fitbit") {
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

// Unknown ids and data sources left inactive (no credentials configured) are both "not found" to callers.
private suspend fun ApplicationCall.respondUnknownDataSource(
    dataSourceId: String,
    logger: Logger,
) {
    logger.warn("Request for unknown or inactive data source '{}'.", dataSourceId)
    respond(HttpStatusCode.NotFound)
}

/**
 * Tells the participant, who lands here from the vendor, whether connecting [dataSourceId] worked.
 * Always 200: vendors may check the callback URL, and the participant only needs the message.
 */
private suspend fun ApplicationCall.respondConnectionPage(
    dataSourceId: String,
    connected: Boolean,
) {
    val vendor = dataSourceId.replaceFirstChar { it.uppercase() }
    val (title, message) =
        if (connected) {
            "$vendor connected" to "Your $vendor data will now be shared with the study. You can close this page."
        } else {
            "$vendor could not be connected" to
                "Please open the link from the study team again. If it keeps failing, contact the study team."
        }
    respondText(connectionPageHtml(title, message), ContentType.Text.Html)
}

private fun connectionPageHtml(
    title: String,
    message: String,
): String =
    """
    <!doctype html>
    <html lang="en">
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <title>$title</title>
      <style>
        :root { color-scheme: light dark; --bg: #f6f7f9; --card: #fff; --text: #1d2330; --muted: #5b6475; }
        @media (prefers-color-scheme: dark) { :root { --bg: #14171c; --card: #1e232b; --text: #e8ebf0; --muted: #a2abb9; } }
        body { margin: 0; min-height: 100vh; display: grid; place-items: center; background: var(--bg);
               color: var(--text); font: 16px/1.5 system-ui, -apple-system, "Segoe UI", sans-serif; }
        main { margin: 16px; max-width: 420px; padding: 32px 28px; border-radius: 12px; background: var(--card); }
        h1 { margin: 0 0 8px; font-size: 1.4rem; }
        p { margin: 0; color: var(--muted); }
      </style>
    </head>
    <body><main><h1>$title</h1><p>$message</p></main></body>
    </html>
    """.trimIndent()

private fun buildApplicationData(
    deploymentId: String?,
    deviceRoleName: String?,
): String? {
    if (deploymentId.isNullOrBlank()) return null
    val mapper = ConfiguredObjectMapper.instance
    val node =
        mapper.createObjectNode().apply {
            put("deploymentId", deploymentId)
            if (!deviceRoleName.isNullOrBlank()) {
                put("deviceRoleName", deviceRoleName)
            }
        }
    return mapper.writeValueAsString(node)
}
