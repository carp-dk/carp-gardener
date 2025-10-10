package dk.carp.gardener.authentication.core.common.events.eventbus

import kotlin.reflect.KClass

/**
 * A message bus with a publishing/subscribe mechanism to distribute integration events across application services.
 */
interface IEventBus {

    /**
     * Subscribe to an [IntegrationEvent].
     * This is not data source specific, every handler is executed.
     *
     * @param subscriber Who subscribes to the event.
     * @param eventType What type of event the subscription is for.
     * @param handler The callback function to be executed upon an event.
     *
     * @throws IllegalArgumentException When trying to register a handler that is already registered.
     */
    fun subscribe(subscriber: KClass<*>, eventType: KClass<out IntegrationEvent>, handler: (IntegrationEvent) -> Unit)

    /**
     * Subscribe to a [DataSourceEvent].
     * Only those [handler]s are executed that are subscribed to a
     * specific data source.
     *
     * @param subscriber Who subscribes to the event.
     * @param eventType What type of event the subscription is for.
     * @param dataSourceId ID of the data source the event is fired from.
     * @param handler The callback function to be executed upon an event.
     *
     * @throws IllegalArgumentException When trying to register a handler that is already registered.
     */
    fun subscribe(subscriber: KClass<*>, eventType: KClass<out DataSourceEvent>, dataSourceId: String, handler: (DataSourceEvent) -> Unit)

    /**
     * Publish the specified [event].
     *
     * @param eventSource Who fires the event.
     * @param event The actual event that is being fired.
     */
    fun publish(eventSource: KClass<*>, event: IntegrationEvent)

}
