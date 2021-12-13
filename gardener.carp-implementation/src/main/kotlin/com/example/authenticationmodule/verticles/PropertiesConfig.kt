package com.example.authenticationmodule.verticles

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
        val profile = System.getenv("profile") ?: "test"
        LOGGER.info("The following profile is active: $profile")
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
        properties = future.result()
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