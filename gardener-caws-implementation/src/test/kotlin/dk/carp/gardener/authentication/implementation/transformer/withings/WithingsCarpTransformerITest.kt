package dk.carp.gardener.authentication.implementation.transformer.withings

import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataCollectionType
import dk.carp.gardener.authentication.core.authorization.devices.withings.WithingsDataSource
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import dk.carp.gardener.authentication.implementation.transformer.carp.CarpDataPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WithingsCarpTransformerITest {
    private val transformer = WithingsCarpTransformerI()

    @Test
    fun transformsWithingsActivitiesPayloadIntoCarpDataPoint() {
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
        val thirdPartyData =
            ThirdPartyData(
                userId = "internal-user",
                dataSourceId = WithingsDataSource.DATA_SOURCE_ID,
                dataIdentifier = WithingsDataCollectionType.DAILY_ACTIVITY,
                rawResponse = rawNode,
                applicationData = "study-123",
            )

        val carpPoints = transformer.transformActivities(thirdPartyData)

        assertEquals(1, carpPoints.size)
        val point = carpPoints.first() as CarpDataPoint
        val header = requireNotNull(point.carpHeader)
        assertEquals("study-123", header.studyId)
        assertEquals("internal-user", header.userId)
        val format = requireNotNull(header.dataFormat)
        assertEquals("com.withings", format.namespace)
        assertEquals("daily_activity", format.name)
        assertEquals("Unknown", header.triggerId)
        assertEquals("Patient's wearables", header.deviceRoleName)

        val body = requireNotNull(point.carpBody)
        assertTrue(body.has("body"))
        val activities = requireNotNull(body.get("body").get("activities"))
        assertTrue(activities.isArray)
        val firstActivity = requireNotNull(activities.get(0))
        assertEquals(1962, firstActivity.get("steps").intValue())
    }
}
