package com.example.authenticationmodule.core.authorization.devices.fitbit

import com.example.authenticationmodule.core.collection.data.ThirdPartyData
import com.example.authenticationmodule.core.common.datatype.DataCollectionType
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformer

/**
 * Supported Fitbit [DataCollectionType]s.
 */
enum class FitbitDataCollectionType(
    private val id: String,
    private val ep: String,
    private val cn: String,
    private val ns: String,
    val version: String = "1",
) : DataCollectionType {

    ACTIVITIES(
        "activities",
        "activities",
        "Fitbit Activity Summary",
        "com.fitbit.activities"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as FitbitDataTypeTransformer
            return transformer.transformActivities(data)
        }
    },
    HEART_RATE(
        "activities",
        "activities/heart",
        "Fitbit Heart rate",
        "com.fitbit.heart_rate"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as FitbitDataTypeTransformer
            return transformer.transformHeartRate(data)
        }
    },
    BODY(
        "body",
        "body/log/weight",
        "Fitbit Body Weight",
        "com.fitbit.body"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as FitbitDataTypeTransformer
            return transformer.transformBody(data)
        }
    },
    SLEEP(
        "sleep",
        "sleep",
        "Fitbit Sleep logs",
        "com.fitbit.sleep",
        "1.2") {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as FitbitDataTypeTransformer
            return transformer.transformSleep(data)
        }
    },
    FOOD(
        "foods",
        "foods/log",
        "Fitbit Food logs",
        "com.fitbit.food"
    ) {
        override fun acceptTransformer(transformer: IDataTypeTransformer, data: ThirdPartyData): List<Any> {
            transformer as FitbitDataTypeTransformer
            return transformer.transformFood(data)
        }
    };

    companion object {
        fun from(type: String?): DataCollectionType? = values().find { it.id == type }
    }

    override fun getIdentifier() = id
    override fun getEndpoint() = ep
    override fun getCustomName() = cn
    override fun getNamespace(): String = ns
}