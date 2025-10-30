package dk.carp.gardener.authentication.core.common.util.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * A singleton class of Jackson's [ObjectMapper] instance
 * configured.
 */
object ConfiguredObjectMapper {
    val instance: ObjectMapper =
        ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
}
