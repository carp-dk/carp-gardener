package dk.carp.gardener.authentication.implementation.transformer.carp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import dk.cachet.carp.common.application.NamespacedId
import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.application.data.Data
import dk.cachet.carp.common.infrastructure.serialization.COMMON_SERIAL_MODULE
import dk.cachet.carp.common.infrastructure.serialization.DataSerializer
import dk.cachet.carp.common.infrastructure.serialization.createDefaultJSON
import dk.cachet.carp.data.application.DataStreamId
import dk.cachet.carp.data.application.Measurement
import dk.cachet.carp.data.application.MutableDataStreamBatch
import dk.cachet.carp.data.application.MutableDataStreamSequence
import dk.cachet.carp.data.infrastructure.DataStreamServiceRequest
import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData
import dk.carp.gardener.authentication.core.common.util.serializer.ConfiguredObjectMapper
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Transforms [ThirdPartyData] into CARP compliant data stream append requests.
 */
class CarpDataStreamBuilder private constructor() {
    companion object {
        private const val DEFAULT_DEVICE_ROLE_NAME = "Wearables"
        private const val DEFAULT_FIRST_SEQUENCE_ID = 0L
        private const val DEFAULT_TRIGGER_ID = 0

        private val carpSerializer: Json = createDefaultJSON(COMMON_SERIAL_MODULE)

        fun fromThirdPartyData(thirdPartyData: ThirdPartyData): List<JsonNode> {
            val payloads = collectPayloads(thirdPartyData.rawResponse)
            if (payloads.isEmpty()) return emptyList()

            val request = buildRequest(thirdPartyData, payloads)
            return listOf(serializeRequest(request))
        }

        private fun collectPayloads(root: JsonNode): List<JsonNode> =
            when (root) {
                is ArrayNode -> root.toList()
                else -> listOf(root)
            }

        private fun buildRequest(
            thirdPartyData: ThirdPartyData,
            payloads: List<JsonNode>,
        ): DataStreamServiceRequest.AppendToDataStreams {
            val applicationContext = parseApplicationContext(thirdPartyData.applicationData)
            val studyDeploymentId = applicationContext.deploymentId.let(::UUID)
            val dataTypeValue = thirdPartyData.dataIdentifier.getNamespace()
            val dataType = parseNamespacedId(dataTypeValue)

            val dataStreamId =
                DataStreamId(
                    studyDeploymentId = studyDeploymentId,
                    deviceRoleName = applicationContext.deviceRoleName ?: DEFAULT_DEVICE_ROLE_NAME,
                    dataType = dataType,
                )
            val sequence =
                MutableDataStreamSequence<Data>(
                    dataStream = dataStreamId,
                    firstSequenceId = DEFAULT_FIRST_SEQUENCE_ID,
                    triggerIds = listOf(DEFAULT_TRIGGER_ID),
                )

            payloads.forEachIndexed { index, payload ->
                sequence.appendMeasurements(
                    buildMeasurement(
                        thirdPartyData = thirdPartyData,
                        payload = payload,
                        dataType = dataType,
                        dataTypeValue = dataTypeValue,
                        sequenceOffset = index,
                    ),
                )
            }

            val batch = MutableDataStreamBatch()
            batch.appendSequence(sequence)

            return DataStreamServiceRequest.AppendToDataStreams(
                studyDeploymentId = studyDeploymentId,
                batch = batch,
            )
        }

        private fun buildMeasurement(
            thirdPartyData: ThirdPartyData,
            payload: JsonNode,
            dataType: NamespacedId,
            dataTypeValue: String,
            sequenceOffset: Int,
        ): Measurement<Data> {
            val measurementNode = buildMeasurementData(payload, dataTypeValue)
            val measurementJson = ConfiguredObjectMapper.instance.writeValueAsString(measurementNode)
            val measurementData = carpSerializer.decodeFromString(DataSerializer, measurementJson)

            return Measurement(
                sensorStartTime = toSensorStartTime(thirdPartyData.collectedAt, sequenceOffset.toLong()),
                sensorEndTime = null,
                dataType = dataType,
                data = measurementData,
            )
        }

        private fun buildMeasurementData(payload: JsonNode, dataType: String): JsonNode {
            val mapper = ConfiguredObjectMapper.instance
            val dataNode: ObjectNode =
                if (payload.isObject) {
                    payload.deepCopy<ObjectNode>()
                } else {
                    mapper.createObjectNode().apply {
                        set<JsonNode>("value", payload)
                    }
                }
            dataNode.put("__type", dataType)
            return dataNode
        }

        private fun serializeRequest(request: DataStreamServiceRequest.AppendToDataStreams): JsonNode =
            ConfiguredObjectMapper.instance.readTree(
                carpSerializer.encodeToString(DataStreamServiceRequest.AppendToDataStreams.serializer(), request),
            )

        private fun toSensorStartTime(collectedAt: Instant, sequenceOffset: Long): Long =
            collectedAt.epochSecond * 1_000_000 + collectedAt.nano / 1_000 + sequenceOffset

        private fun parseNamespacedId(identifier: String): NamespacedId {
            val delimiterIndex = identifier.lastIndexOf('.')
            require(delimiterIndex > 0) { "Data type must contain a namespace and name separated by '.'" }
            val namespace = identifier.substring(0, delimiterIndex)
            val name = identifier.substring(delimiterIndex + 1)
            return NamespacedId(namespace, name)
        }

        private fun parseApplicationContext(rawApplicationData: String?): ApplicationContext {
            require(!rawApplicationData.isNullOrBlank()) {
                "Missing study deployment id in application data."
            }

            val mapper = ConfiguredObjectMapper.instance
            val parsedNode =
                runCatching { mapper.readTree(rawApplicationData) }
                    .getOrNull()

            if (parsedNode != null && parsedNode.hasNonNull("deploymentId")) {
                val deploymentId = parsedNode.get("deploymentId").asText()
                val deviceRoleName = parsedNode.get("deviceRoleName")?.asText()
                return ApplicationContext(deploymentId, deviceRoleName)
            }

            return ApplicationContext(rawApplicationData, null)
        }

        private data class ApplicationContext(
            val deploymentId: String,
            val deviceRoleName: String?,
        )
    }
}
