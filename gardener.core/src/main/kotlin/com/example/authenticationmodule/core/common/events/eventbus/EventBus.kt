package com.example.authenticationmodule.core.common.events.eventbus

import kotlin.reflect.KClass

abstract class EventBus : IEventBus {

  /**
   * Represents a subscription.
   */
  protected data class Handler(
    val eventSource: KClass<*>,
    val handler: (IntegrationEvent) -> Unit,
    val dataSourceId: String? = null
  )

  /**
   * Contains a list of the registered [Handler]s for [IntegrationEvent]s.
   */
  protected val subscribers: MutableMap<KClass<out IntegrationEvent>, MutableList<Handler>> = mutableMapOf()

  /**
   * Subscribe to an [IntegrationEvent].
   * This is not data source specific, every handler is executed.
   *
   * @param subscriber Who subscribes to the event.
   * @param eventType What type of event the subscription is for.
   * @param handler The callback function to be executed upon an event.
   */
  override fun subscribe(
    subscriber: KClass<*>,
    eventType: KClass<out IntegrationEvent>,
    handler: (IntegrationEvent) -> Unit
  ) {
    val eventHandler = Handler(subscriber, handler)
    registerHandler(eventType, eventHandler)
  }

  /**
   * Subscribe to a [DataSourceEvent].
   * Only those [handler]s are executed that are subscribed to a
   * specific data source.
   *
   * @param subscriber Who subscribes to the event.
   * @param eventType What type of event the subscription is for.
   * @param dataSourceId ID of the data source the event is fired from.
   * @param handler The callback function to be executed upon an event.
   */
  @Suppress("UNCHECKED_CAST")
  override fun subscribe(
    subscriber: KClass<*>,
    eventType: KClass<out DataSourceEvent>,
    dataSourceId: String,
    handler: (DataSourceEvent) -> Unit
  ) {
    val eventHandler = Handler(subscriber, handler as (IntegrationEvent) -> Unit, dataSourceId)
    registerHandler(eventType, eventHandler)
  }

  /**
   * Adds the specified [handler] to the [eventType].
   */
  private fun registerHandler(eventType: KClass<out IntegrationEvent>, handler: Handler) {
    if (subscribers.containsKey(eventType)) {
      val handlerList = subscribers[eventType]!!
      handlerList.add(handler)
    } else {
      subscribers[eventType] = mutableListOf(handler)
    }
  }

}
