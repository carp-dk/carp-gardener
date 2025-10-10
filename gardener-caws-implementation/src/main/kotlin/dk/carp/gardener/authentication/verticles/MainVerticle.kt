package dk.carp.gardener.authentication.verticles

import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth1AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationrequest.OAuth2AuthorizationRequestParams
import dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationStateServiceHost
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateRepository
import dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService
import dk.carp.gardener.authentication.core.authorization.datasource.AuthorizationType
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
import dk.carp.gardener.authentication.core.common.events.oauth1.OAuth1Event
import dk.carp.gardener.authentication.core.common.events.oauth2.OAuth2Event
import dk.carp.gardener.authentication.core.common.transformer.DataTypeTransformerRegistryHost
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformerRegistry
import dk.carp.gardener.authentication.core.infrastructure.eventbus.SingleThreadedEventBus
import dk.carp.gardener.authentication.core.infrastructure.transformer.dexcom.DexcomEmptyTransformerI
import dk.carp.gardener.authentication.implementation.oauth1.OAuth1DataCollectionOperatorBuilder
import dk.carp.gardener.authentication.implementation.oauth1.OAuth1Operator
import dk.carp.gardener.authentication.implementation.oauth2.OAuth2DataCollectionOperatorBuilder
import dk.carp.gardener.authentication.implementation.oauth2.OAuth2Operator
import dk.carp.gardener.authentication.implementation.publisher.RabbitMqDataPublisher
import dk.carp.gardener.authentication.implementation.repository.MongoAccessParamsRepository
import dk.carp.gardener.authentication.implementation.repository.MongoAuthorizationStateRepository
import dk.carp.gardener.authentication.implementation.transformer.fitbit.FitbitCarpTransformerI
import dk.carp.gardener.authentication.implementation.transformer.garmin.GarminCarpTransformerI
import dk.carp.gardener.authentication.implementation.transformer.withings.WithingsCarpTransformerI
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.core.net.pemKeyCertOptionsOf
import org.slf4j.LoggerFactory

/**
 * The Main Verticle that sets up the web server.
 */
class MainVerticle : AbstractVerticle() {

  companion object {
    private val LOGGER = LoggerFactory.getLogger(MainVerticle::class.java)
  }

  lateinit var eventBus: IEventBus
    private set
  lateinit var authorizationStateRepository: IAuthorizationStateRepository
    private set
  lateinit var authorizationStateService: IAuthorizationStateService
    private set
  lateinit var accessParamsRepository: IAccessParamsRepository
    private set
  lateinit var accessParamService: IAccessParamsService
    private set
  lateinit var dataSourceRegistry: IDataSourceRegistry
    private set
  lateinit var transformerRegistry: IDataTypeTransformerRegistry
    private set
  lateinit var publisher: IDataPublisher
    private set
  lateinit var oauth1DataCollectionService: IDataCollectionService
    private set
  lateinit var oauth2DataCollectionService: IDataCollectionService
    private set
  lateinit var mongoClient: MongoClient
    private set

