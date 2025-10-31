package dk.carp.gardener.authentication.ktor

import com.github.scribejava.core.builder.ServiceBuilder
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationStateServiceHost
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateRepository
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.datasource.oauth1.OAuth1ClientSettings
import dk.carp.gardener.authentication.core.authorization.datasource.oauth2.OAuth2ClientSettings
import dk.carp.gardener.authentication.core.authorization.datasourceregistry.DataSourceRegistryHost
import dk.carp.gardener.authentication.core.authorization.datasourceregistry.IDataSourceRegistry
import dk.carp.gardener.authentication.core.authorization.devices.dexcom.DexcomDataSource
import dk.carp.gardener.authentication.core.authorization.devices.fitbit.FitbitDataSource
import dk.carp.gardener.authentication.core.authorization.devices.garmin.GarminDataSource
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataSource
import dk.carp.gardener.authentication.core.collection.IDataCollectionService
import dk.carp.gardener.authentication.core.collection.oauth1.OAuth1DataCollectionService
import dk.carp.gardener.authentication.core.collection.oauth2.OAuth2DataCollectionService
import dk.carp.gardener.authentication.core.collection.publisher.IDataPublisher
import dk.carp.gardener.authentication.core.common.accessparams.AccessParamsServiceHost
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsRepository
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.transformer.DataTypeTransformerRegistryHost
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformerRegistry
import dk.carp.gardener.authentication.core.infrastructure.eventbus.SingleThreadedEventBus
import dk.carp.gardener.authentication.implementation.oauth1.AdapterOAuth1Operator
import dk.carp.gardener.authentication.implementation.oauth1.CoroutineOAuth1Operator
import dk.carp.gardener.authentication.implementation.oauth1.OAuth1ApiDefinition
import dk.carp.gardener.authentication.implementation.oauth1.OAuth1DataCollectionOperatorBuilder
import dk.carp.gardener.authentication.implementation.oauth2.OAuth2DataCollectionOperatorBuilder
import dk.carp.gardener.authentication.implementation.oauth2.OAuth2Operator
import dk.carp.gardener.authentication.implementation.publisher.RabbitMqDataPublisher
import dk.carp.gardener.authentication.implementation.repository.AdapterAccessParamsRepository
import dk.carp.gardener.authentication.implementation.repository.AdapterAuthorizationStateRepository
import dk.carp.gardener.authentication.implementation.repository.CoroutineMongoAccessParamsRepository
import dk.carp.gardener.authentication.implementation.repository.CoroutineMongoAuthorizationStateRepository
import dk.carp.gardener.authentication.implementation.transformer.withings.WithingsCarpTransformerI
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("LongMethod")
fun main() {
    val properties = PropertiesConfig()
    val logger = LoggerFactory.getLogger("MainApplication")

    logger.info("Starting Gardener authentication service bootstrap.")

    val webServerPort = properties.getProperty("webserver.port").toInt()
    logger.info("Configured web server port: {}", webServerPort)

    val eventBus: IEventBus = SingleThreadedEventBus()

    logger.info(
        "MongoDB connection establishment starting for ${properties.getProperty(
            "mongo.db.connection_string",
        )}/${properties.getProperty("mongo.db.name")}",
    )
    val mongoClient: MongoClient = createMongoClient(properties)
    val mongoDatabaseName = properties.getProperty("mongo.db.name")
    logger.info("MongoDB client successfully created for database '{}'.", mongoDatabaseName)

    val authorizationStateRepository: IAuthorizationStateRepository =
        AdapterAuthorizationStateRepository(CoroutineMongoAuthorizationStateRepository(mongoClient, mongoDatabaseName))
    val authorizationStateService: IAuthorizationStateService =
        AuthorizationStateServiceHost(authorizationStateRepository)

    val accessParamsRepository: IAccessParamsRepository =
        AdapterAccessParamsRepository(CoroutineMongoAccessParamsRepository(mongoClient, mongoDatabaseName))
    val accessParamService: IAccessParamsService = AccessParamsServiceHost(accessParamsRepository)
    val dataSourceRegistry: IDataSourceRegistry = DataSourceRegistryHost(eventBus)

    val transformerRegistry: IDataTypeTransformerRegistry =
        DataTypeTransformerRegistryHost().apply {
            registerTransformer(WithingsDataSource.DATA_SOURCE_ID, WithingsCarpTransformerI())
        }

    val publisher: IDataPublisher = RabbitMqDataPublisher(properties)

    @Suppress("UNUSED_VARIABLE")
    val oauth2DataCollectionService: IDataCollectionService =
        OAuth2DataCollectionService(eventBus, publisher, transformerRegistry, OAuth2DataCollectionOperatorBuilder())

    @Suppress("UNUSED_VARIABLE")
    val oauth1DataCollectionService: IDataCollectionService =
        OAuth1DataCollectionService(eventBus, publisher, transformerRegistry, OAuth1DataCollectionOperatorBuilder())

    registerOAuth2DataSource(
        name = "Withings",
        enabled = properties.isEnabled("withings.enabled"),
        clientId = properties.getOptionalProperty("withings.client.id"),
        clientSecret = properties.getOptionalProperty("withings.client.secret"),
        logger = logger,
        settingsFactory = { clientId, clientSecret ->
            OAuth2ClientSettings(
                clientId = clientId,
                clientSecret = clientSecret,
                authorizationUri = properties.getProperty("withings.oauth2.authorization.uri"),
                accessUri = properties.getProperty("withings.oauth2.access.uri"),
                dataUrl = properties.getProperty("withings.data.uri"),
                callbackUri = properties.getProperty("withings.client.callback.uri"),
            )
        },
        activator = { settings ->
            val dataSource =
                WithingsDataSource(
                    eventBus,
                    authorizationStateService,
                    accessParamService,
                    settings,
                    OAuth2Operator(settings, properties),
                )
            dataSourceRegistry.activateDataSource(dataSource)
        },
    )

    registerOAuth2DataSource(
        name = "Fitbit",
        enabled = properties.isEnabled("fitbit.enabled"),
        clientId = properties.getOptionalProperty("fitbit.client.id"),
        clientSecret = properties.getOptionalProperty("fitbit.client.secret"),
        logger = logger,
        settingsFactory = { clientId, clientSecret ->
            OAuth2ClientSettings(
                clientId = clientId,
                clientSecret = clientSecret,
                authorizationUri = properties.getProperty("fitbit.oauth2.authorization.uri"),
                accessUri = properties.getProperty("fitbit.oauth2.access.uri"),
                dataUrl = properties.getProperty("fitbit.data.uri"),
                callbackUri = properties.getProperty("fitbit.client.callback.uri"),
            )
        },
        activator = { settings ->
            val dataSource =
                FitbitDataSource(
                    eventBus,
                    authorizationStateService,
                    accessParamService,
                    settings,
                    OAuth2Operator(settings, properties),
                )
            dataSourceRegistry.activateDataSource(dataSource)
        },
    )

    registerOAuth2DataSource(
        name = "Dexcom",
        enabled = properties.isEnabled("dexcom.enabled"),
        clientId = properties.getOptionalProperty("dexcom.client.id"),
        clientSecret = properties.getOptionalProperty("dexcom.client.secret"),
        logger = logger,
        settingsFactory = { clientId, clientSecret ->
            OAuth2ClientSettings(
                clientId = clientId,
                clientSecret = clientSecret,
                authorizationUri = properties.getProperty("dexcom.oauth2.authorization.uri"),
                accessUri = properties.getProperty("dexcom.oauth2.access.uri"),
                dataUrl = properties.getProperty("dexcom.data.uri"),
                callbackUri = properties.getProperty("dexcom.client.callback.uri"),
            )
        },
        activator = { settings ->
            val dataSource =
                DexcomDataSource(
                    eventBus,
                    authorizationStateService,
                    accessParamService,
                    settings,
                    OAuth2Operator(settings, properties),
                )
            dataSourceRegistry.activateDataSource(dataSource)
        },
    )

    registerOAuth1DataSource(
        name = "Garmin",
        enabled = properties.isEnabled("garmin.enabled"),
        consumerKey = properties.getOptionalProperty("garmin.consumer.key"),
        consumerSecret = properties.getOptionalProperty("garmin.consumer.secret"),
        logger = logger,
        settingsFactory = { consumerKey, consumerSecret ->
            OAuth1ClientSettings(
                consumerKey = consumerKey,
                consumerSecret = consumerSecret,
                signatureMethod = properties.getProperty("garmin.oauth.signature.method"),
                version = properties.getProperty("garmin.oauth.version"),
                requestTokenUri = properties.getProperty("garmin.oauth.request_token.uri"),
                accessTokenUri = properties.getProperty("garmin.oauth.access_token.uri"),
                authorizationUri = properties.getProperty("garmin.oauth.authorization.uri"),
                dataUrl = properties.getProperty("garmin.data.uri"),
                clientCallbackUri = properties.getProperty("garmin.client.callback.uri"),
            )
        },
        activator = { settings ->
            val service =
                ServiceBuilder(settings.consumerKey)
                    .apiSecret(settings.consumerSecret)
                    .build(OAuth1ApiDefinition(settings.requestTokenUri, settings.accessTokenUri, settings.authorizationUri))
            val operator = AdapterOAuth1Operator(CoroutineOAuth1Operator(settings, service))
            val dataSource =
                GarminDataSource(
                    eventBus,
                    authorizationStateService,
                    accessParamService,
                    settings,
                    operator,
                )
            dataSourceRegistry.activateDataSource(dataSource)
        },
    )

    logger.info("Set-up phase successfully conducted.")

    val server =
        embeddedServer(Netty, port = webServerPort) {
            install(ContentNegotiation) { jackson() }
            configureRouting(dataSourceRegistry, eventBus, properties, logger)
        }

    server.environment.monitor.subscribe(ApplicationStarted) {
        logger.info("Ktor server started on port {}", webServerPort)
        logAuthorizationSamples(webServerPort, properties, logger)
    }

    server.start(wait = true)
}

