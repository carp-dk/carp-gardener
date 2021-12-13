package com.example.authenticationmodule.core.common.events.eventbus

/**
 * An [IntegrationEvent] connected to a specific data source
 */
abstract class DataSourceEvent : IntegrationEvent() {

    abstract val dataSourceId: String

}