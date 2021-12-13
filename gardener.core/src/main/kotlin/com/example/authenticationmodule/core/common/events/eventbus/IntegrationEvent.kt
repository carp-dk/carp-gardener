package com.example.authenticationmodule.core.common.events.eventbus

import java.time.Instant
import java.util.*

/**
 * An event raised by the application.
 */
abstract class IntegrationEvent {

  val id: String = UUID.randomUUID().toString()

  val createdAt: Instant = Instant.now()

}
