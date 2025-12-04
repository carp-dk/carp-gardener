package dk.carp.gardener.authentication.implementation.transformer.carp

import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataCollectionType
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataSource
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class CarpDataStreamBuilderTest {
    private val mapper = ConfiguredObjectMapper.instance

    @Test
    fun `builds append request json for single payload`() {
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
        val rawNode = mapper.readTree(rawJson)
        val deploymentId = "a866a1f2-953e-45bb-80ab-18fb1a380bb9"
        val collectedAt = Instant.parse("2021-10-28T10:15:30Z")
        val thirdPartyData =
            ThirdPartyData(
                userId = "internal-user",
                dataSourceId = WithingsDataSource.DATA_SOURCE_ID,
                dataIdentifier = WithingsDataCollectionType.DAILY_ACTIVITY,
                rawResponse = rawNode,
                applicationData = deploymentId,
                collectedAt = collectedAt,
            )

        val actual = CarpDataStreamBuilder.fromThirdPartyData(thirdPartyData)

        val expectedJson =
            """
            {
              "apiVersion": "1.1",
              "studyDeploymentId": "$deploymentId",
              "batch": [
                {
                  "dataStream": {
                    "studyDeploymentId": "$deploymentId",
                    "deviceRoleName": "Patient's wearables",
                    "dataType": "com.withings.daily_activity"
                  },
                  "firstSequenceId": 0,
                  "measurements": [
                    {
                      "sensorStartTime": ${collectedAt.epochSecond * 1_000_000 + collectedAt.nano / 1_000},
                      "data": {
                        "body": {
                          "activities": [
                            {
                              "date": "2021-10-28",
                              "steps": 1962,
                              "timezone": "Europe/Copenhagen"
                            }
                          ]
                        },
                        "status": 0,
                        "__type": "com.withings.daily_activity"
                      }
                    }
                  ],
                  "triggerIds": [ 0 ]
                }
              ]
            }
            """.trimIndent()
        val expected = listOf(mapper.readTree(expectedJson))

        assertEquals(1, actual.size)
        assertIterableEquals(expected, actual)
    }

    @Test
    fun `uses device role name from application data when provided`() {
        val rawJson =
            """
            {
              "body": { "activities": [] },
              "status": 0
            }
            """.trimIndent()
        val rawNode = mapper.readTree(rawJson)
        val deploymentId = "a866a1f2-953e-45bb-80ab-18fb1a380bb9"
        val deviceRoleName = "Custom wearables"
        val applicationData =
            mapper.createObjectNode().apply {
                put("deploymentId", deploymentId)
                put("deviceRoleName", deviceRoleName)
            }.toString()
        val thirdPartyData =
            ThirdPartyData(
                userId = "internal-user",
                dataSourceId = WithingsDataSource.DATA_SOURCE_ID,
                dataIdentifier = WithingsDataCollectionType.DAILY_ACTIVITY,
                rawResponse = rawNode,
                applicationData = applicationData,
                collectedAt = Instant.EPOCH,
            )

        val actual = CarpDataStreamBuilder.fromThirdPartyData(thirdPartyData)
        val node = actual.single()
        val deviceRole = node.path("batch").first().path("dataStream").path("deviceRoleName").textValue()

        assertEquals(deviceRoleName, deviceRole)
    }
}
