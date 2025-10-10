package dk.carp.gardener.authentication.base

import dk.carp.gardener.authentication.core.collection.publisher.IDataPublisher
import dk.carp.gardener.authentication.core.common.accessparams.AccessParamsServiceHost
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsRepository
import dk.carp.gardener.authentication.core.common.accessparams.IAccessParamsService
import dk.carp.gardener.authentication.core.common.events.eventbus.IEventBus
import dk.carp.gardener.authentication.core.common.transformer.DataTypeTransformerRegistryHost
import dk.carp.gardener.authentication.core.common.transformer.IDataTypeTransformerRegistry
import dk.carp.gardener.authentication.core.infrastructure.eventbus.SingleThreadedEventBus
import dk.carp.gardener.authentication.core.infrastructure.publisher.InMemoryDataPublisher
import dk.carp.gardener.authentication.core.infrastructure.repository.InMemoryAccessParamsRepository
import dk.carp.gardener.authentication.core.infrastructure.repository.InMemoryAuthorizationStateRepository
import org.mockito.Mockito.spy
import kotlin.test.BeforeTest

/**
 * A common parent for every core related test classes.
 * Sets up the application services.
 */
abstract class CoreTest {

    protected val cleanEventBus: IEventBus = SingleThreadedEventBus()
    protected var spyingEventBus: IEventBus = spy(cleanEventBus)
    protected var authorizationStateRepository: dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateRepository = InMemoryAuthorizationStateRepository()
    protected val authorizationStateService: dk.carp.gardener.authentication.core.authorization.authorizationstate.IAuthorizationStateService =
        dk.carp.gardener.authentication.core.authorization.authorizationstate.AuthorizationStateServiceHost(
            authorizationStateRepository
        )
    protected var accessParamsRepository: IAccessParamsRepository = InMemoryAccessParamsRepository()
    protected val accessParamService : IAccessParamsService = AccessParamsServiceHost(accessParamsRepository)
    protected var dataSourceRegistry: dk.carp.gardener.authentication.core.authorization.datasourceregistry.IDataSourceRegistry =
        dk.carp.gardener.authentication.core.authorization.datasourceregistry.DataSourceRegistryHost(spyingEventBus)
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
        dataSourceRegistry =
            dk.carp.gardener.authentication.core.authorization.datasourceregistry.DataSourceRegistryHost(spyingEventBus)
    }

}