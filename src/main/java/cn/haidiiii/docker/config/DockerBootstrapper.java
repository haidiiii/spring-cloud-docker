package cn.haidiiii.docker.config;

import com.haidiiii.docker.DockerProperties;
import com.github.dockerjava.api.DockerClient;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.util.Assert;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author fht
 * @since 2022-07-01 10:03
 */
public class DockerBootstrapper implements BootstrapRegistryInitializer {

    private Function<BootstrapContext, DockerClient> dockerClientFactory;

    private LoaderInterceptor loaderInterceptor;

    static BootstrapRegistryInitializer fromDockerProperties(Function<DockerProperties, DockerClient> factory) {
        return registry -> registry.register(DockerClient.class, context -> {
            DockerProperties properties = context.get(DockerProperties.class);
            return factory.apply(properties);
        });
    }

    static BootstrapRegistryInitializer fromBootstrapContext(Function<BootstrapContext, DockerClient> factory) {
        return registry -> registry.register(DockerClient.class, factory::apply);
    }

    static DockerBootstrapper create() {
        return new DockerBootstrapper();
    }

    // TODO: document there will be a DockerProperties in BootstrapContext
    public DockerBootstrapper withDockerClientFactory(Function<BootstrapContext, DockerClient> dockerClientFactory) {
        this.dockerClientFactory = dockerClientFactory;
        return this;
    }

    public DockerBootstrapper withLoaderInterceptor(LoaderInterceptor loaderInterceptor) {
        this.loaderInterceptor = loaderInterceptor;
        return this;
    }

    @Override
    public void initialize(BootstrapRegistry registry) {
        if (dockerClientFactory != null) {
            registry.register(DockerClient.class, dockerClientFactory::apply);
        }
        if (loaderInterceptor != null) {
            registry.register(LoaderInterceptor.class, BootstrapRegistry.InstanceSupplier.of(loaderInterceptor));
        }
    }

    public interface LoaderInterceptor extends Function<LoadContext, ConfigData> {
    }

    @FunctionalInterface
    public interface LoaderInvocation extends BiFunction<ConfigDataLoaderContext, DockerConfigDataResource, ConfigData> {
    }

    public static class LoadContext {

        private final ConfigDataLoaderContext loaderContext;

        private final DockerConfigDataResource resource;

        private final Binder binder;

        private final LoaderInvocation invocation;

        LoadContext(ConfigDataLoaderContext loaderContext, DockerConfigDataResource resource, Binder binder, LoaderInvocation invocation) {
            Assert.notNull(loaderContext, "loaderContext may not be null");
            Assert.notNull(resource, "resource may not be null");
            Assert.notNull(binder, "binder may not be null");
            Assert.notNull(invocation, "invocation may not be null");
            this.loaderContext = loaderContext;
            this.resource = resource;
            this.binder = binder;
            this.invocation = invocation;
        }

        public ConfigDataLoaderContext getLoaderContext() {
            return this.loaderContext;
        }

        public DockerConfigDataResource getResource() {
            return this.resource;
        }

        public Binder getBinder() {
            return this.binder;
        }

        public LoaderInvocation getInvocation() {
            return this.invocation;
        }

    }

}
