package dk.carp.gardener.authentication.ktor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PropertiesConfigTest {
    private val config =
        PropertiesConfig(
            profile = "unit",
            loader =
            mock {
                on { loadFromClasspath("unit") } doReturn
                    mapOf(
                        "propertiesconfigtest.set" to "value",
                        "propertiesconfigtest.blank" to "",
                        "propertiesconfigtest.whitespace" to "  ",
                    )
            },
        )

    @Test
    fun setValueIsReturned() {
        assertEquals("value", config.getProperty("propertiesconfigtest.set"))
    }

    @Test
    fun blankValueCountsAsMissing() {
        val error = assertThrows<IllegalArgumentException> { config.getProperty("propertiesconfigtest.blank") }
        assertEquals("No configuration value found for key 'propertiesconfigtest.blank'", error.message)
        assertThrows<IllegalArgumentException> { config.getProperty("propertiesconfigtest.whitespace") }
    }

    @Test
    fun missingKeyFails() {
        assertThrows<IllegalArgumentException> { config.getProperty("propertiesconfigtest.absent") }
    }

    @Test
    fun optionalPropertyStillReturnsBlankAsIs() {
        assertEquals("", config.getOptionalProperty("propertiesconfigtest.blank"))
        assertNull(config.getOptionalProperty("propertiesconfigtest.absent"))
    }
}