  /**
   * Instantiates core services and declares routes.
   */
  private fun setUp(router: Router, properties: PropertiesConfig) {
    LOGGER.info("Starting set-up phase.")
    // Service inits

    // Common
    eventBus = SingleThreadedEventBus()
    LOGGER.info("MongoDB connection establishment starting for ${properties.getProperty("mongo.db.connection_string")}/${properties.getProperty("mongo.db.name")}")
    mongoClient = MongoClient.createShared(vertx, JsonObject().apply {
      put("connection_string", properties.getProperty("mongo.db.connection_string"))
      put("db_name", properties.getProperty("mongo.db.name"))
      put("username", properties.getProperty("mongo.db.username"))
      put("password", properties.getProperty("mongo.db.password"))
    })

    // Authentication module
    authorizationStateRepository = MongoAuthorizationStateRepository(mongoClient)
    authorizationStateService = AuthorizationStateServiceHost(authorizationStateRepository)
    accessParamsRepository = MongoAccessParamsRepository(mongoClient)
    accessParamService = AccessParamsServiceHost(accessParamsRepository)
    dataSourceRegistry = DataSourceRegistryHost(eventBus)

    // Data collection module
    transformerRegistry = DataTypeTransformerRegistryHost().apply {
      registerTransformer(FitbitDataSource.DATA_SOURCE_ID, FitbitCarpTransformerI())
      registerTransformer(GarminDataSource.DATA_SOURCE_ID, GarminCarpTransformerI())
      registerTransformer(WithingsDataSource.DATA_SOURCE_ID, WithingsCarpTransformerI())
      registerTransformer(DexcomDataSource.DATA_SOURCE_ID, DexcomEmptyTransformerI())
    }
    publisher = RabbitMqDataPublisher(properties)
    oauth2DataCollectionService = OAuth2DataCollectionService(eventBus, publisher, transformerRegistry, OAuth2DataCollectionOperatorBuilder(vertx, properties))
    oauth1DataCollectionService = OAuth1DataCollectionService(eventBus, publisher, transformerRegistry, OAuth1DataCollectionOperatorBuilder())

    // Register modules

    // Fitbit
    val fitbitClientSettings = OAuth2ClientSettings(
      clientId = properties.getProperty("fitbit.client.id"),
      clientSecret = properties.getProperty("fitbit.client.secret"),
      authorizationUri = properties.getProperty("fitbit.oauth2.authorization.uri"),
      accessUri = properties.getProperty("fitbit.oauth2.access.uri"),
      dataUrl = properties.getProperty("fitbit.data.uri"),
      callbackUri = properties.getProperty("fitbit.client.callback.uri"),
    )
    val fitbitDataSource = FitbitDataSource(
      eventBus,
      authorizationStateService,
      accessParamService,
      fitbitClientSettings,
      OAuth2Operator(fitbitClientSettings, vertx, properties)
    )
    dataSourceRegistry.activateDataSource(fitbitDataSource)

    // Garmin
    val garminClientSettings = OAuth1ClientSettings(
      consumerKey = properties.getProperty("garmin.consumer.key"),
      consumerSecret = properties.getProperty("garmin.consumer.secret"),
      signatureMethod = properties.getProperty("garmin.oauth.signature.method"),
      version = properties.getProperty("garmin.oauth.version"),
      requestTokenUri = properties.getProperty("garmin.oauth.request_token.uri"),
      accessTokenUri = properties.getProperty("garmin.oauth.access_token.uri"),
      authorizationUri = properties.getProperty("garmin.oauth.authorization.uri"),
      dataUrl = properties.getProperty("garmin.data.uri"),
      clientCallbackUri = properties.getProperty("garmin.client.callback.uri"),
    )
    val garminDataSource = GarminDataSource(
      eventBus,
      authorizationStateService,
      accessParamService,
      garminClientSettings,
      OAuth1Operator(garminClientSettings)
    )
    dataSourceRegistry.activateDataSource(garminDataSource)

    // Withings
    val withingsClientSettings = OAuth2ClientSettings(
      clientId = properties.getProperty("withings.client.id"),
      clientSecret = properties.getProperty("withings.client.secret"),
      authorizationUri = properties.getProperty("withings.oauth2.authorization.uri"),
      accessUri = properties.getProperty("withings.oauth2.access.uri"),
      dataUrl = properties.getProperty("withings.data.uri"),
      callbackUri = properties.getProperty("withings.client.callback.uri"),
    )
    val withingsDataSource = WithingsDataSource(
      eventBus,
      authorizationStateService,
      accessParamService,
      withingsClientSettings,
      OAuth2Operator(withingsClientSettings, vertx, properties)
    )
    dataSourceRegistry.activateDataSource(withingsDataSource)

    // Dexcom
    val dexcomClientSettings = OAuth2ClientSettings(
      clientId = properties.getProperty("dexcom.client.id"),
      clientSecret = properties.getProperty("dexcom.client.secret"),
      authorizationUri = properties.getProperty("dexcom.oauth2.authorization.uri"),
      accessUri = properties.getProperty("dexcom.oauth2.access.uri"),
      dataUrl = properties.getProperty("dexcom.data.uri"),
      callbackUri = properties.getProperty("dexcom.client.callback.uri"),
    )
    val dexcomDataSource = DexcomDataSource(
      eventBus,
      authorizationStateService,
      accessParamService,
      dexcomClientSettings,
      OAuth2Operator(dexcomClientSettings, vertx, properties)
    )
    dataSourceRegistry.activateDataSource(dexcomDataSource)

    // Route registration

    // Common authorization endpoint
    router.get("/wearables/api/authorize/:dataSourceId/:userId").handler { ctx: RoutingContext ->
      val dataSourceId = ctx.request().getParam("dataSourceId")
      val userId = ctx.request().getParam("userId")
      val deploymentId = ctx.request().getParam("deploymentId")
      LOGGER.info("Authorization request initiated for $dataSourceId/$userId")

      val dataSource = dataSourceRegistry.getDataSourceById(dataSourceId)
      val request = if (dataSource.getAuthorizationType() == AuthorizationType.OAUTH2) {
        val scopes: String = ctx.request().getParam("scopes")
        val parsedScopes = scopes.split(",").toList()
        val authorizationParams = dataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
        authorizationParams.applicationData = deploymentId
        authorizationParams.scopes.addAll(parsedScopes)
        dataSource.initiateUserAuthorization(userId, dataSourceId, authorizationParams)
      } else {
        val authorizationParams = dataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams
        authorizationParams.applicationData = deploymentId
        dataSource.initiateUserAuthorization(userId, dataSourceId, authorizationParams)
      }

      LOGGER.info("Redirecting $dataSourceId/$userId to: ${request.authorizationUrl}")
      ctx
        .response()
        .putHeader("Location", request.authorizationUrl)
        .setStatusCode(302)
        .end()
    }

    // Common callback endpoint
    router.get("/wearables/api/oauth/:dataSourceId/callback").handler { ctx: RoutingContext ->
      val dataSourceId = ctx.request().getParam("dataSourceId")
      val dataSource = dataSourceRegistry.getDataSourceById(dataSourceId)
      LOGGER.info("Authorization Callback called for $dataSourceId")

      if (dataSource.getAuthorizationType() == AuthorizationType.OAUTH2) {
        val code = ctx.request().getParam("code")
        val stateId = ctx.request().getParam("state")
        eventBus.publish(this::class, OAuth2Event.AuthorizationCodeAcquired(
          code = code,
          stateId = stateId,
          dataSourceId = dataSourceId,
          params = dataSource.getEstablishedAuthorizationRequestParams() as OAuth2AuthorizationRequestParams
        ))
      } else {
        val stateId = ctx.request().getParam("state")
        val token = ctx.request().getParam("oauth_token")
        val tokenVerifier = ctx.request().getParam("oauth_verifier")
        eventBus.publish(this::class, OAuth1Event.AuthorizedTokenAcquired(
          stateId = stateId,
          requestToken = token,
          tokenVerifier = tokenVerifier,
          dataSourceId = dataSourceId,
          params = dataSource.getEstablishedAuthorizationRequestParams() as OAuth1AuthorizationRequestParams
        ))
      }

      ctx.response().setStatusCode(200).end()
    }

    // Common data collection initiation
    router.post("/wearables/api/collection/:dataSourceId").handler(BodyHandler.create()).handler { ctx: RoutingContext ->
      val dataSourceId = ctx.request().getParam("dataSourceId")
      val dataSource = dataSourceRegistry.getDataSourceById(dataSourceId)
      LOGGER.info("Data collection callback called for $dataSourceId")
      val payload: String? = ctx.body().asString()
      // Closing the connection towards the third-party API before making callbacks
      // According to the vendor's policy.
      when (dataSourceId) {
        FitbitDataSource.DATA_SOURCE_ID -> ctx.response().setStatusCode(204).end()
        else -> ctx.response().setStatusCode(200).end()
      }

      if (payload == null) {
        LOGGER.info("Payload is null. request aborted.")
        return@handler
      }
      LOGGER.info("Data Collection Payload for $dataSourceId: $payload")
      val events = dataSource.getDataCollectionPreparationEventFromPing(payload)
      events.forEach { event -> eventBus.publish(this::class, event) }
    }

    // Fitbit subscriber verification endpoint
    router.get("/wearables/api/collection/fitbit").handler(BodyHandler.create()).handler { ctx: RoutingContext ->
      LOGGER.info("Fitbit subscriber verification endpoint called.")
      val verificationCode = properties.getProperty("fitbit.client.subscription.code")

      if (ctx.queryParam("verify").size != 0) {
        if (ctx.queryParam("verify")[0] == verificationCode) {
          LOGGER.info("Correct fitbit verification code received.")
          ctx.response().setStatusCode(204).end()
        } else {
          LOGGER.info("Incorrect fitbit verification code received.")
          ctx.response().setStatusCode(404).end()
        }
      }
    }

    LOGGER.info("Set-up phase successfully conducted.")
  }

