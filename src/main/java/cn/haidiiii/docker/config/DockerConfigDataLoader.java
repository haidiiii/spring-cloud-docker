package cn.haidiiii.docker.config;

import com.github.dockerjava.api.DockerClient;
import org.apache.commons.logging.Log;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * @author fht
 * @since 2022-07-01 10:01
 */
public class DockerConfigDataLoader implements ConfigDataLoader<DockerConfigDataResource> {

    private static final EnumSet<ConfigData.Option> ALL_OPTIONS = EnumSet.allOf(ConfigData.Option.class);

    private final Log log;

    public DockerConfigDataLoader(Log log) {
        this.log = log;
    }

    @Override
    public ConfigData load(ConfigDataLoaderContext context, DockerConfigDataResource resource) {
        if (context.getBootstrapContext().isRegistered(DockerBootstrapper.LoaderInterceptor.class)) {
            DockerBootstrapper.LoaderInterceptor interceptor = context.getBootstrapContext().get(DockerBootstrapper.LoaderInterceptor.class);
            if (interceptor != null) {
                Binder binder = context.getBootstrapContext().get(Binder.class);
                return interceptor.apply(new DockerBootstrapper.LoadContext(context, resource, binder, this::doLoad));
            }
        }
        return doLoad(context, resource);
    }

    public ConfigData doLoad(ConfigDataLoaderContext context, DockerConfigDataResource resource) {
        try {
            DockerClient dockerClient = getBean(context, DockerClient.class);

            DockerPropertySource propertySource = resource.getDockerPropertySources()
                .createPropertySource(resource.getContext(), dockerClient);
            if (propertySource == null) {
                return null;
            }
            List<DockerPropertySource> propertySources = Collections.singletonList(propertySource);
            if (ALL_OPTIONS.size() == 1) {
                // boot 2.4.2 and prior
                return new ConfigData(propertySources);
            }
            else if (ALL_OPTIONS.size() == 2) {
                // boot 2.4.3 and 2.4.4
                return new ConfigData(propertySources, ConfigData.Option.IGNORE_IMPORTS, ConfigData.Option.IGNORE_PROFILES);
            }
            else if (ALL_OPTIONS.size() > 2) {
                // boot 2.4.5+
                return new ConfigData(propertySources, source -> {
                    List<ConfigData.Option> options = new ArrayList<>();
                    options.add(ConfigData.Option.IGNORE_IMPORTS);
                    options.add(ConfigData.Option.IGNORE_PROFILES);
                    if (StringUtils.hasText(resource.getProfile())) {
                        options.add(ConfigData.Option.PROFILE_SPECIFIC);
                    }
                    return ConfigData.Options.of(options.toArray(new ConfigData.Option[0]));
                });
            }
        }
        catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error getting properties from docker: " + resource, e);
            }
            throw new ConfigDataResourceNotFoundException(resource, e);
        }
        return null;
    }

    protected <T> T getBean(ConfigDataLoaderContext context, Class<T> type) {
        if (context.getBootstrapContext().isRegistered(type)) {
            return context.getBootstrapContext().get(type);
        }
        return null;
    }

}
