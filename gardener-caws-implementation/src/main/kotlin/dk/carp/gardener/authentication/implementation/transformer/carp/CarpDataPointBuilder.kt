package dk.carp.gardener.authentication.implementation.transformer.carp

import dk.carp.gardener.authentication.core.collection.data.ThirdPartyData

/**
 * Transforms a [ThirdPartyData] into a [CarpDataPoint].
 */
class CarpDataPointBuilder private constructor() {

    companion object {

        fun fromThirdPartyData(thirdPartyData: ThirdPartyData): List<CarpDataPoint> {
            if (thirdPartyData.rawResponse.isArray) {
                val result: MutableList<CarpDataPoint> = mutableListOf()
                thirdPartyData.rawResponse.forEach { data ->
                    result.add(
                        CarpDataPoint(
                            getCarpHeaderFor(thirdPartyData),
                            data
                        )
                    )
                }
                return result
            }
            else return listOf(
                CarpDataPoint(
                    getCarpHeaderFor(thirdPartyData),
                    thirdPartyData.rawResponse
                )
            )
        }

        private fun getCarpHeaderFor(data: ThirdPartyData): CarpDataPointHeader {
            val namespace = data.dataIdentifier.getNamespace()
            val carpNamespace = namespace.substring(0, namespace.lastIndexOf("."))
            val carpName = namespace.substring(namespace.lastIndexOf(".") + 1)

            val format = CarpDataPointFormat(carpNamespace, carpName)
            return CarpDataPointHeader(
                studyId = data.applicationData,
                userId = data.userId,
                dataFormat = format,
                triggerId = "Unknown",
                deviceRoleName = "Patient's wearables"
            )
        }

    }

}