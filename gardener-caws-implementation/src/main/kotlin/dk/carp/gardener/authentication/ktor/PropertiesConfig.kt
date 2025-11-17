package dk.carp.gardener.authentication.ktor

import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import org.slf4j.LoggerFactory
import java.io.IOException

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
    private val environmentOverrides: Map<String, String>

    init {
        LOGGER.info("The following profile is active: {}", profile)
        properties = loader.loadFromClasspath(profile)
        environmentOverrides = EnvOverridesLoader.load()
        LOGGER.info("Application properties successfully set.")
    }

    fun getProperty(key: String): String =
        environmentOverrides[key]
            ?: properties[key]?.toString()
            ?: throw IllegalArgumentException("No configuration value found for key '$key'")

    fun getOptionalProperty(key: String): String? = environmentOverrides[key] ?: properties[key]?.toString()

    fun isEnabled(
        key: String,
        default: Boolean = true,
    ): Boolean {
        val raw = environmentOverrides[key] ?: properties[key]?.toString() ?: return default
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

private object EnvOverridesLoader {
    private val LOGGER = LoggerFactory.getLogger(EnvOverridesLoader::class.java)

    fun load(): Map<String, String> {
        val overrides = mutableMapOf<String, String>()
        overrides.putAll(loadFromDotEnv())
        overrides.putAll(loadFromSystemEnv())
        if (overrides.isNotEmpty()) {
            LOGGER.info("Loaded {} configuration overrides from environment variables.", overrides.size)
        }
        return overrides
    }

    private fun loadFromSystemEnv(): Map<String, String> =
        System.getenv()
            .mapNotNull { (key, value) -> normalizeKey(key)?.let { it to value } }
            .toMap()

    private fun loadFromDotEnv(): Map<String, String> {
        val overrides = mutableMapOf<String, String>()
        val dotEnvPath = java.nio.file.Paths.get(".env")
        if (!java.nio.file.Files.exists(dotEnvPath)) {
            return overrides
        }

        try {
            java.nio.file.Files
                .readAllLines(dotEnvPath)
                .forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        return@forEach
                    }
                    val separatorIndex = trimmed.indexOf('=')
                    if (separatorIndex <= 0) {
                        return@forEach
                    }

                    val rawKey = trimmed.substring(0, separatorIndex).trim()
                    var rawValue = trimmed.substring(separatorIndex + 1).trim().stripWrappingQuotes()

                    if (rawValue.isEmpty()) {
                        return@forEach
                    }
                    val normalizedKey = normalizeKey(rawKey) ?: return@forEach
                    overrides.putIfAbsent(normalizedKey, rawValue)
                }
        } catch (ex: IOException) {
            LOGGER.warn("Failed loading .env overrides from {}: {}", dotEnvPath.toAbsolutePath(), ex.message)
        } catch (ex: SecurityException) {
            LOGGER.warn("Failed loading .env overrides from {}: {}", dotEnvPath.toAbsolutePath(), ex.message)
        }

        return overrides
    }

    private fun normalizeKey(rawKey: String): String? {
        if (rawKey.isBlank()) {
            return null
        }
        return rawKey
            .trim()
            .lowercase()
            .replace("__", ".")
    }

    private fun String.stripWrappingQuotes(): String {
        if (length < 2) {
            return this
        }
        return when {
            (startsWith("\"") && endsWith("\"")) -> substring(1, length - 1)
            (startsWith("'") && endsWith("'")) -> substring(1, length - 1)
            else -> this
        }
    }
}
