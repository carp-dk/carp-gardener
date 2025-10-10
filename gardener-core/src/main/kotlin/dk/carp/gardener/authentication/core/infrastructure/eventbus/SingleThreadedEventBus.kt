package dk.carp.gardener.authentication.core.infrastructure.eventbus

import dk.carp.gardener.authentication.core.common.events.eventbus.DataSourceEvent
import dk.carp.gardener.authentication.core.common.events.eventbus.EventBus
import dk.carp.gardener.authentication.core.common.events.eventbus.IntegrationEvent
import kotlin.reflect.KClass

/**
 * A simple [EventBus] implementation for testing purposes which publishes and subscribes to events on the same thread.
 *
 * The class is open due to mocking purposes during testing.
 */
open class SingleThreadedEventBus : EventBus() {

  override fun publish(eventSource: KClass<*>, event: IntegrationEvent) {
    if (!subscribers.containsKey(event::class)) {
      return
    }
    val handlers: List<Handler> = if (event is DataSourceEvent) {
      subscribers[event::class]!!.filter { it.dataSourceId != null && it.dataSourceId == event.dataSourceId }
    } else {
      subscribers[event::class]!!
    }
    handlers.forEach {
      try {
        it.handler(event)
      } catch (ex: Exception) {
        println("Exception encountered while executing a handler: ${ex.message}")
      }
    }
  }

}
