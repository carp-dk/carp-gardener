package dk.carp.gardener.authentication.implementation.transformer.carp

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.io.Serializable
import java.time.Instant

/**
 * CARP data point DTO Header (CARP Core v28)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CarpDataPointHeader(
    val studyId: String? = null,
    val userId: String? = null,
    val dataFormat: CarpDataPointFormat? = null,
    var triggerId: String? = null,
    var deviceRoleName: String? = null,
    var uploadTime: Instant = Instant.now(),
    var startTime: Instant = Instant.now(),
    var endTime: Instant = Instant.now(),
) : Serializable
