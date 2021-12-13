package com.example.authenticationmodule.base

import com.example.authenticationmodule.core.authorization.authorizationstate.IAuthorizationStateRepository
import com.example.authenticationmodule.core.authorization.authorizationstate.AuthorizationStateServiceHost
import com.example.authenticationmodule.core.authorization.authorizationstate.IAuthorizationStateService
import com.example.authenticationmodule.core.authorization.datasourceregistry.DataSourceRegistryHost
import com.example.authenticationmodule.core.authorization.datasourceregistry.IDataSourceRegistry
import com.example.authenticationmodule.core.collection.publisher.IDataPublisher
import com.example.authenticationmodule.core.common.accessparams.IAccessParamsRepository
import com.example.authenticationmodule.core.common.accessparams.AccessParamsServiceHost
import com.example.authenticationmodule.core.common.accessparams.IAccessParamsService
import com.example.authenticationmodule.core.common.events.eventbus.IEventBus
import com.example.authenticationmodule.core.common.transformer.DataTypeTransformerRegistryHost
import com.example.authenticationmodule.core.common.transformer.IDataTypeTransformerRegistry
import com.example.authenticationmodule.core.infrastructure.eventbus.SingleThreadedEventBus
import com.example.authenticationmodule.core.infrastructure.publisher.InMemoryDataPublisher
import com.example.authenticationmodule.core.infrastructure.repository.InMemoryAccessParamsRepository
import com.example.authenticationmodule.core.infrastructure.repository.InMemoryAuthorizationStateRepository
import org.mockito.Mockito.spy
import kotlin.test.BeforeTest

/**
 * A common parent for every core related test classes.
 * Sets up the application services.
 */
abstract class CoreTest {

    protected val cleanEventBus: IEventBus = SingleThreadedEventBus()
    protected var spyingEventBus: IEventBus = spy(cleanEventBus)
    protected var authorizationStateRepository: IAuthorizationStateRepository = InMemoryAuthorizationStateRepository()
    protected val authorizationStateService: IAuthorizationStateService = AuthorizationStateServiceHost(authorizationStateRepository)
    protected var accessParamsRepository: IAccessParamsRepository = InMemoryAccessParamsRepository()
    protected val accessParamService : IAccessParamsService = AccessParamsServiceHost(accessParamsRepository)
    protected var dataSourceRegistry: IDataSourceRegistry = DataSourceRegistryHost(spyingEventBus)
    protected val transformerRegistry: IDataTypeTransformerRegistry = DataTypeTransformerRegistryHost()
    protected val cleanPublisher: IDataPublisher = InMemoryDataPublisher()
    protected val spyingPublisher: IDataPublisher = spy(cleanPublisher)

    /**
     * Cleans the repositories before every test run.
     */
    @BeforeTest
    fun clean() {
        authorizationStateRepository = InMemoryAuthorizationStateRepository()
        accessParamsRepository = InMemoryAccessParamsRepository()
        dataSourceRegistry = DataSourceRegistryHost(spyingEventBus)
    }

}