package dk.carp.gardener.authentication.implementation.transformer.withings

import com.fasterxml.jackson.databind.JsonNode
import dk.cachet.carp.common.infrastructure.serialization.COMMON_SERIAL_MODULE
import dk.cachet.carp.common.infrastructure.serialization.CustomData
import dk.cachet.carp.common.infrastructure.serialization.createDefaultJSON
import dk.cachet.carp.data.infrastructure.DataStreamServiceRequest
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataCollectionType
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataSource
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class WithingsCarpTransformerITest {
    private val carpSerializer = createDefaultJSON(COMMON_SERIAL_MODULE)
    private val transformer = WithingsCarpTransformerI()

    @Test
    fun transformsWithingsActivitiesPayloadIntoCarpDataStream() {
        val rawJson =
            """
            {
              "body": {
                "activities": [
                  {
                    "date": "2021-10-28",
                    "steps": 1962,
                    "timezone": "Europe/Copenhagen"
                  }
                ]
              },
              "status": 0
            }
            """.trimIndent()
        val rawNode = ConfiguredObjectMapper.instance.readTree(rawJson)
        val collectedAt = Instant.parse("2021-10-28T10:15:30Z")
        val studyDeploymentId = "a866a1f2-953e-45bb-80ab-18fb1a380bb9"
        val thirdPartyData =
            ThirdPartyData(
                userId = "internal-user",
                dataSourceId = WithingsDataSource.DATA_SOURCE_ID,
                dataIdentifier = WithingsDataCollectionType.DAILY_ACTIVITY,
                rawResponse = rawNode,
                applicationData = studyDeploymentId,
                collectedAt = collectedAt,
            )

        val carpPoints = transformer.transformActivities(thirdPartyData)

        assertEquals(1, carpPoints.size)
        val node = carpPoints.first() as JsonNode
        val request =
            carpSerializer.decodeFromString(DataStreamServiceRequest.AppendToDataStreams.serializer(), node.toString())

        assertEquals(1, request.apiVersion.major)
        assertEquals(1, request.apiVersion.minor)
        val sequence = request.batch.sequences.toList().single()

        assertEquals(studyDeploymentId, request.studyDeploymentId.stringRepresentation)
        assertEquals("Patient's wearables", sequence.dataStream.deviceRoleName)
        assertEquals("com.withings.daily_activity", sequence.dataStream.dataType.toString())
        assertEquals(listOf(0), sequence.triggerIds)
        assertEquals(0L, sequence.firstSequenceId)

        val measurement = sequence.measurements.single()
        val expectedSensorStartTime = collectedAt.epochSecond * 1_000_000 + collectedAt.nano / 1_000
        assertEquals(expectedSensorStartTime, measurement.sensorStartTime)
        assertEquals("com.withings.daily_activity", measurement.dataType.toString())

        val customData = measurement.data as CustomData
        val measurementData = ConfiguredObjectMapper.instance.readTree(customData.jsonSource)
        assertEquals("com.withings.daily_activity", measurementData.get("__type").textValue())
        assertTrue(measurementData.has("body"))
        val activities = measurementData.path("body").path("activities")
        assertTrue(activities.isArray)
        assertFalse(activities.isEmpty)
        val firstActivity = activities.get(0)
        assertEquals(1962, firstActivity.get("steps").intValue())
    }
}