fun Application.module() {
    // empty - reserved for Ktor test tooling
}

private fun createMongoClient(properties: PropertiesConfig): MongoClient {
    val connectionString = properties.getProperty("mongo.db.connection_string")
    val username = properties.getOptionalProperty("mongo.db.username")
    val password = properties.getOptionalProperty("mongo.db.password")
    val database = properties.getProperty("mongo.db.name")

    if (username.isNullOrBlank() || password.isNullOrBlank()) {
        return MongoClients.create(connectionString)
    }

    val credential = MongoCredential.createCredential(username, database, password.toCharArray())
    val settings =
        MongoClientSettings
            .builder()
            .applyConnectionString(ConnectionString(connectionString))
            .credential(credential)
            .build()
    return MongoClients.create(settings)
}

@Suppress("LongParameterList")
private fun registerOAuth2DataSource(
    name: String,
    enabled: Boolean,
    clientId: String?,
    clientSecret: String?,
    logger: Logger,
    settingsFactory: (String, String) -> OAuth2ClientSettings,
    activator: (OAuth2ClientSettings) -> Unit,
) {
    if (!enabled) {
        logger.info("{} data source disabled via configuration flag.", name)
        return
    }

    val resolvedClientId = clientId?.takeIf { it.isNotBlank() }
    val resolvedClientSecret = clientSecret?.takeIf { it.isNotBlank() }
    if (resolvedClientId == null || resolvedClientSecret == null) {
        logger.info("{} client credentials not provided; skipping data source activation in this environment.", name)
        return
    }

    val settings = settingsFactory(resolvedClientId, resolvedClientSecret)
    activator(settings)
    logger.info("{} data source activated.", name)
}

