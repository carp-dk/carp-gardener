package dk.carp.gardener.authentication.ktor

import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import org.slf4j.LoggerFactory

/**
 * Simple configuration loader that reads JSON profiles from the classpath.
 */
class PropertiesConfig(
    profile: String = System.getenv("profile") ?: "local",
    private val loader: PropertiesLoader = PropertiesLoader(),
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(PropertiesConfig::class.java)
    }

    private val properties: Map<String, Any?>

    init {
        LOGGER.info("The following profile is active: {}", profile)
        properties = loader.loadFromClasspath(profile)
        LOGGER.info("Application properties successfully set.")
    }

    fun getProperty(key: String): String =
        properties[key]?.toString()
            ?: throw IllegalArgumentException("No configuration value found for key '$key'")

    fun getOptionalProperty(key: String): String? = properties[key]?.toString()

    fun isEnabled(
        key: String,
        default: Boolean = true,
    ): Boolean {
        val raw = properties[key]?.toString() ?: return default
        return when {
            raw.equals("true", ignoreCase = true) -> true
            raw.equals("false", ignoreCase = true) -> false
            raw == "1" -> true
            raw == "0" -> false
            else -> default
        }
    }
}

class PropertiesLoader(
    private val mapper: com.fasterxml.jackson.databind.ObjectMapper = ConfiguredObjectMapper.instance,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(PropertiesLoader::class.java)
    }

    fun loadFromClasspath(profile: String): Map<String, Any?> {
        val configFileName = "conf/config-$profile.json"
        LOGGER.info("Loading configuration from classpath: {}", configFileName)
        val resource =
            PropertiesLoader::class.java.classLoader.getResource(configFileName)
                ?: throw IllegalArgumentException("Configuration file not found on classpath: $configFileName")
        val rawConfig = resource.readText()
        LOGGER.info("Configuration file {} successfully read ({} bytes).", configFileName, rawConfig.length)
        return mapper.readValue(rawConfig, mapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java))
    }
}
