package cn.haidiiii.docker.config;

import com.haidiiii.docker.DockerConfigProperties;
import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fht
 * @since 2022-01-24 9:13
 */
@Slf4j
public class DockerPropertySources {

    protected static final List<String> FILES_SUFFIXES = Collections.unmodifiableList(Arrays.asList(".yml", ".yaml", ".properties"));

    private final DockerConfigProperties properties;

    public DockerPropertySources(DockerConfigProperties properties) {
        this.properties = properties;
    }

    public List<String> getAutomaticContexts(List<String> profiles) {
        return getAutomaticContexts(profiles, true);
    }

    public List<String> getAutomaticContexts(List<String> profiles, boolean reverse) {
        return generateAutomaticContexts(profiles, reverse).stream().map(Context::getPath).collect(Collectors.toList());
    }

    public List<Context> generateAutomaticContexts(List<String> profiles, boolean reverse) {
        List<Context> contexts = new ArrayList<>();
        for (String prefix : this.properties.getPrefixes()) {
            String defaultContext = getContext(prefix, properties.getDefaultContext());
            List<String> suffixes = getSuffixes();
            for (String suffix : suffixes) {
                contexts.add(new Context(defaultContext + suffix));
            }
            for (String suffix : suffixes) {
                addProfiles(contexts, defaultContext, profiles, suffix);
            }

            // getName() defaults to ${spring.application.name} or application
            String baseContext = getContext(prefix, properties.getName());

            for (String suffix : suffixes) {
                contexts.add(new Context(baseContext + suffix));
            }
            for (String suffix : suffixes) {
                addProfiles(contexts, baseContext, profiles, suffix);
            }
        }
        if (reverse) {
            // we build them backwards, first wins, so reverse
            Collections.reverse(contexts);
        }
        return contexts;

    }

    protected String getContext(String prefix, String context) {
        if (!StringUtils.hasText(prefix)) {
            return context;
        } else {
            return prefix + "/" + context;
        }
    }

    protected List<String> getSuffixes() {
        return FILES_SUFFIXES;
    }

    private void addProfiles(List<Context> contexts, String baseContext, List<String> profiles, String suffix) {
        for (String profile : profiles) {
            String path = baseContext + properties.getProfileSeparator() + profile + suffix;
            contexts.add(new Context(path, profile));
        }
    }

    public DockerPropertySource createPropertySource(String propertySourceContext, DockerClient dockerClient) {
        try {
            return create(propertySourceContext, dockerClient);
        } catch (DockerPropertySources.PropertySourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            if (properties.isFailFast()) {
                throw new PropertySourceNotFoundException(propertySourceContext, e);
            } else {
                log.warn("Unable to load docker config from " + propertySourceContext, e);
            }
        }
        return null;
    }

    private DockerPropertySource create(String context, DockerClient dockerClient) {
        DockerPropertySource propertySource = new DockerPropertySource(context, dockerClient);
        propertySource.init();
        return propertySource;
    }

    public static class Context {

        private final String path;

        private final String profile;

        public Context(String path) {
            this.path = path;
            this.profile = null;
        }

        public Context(String path, String profile) {
            this.path = path;
            this.profile = profile;
        }

        public String getPath() {
            return this.path;
        }

        public String getProfile() {
            return this.profile;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this).append("path", path).append("profile", profile).toString();

        }

    }

    static class PropertySourceNotFoundException extends RuntimeException {

        private final String context;

        PropertySourceNotFoundException(String context) {
            this.context = context;
        }

        PropertySourceNotFoundException(String context, Exception cause) {
            super(cause);
            this.context = context;
        }

        public String getContext() {
            return this.context;
        }

    }

}