@Suppress("LongParameterList")
private fun registerOAuth1DataSource(
    name: String,
    enabled: Boolean,
    consumerKey: String?,
    consumerSecret: String?,
    logger: Logger,
    settingsFactory: (String, String) -> OAuth1ClientSettings,
    activator: (OAuth1ClientSettings) -> Unit,
) {
    if (!enabled) {
        logger.info("{} data source disabled via configuration flag.", name)
        return
    }

    val resolvedKey = consumerKey?.takeIf { it.isNotBlank() }
    val resolvedSecret = consumerSecret?.takeIf { it.isNotBlank() }
    if (resolvedKey == null || resolvedSecret == null) {
        logger.info("{} consumer credentials not provided; skipping data source activation in this environment.", name)
        return
    }

    val settings = settingsFactory(resolvedKey, resolvedSecret)
    activator(settings)
    logger.info("{} data source activated.", name)
}

private fun logAuthorizationSamples(
    port: Int,
    properties: PropertiesConfig,
    logger: Logger,
) {
    val environment = properties.getProperty("webserver.environment")
    if (environment != "local" && environment != "test") {
        return
    }

    val baseAuthorizeUrl = "http://localhost:$port/wearables/api/authorize"
    logger.info(
        "Authorization URI for Fitbit: {}",
        "$baseAuthorizeUrl/fitbit/userid?scopes=activity,heartrate,weight,sleep,nutrition,profile,settings&deploymentId=deploymentId",
    )
    logger.info("Authorization URI for Garmin: {}/garmin/userid", baseAuthorizeUrl)
    logger.info(
        "Authorization URI for Withings: {}",
        "$baseAuthorizeUrl/withings/userid?scopes=user.metrics,user.activity&deploymentId=deploymentId",
    )
    logger.info(
        "Authorization URI for Dexcom: {}",
        "$baseAuthorizeUrl/dexcom/userid?deploymentId=deploymentId&scopes=",
    )
}
