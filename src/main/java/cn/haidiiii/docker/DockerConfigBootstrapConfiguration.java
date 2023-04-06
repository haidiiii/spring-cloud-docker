package cn.haidiiii.docker;

import com.haidiiii.docker.ConditionalOnDockerEnabled;
import com.haidiiii.docker.DockerConfigProperties;
import com.haidiiii.docker.DockerProperties;
import cn.haidiiii.docker.config.DockerPropertySourceLocator;
import com.github.dockerjava.api.DockerClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @author fht
 * @since 2022-01-24 9:05
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDockerEnabled
@EnableConfigurationProperties({DockerProperties.class, DockerConfigProperties.class})
public class DockerConfigBootstrapConfiguration {

    @Configuration(proxyBeanMethods = false)
    @Import(DockerConfiguration.class)
    @EnableConfigurationProperties
    @ConditionalOnProperty(name = "spring.cloud.docker.config.enabled", matchIfMissing = true)
    protected static class DockerPropertySourceConfiguration {

        private final DockerClient dockerClient;

        public DockerPropertySourceConfiguration(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
        }

        @Bean
        @ConditionalOnMissingBean
        public DockerConfigProperties dockerConfigProperties(Environment env) {
            DockerConfigProperties properties = new DockerConfigProperties();
            if (StringUtils.isBlank(properties.getName())) {
                properties.setName(env.getProperty("spring.application.name", "application"));
            }
            return properties;
        }

        @Bean
        public DockerPropertySourceLocator dockerPropertySourceLocator(DockerConfigProperties dockerConfigProperties) {
            return new DockerPropertySourceLocator(this.dockerClient, dockerConfigProperties);
        }

    }

}
