package cn.haidiiii.docker.config;

import cn.haidiiii.docker.DockerConfiguration;
import com.haidiiii.docker.DockerConfigProperties;
import com.haidiiii.docker.DockerProperties;
import com.github.dockerjava.api.DockerClient;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.*;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fht
 * @since 2022-07-01 8:33
 */
public class DockerConfigDataLocationResolver implements ConfigDataLocationResolver<DockerConfigDataResource> {

    /**
     * docker ConfigData prefix.
     */
    public static final String PREFIX = "docker:";

    protected static final List<String> FILES_SUFFIXES = Collections.unmodifiableList(Arrays.asList(".yml", ".yaml", ".properties"));

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        if (!location.hasPrefix(PREFIX)) {
            return false;
        }
        // only bind if correct prefix
        boolean enabled = context.getBinder().bind(DockerProperties.PREFIX + ".enabled", Boolean.class).orElse(true);
        boolean configEnabled = context.getBinder().bind(DockerConfigProperties.PREFIX + ".enabled", Boolean.class)
            .orElse(true);
        return configEnabled && enabled;
    }

    @Override
    public List<DockerConfigDataResource> resolve(ConfigDataLocationResolverContext context,
                                                  ConfigDataLocation location) throws ConfigDataLocationNotFoundException {
        return Collections.emptyList();
    }

    @Override
    public List<DockerConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext resolverContext,
                                                                 ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {
        UriComponents locationUri = parseLocation(resolverContext, location);

        // create docker client
        registerBean(resolverContext, DockerProperties.class, loadProperties(resolverContext, locationUri));

        registerAndPromoteBean(resolverContext, DockerClient.class, this::createDockerClient);

        // create locations
        DockerConfigProperties properties = loadConfigProperties(resolverContext);

        DockerPropertySources dockerPropertySources = new DockerPropertySources(properties);

        List<DockerPropertySources.Context> contexts = (locationUri == null || CollectionUtils.isEmpty(locationUri.getPathSegments()))
            ? dockerPropertySources.generateAutomaticContexts(profiles.getAccepted(), false)
            : getCustomContexts(locationUri, properties);

        registerAndPromoteBean(resolverContext, DockerConfigProperties.class, BootstrapRegistry.InstanceSupplier.of(properties));

        return contexts
            .stream().map(propertySourceContext -> new DockerConfigDataResource(propertySourceContext.getPath(),
                properties, dockerPropertySources, propertySourceContext.getProfile()))
            .collect(Collectors.toList());
    }

    private BindHandler getBindHandler(ConfigDataLocationResolverContext context) {
        return context.getBootstrapContext().getOrElse(BindHandler.class, null);
    }

    private List<DockerPropertySources.Context> getCustomContexts(UriComponents uriComponents, DockerConfigProperties properties) {
        if (!StringUtils.hasText(uriComponents.getPath())) {
            return Collections.emptyList();
        }

        List<DockerPropertySources.Context> contexts = new ArrayList<>();
        for (String path : uriComponents.getPath().split(";")) {
            for (String suffix : getSuffixes(properties)) {
                contexts.add(new DockerPropertySources.Context(path + suffix));
            }
        }

        return contexts;
    }

    protected List<String> getSuffixes(DockerConfigProperties properties) {
        return FILES_SUFFIXES;
    }

    @Nullable
    protected UriComponents parseLocation(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        String originalLocation = location.getNonPrefixedValue(PREFIX);
        if (!StringUtils.hasText(originalLocation)) {
            return null;
        }
        String uri;
        if (!originalLocation.startsWith("//")) {
            uri = PREFIX + "//" + originalLocation;
        } else {
            uri = originalLocation;
        }
        return UriComponentsBuilder.fromUriString(uri).build();
    }

    protected <T> void registerAndPromoteBean(ConfigDataLocationResolverContext context, Class<T> type,
                                              BootstrapRegistry.InstanceSupplier<T> supplier) {
        registerBean(context, type, supplier);
        context.getBootstrapContext().addCloseListener(event -> {
            T instance = event.getBootstrapContext().get(type);
            String name = "configData" + type.getSimpleName();
            ConfigurableApplicationContext appCtxt = event.getApplicationContext();
            if (!appCtxt.containsBean(name)) {
                appCtxt.getBeanFactory().registerSingleton(name, instance);
            }
        });
    }

    public <T> void registerBean(ConfigDataLocationResolverContext context, Class<T> type, T instance) {
        context.getBootstrapContext().registerIfAbsent(type, BootstrapRegistry.InstanceSupplier.of(instance));
    }

    protected <T> void registerBean(ConfigDataLocationResolverContext context, Class<T> type,
                                    BootstrapRegistry.InstanceSupplier<T> supplier) {
        ConfigurableBootstrapContext bootstrapContext = context.getBootstrapContext();
        bootstrapContext.registerIfAbsent(type, supplier);
    }

    protected DockerClient createDockerClient(BootstrapContext context) {
        DockerProperties properties = context.get(DockerProperties.class);

        return DockerConfiguration.createDockerClient(properties);
    }

    protected DockerProperties loadProperties(ConfigDataLocationResolverContext resolverContext,
                                              UriComponents location) {
        Binder binder = resolverContext.getBinder();
        DockerProperties dockerProperties = binder
            .bind(DockerProperties.PREFIX, Bindable.of(DockerProperties.class), getBindHandler(resolverContext))
            .orElseGet(DockerProperties::new);

        if (location != null) {
            if (StringUtils.hasText(location.getHost()) && location.getPort() >= 0) {
                dockerProperties.setInternalHost("tcp://" + location.getHost() + ":" + location.getPort());
            }
        }

        return dockerProperties;
    }

    protected DockerConfigProperties loadConfigProperties(ConfigDataLocationResolverContext resolverContext) {
        Binder binder = resolverContext.getBinder();
        BindHandler bindHandler = getBindHandler(resolverContext);
        DockerConfigProperties properties = binder
            .bind(DockerConfigProperties.PREFIX, Bindable.of(DockerConfigProperties.class), bindHandler)
            .orElseGet(DockerConfigProperties::new);

        if (StringUtils.isEmpty(properties.getName())) {
            properties.setName(binder.bind("spring.application.name", String.class).orElse("application"));
        }
        return properties;
    }

}
