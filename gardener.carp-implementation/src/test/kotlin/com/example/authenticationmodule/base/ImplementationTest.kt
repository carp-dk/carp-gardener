package com.example.authenticationmodule.base

import com.example.authenticationmodule.verticles.MainVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith


/**
 * Common parent fort Vert.x integration tests.
 */
@ExtendWith(VertxExtension::class)
abstract class ImplementationTest {

  protected lateinit var mainVerticle: MainVerticle

  /**
   * Deploys the [MainVerticle] and execute the test methods when the verticle
   * is successfully deployed.
   */
  @BeforeEach
  fun deployMainVerticle(vertx: Vertx, testContext: VertxTestContext) {
    mainVerticle = MainVerticle()
    vertx.deployVerticle(mainVerticle, testContext.succeedingThenComplete())
  }

  /**
   * Drops the collections after every test case execution.
   */
  @AfterEach
  fun cleanUp(testContext: VertxTestContext) {
    val mongoClient = mainVerticle.mongoClient
    val dropParamsFuture = mongoClient.dropCollection("access_params")
    val dropStateFuture = mongoClient.dropCollection("authorization_states")
    val composite = CompositeFuture.all(dropParamsFuture, dropStateFuture)
    composite
      .onSuccess {
        testContext.completeNow()
    }
      .onFailure { msg ->
        testContext.failNow(msg.message)
    }
  }

}