  /**
   * Starts the web server.
   */
  override fun start(startPromise: Promise<Void>) {
    val properties = PropertiesConfig(vertx)

    val webServerPort = properties.getProperty("webserver.port").toInt()
    val httpServerOptions = httpServerOptionsOf(
      port = webServerPort,
      ssl = true,
      pemKeyCertOptions = pemKeyCertOptionsOf(
        certPath = properties.getProperty("webserver.cert.path"),
        keyPath = properties.getProperty("webserver.key.path")
      )
    )

    val router = Router.router(vertx)
    setUp(router, properties)

    vertx.createHttpServer(httpServerOptions)
      .requestHandler(router)
      .listen { async ->
        if (async.succeeded()) {
          LOGGER.info("Webserver started on port $webServerPort")
          if (properties.getProperty("webserver.environment") == "local" || properties.getProperty("webserver.environment") == "test") {
            LOGGER.info("Authorization URI for Fitbit: https://localhost:$webServerPort/wearables/api/authorize/fitbit/userid?scopes=activity,heartrate,weight,sleep,nutrition,profile,settings&deploymentId=deploymentId")
            LOGGER.info("Authorization URI for Garmin: https://localhost:$webServerPort/wearables/api/authorize/garmin/userid")
            LOGGER.info("Authorization URI for Withings: https://localhost:$webServerPort/wearables/api/authorize/withings/userid?scopes=user.metrics,user.activitys&deploymentId=deploymentId")
            LOGGER.info("Authorization URI for Dexcom: https://localhost:$webServerPort/wearables/api/authorize/dexcom/userid?deploymentId=deploymentId&scopes=")
            startPromise.complete()
          }
        } else {
          val errorMsg = "Error on creating the WebServer Verticle. Cause: ${async.cause()}"
          LOGGER.info(errorMsg)
          startPromise.fail(errorMsg)
        }
      }
  }
}
