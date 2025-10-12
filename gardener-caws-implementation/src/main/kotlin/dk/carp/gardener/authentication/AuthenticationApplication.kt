package dk.carp.gardener.authentication

import dk.carp.gardener.authentication.verticles.MainVerticle
import io.vertx.core.Vertx
import io.vertx.core.logging.SLF4JLogDelegateFactory

class AuthenticationApplication

/**
 * The Application's entry point.
 * It starts the [MainVerticle].
 */
fun main() {
    System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory::class.java.name)

    val vertx = Vertx.vertx()
    vertx.deployVerticle(MainVerticle())
}
