package cn.haidiiii.docker.config;

import com.haidiiii.docker.DockerConfigProperties;
import com.github.dockerjava.api.DockerClient;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author fht
 * @since 2022-01-24 9:10
 */
public class DockerPropertySourceLocator implements PropertySourceLocator {

    private final DockerClient dockerClient;

    private final DockerConfigProperties properties;

    private final List<String> contexts = new ArrayList<>();

    public DockerPropertySourceLocator(DockerClient dockerClient, DockerConfigProperties properties) {
        this.dockerClient = dockerClient;
        this.properties = properties;
    }

    @Deprecated
    public List<String> getContexts() {
        return this.contexts;
    }

    @Override
    public Collection<PropertySource<?>> locateCollection(Environment environment) {
        return PropertySourceLocator.locateCollection(this, environment);
    }

    @Override
    public PropertySource<?> locate(Environment environment) {
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment env = (ConfigurableEnvironment) environment;

            DockerPropertySources sources = new DockerPropertySources(properties);

            List<String> profiles = Arrays.asList(env.getActiveProfiles());
            this.contexts.addAll(sources.getAutomaticContexts(profiles));

            CompositePropertySource composite = new CompositePropertySource("docker");

            for (String propertySourceContext : this.contexts) {
                DockerPropertySource propertySource = sources.createPropertySource(propertySourceContext, this.dockerClient);
                if (propertySource != null) {
                    composite.addPropertySource(propertySource);
                }
            }

            return composite;
        }
        return null;
    }

}
