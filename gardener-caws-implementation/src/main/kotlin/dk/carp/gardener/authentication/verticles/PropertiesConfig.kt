package dk.carp.gardener.authentication.verticles

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory


/**
 * Manages the configuration files.
 */
class PropertiesConfig(vertx: Vertx) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(PropertiesConfig::class.java)
    }

    private val properties: JsonObject

    init {
        // If no profile is set, use the 'test' configuration.
        val profile = System.getenv("profile") ?: "local"
        LOGGER.info("The following profile is active: $profile")

        properties = if (profile == "prod") {
            // Retrieve configuration from Spring Config Server
            val stores = SpringCloudConfigStoreProvider().options
            val future = ConfigRetriever.create(
                vertx,
                ConfigRetrieverOptions().addStore(stores)
            ).config

            LOGGER.info("Configuration retrieved: ${future.isComplete}.")

            while (future == null) { }
            if (future.failed())
            {
                throw IllegalArgumentException("Exception occurred while setting up the configuration: ${future.cause().message}")
            }
            LOGGER.info("Application properties successfully set.")
            future.result()
        } else {
            // Retrieve configuration locally
            val configuration = getConfig(profile)

            val store = ConfigStoreOptions()
                .setType("json")
                .setConfig(configuration)

            val future = ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(store)).config
            while (future.result() == null) {}
            if (future.failed()) {
                throw IllegalArgumentException("Exception occurred while setting up the configuration: ${future.cause().message}")
            }
            LOGGER.info("Application properties successfully set.")
            future.result()
        }
    }

    /**
     * Retrieves a property's value by its [key].
     */
    fun getProperty(key: String): String {
        return properties.getString(key)
    }

    /**
     * Reads up the profile specific configuration file.
     */
    private fun getConfig(profile: String): JsonObject {
        val configFileName = "conf/config-$profile.json"
        LOGGER.info("Reading up the following config file: $configFileName")
        val applicationConfig: String
        try {
            applicationConfig = this::class.java.classLoader.getResource(configFileName).readText()
        } catch (ex: Exception) {
            throw IllegalArgumentException("Exception occurred while reading in the configuration $configFileName: ${ex.message}")
        }
        return JsonObject(applicationConfig)
    }
}

class SpringCloudConfigStoreProvider
{
    val options: ConfigStoreOptions
        get()
        {
            return ConfigStoreOptions()
                .setType("spring-config-server")
                .setConfig(
                    JsonObject()
                        .put("url", "http://localhost:8888/config-client/gardener")
                        .put("user", "carp")
                        .put("password", "ENC(aziKqkaLO34e5ad423gd123)")
                        .put("timeout", 10000)
                )
        }
}

class WebClientProvider
{
    // if the SpringCloudConfigStoreProvider doesn't work -> use the simply Webclient or a vertx router
    // curl --location --request GET 'http://localhost:8888/config-client/gardener' --header 'Authorization: Basic Y2FycDpFTkMoYXppS3FrYUxPMzRlNWFkNDIzZ2QxMjMp' --header 'Cookie: JSESSIONID=362A753E1F63813BCBA33F8069B605A0'
}