package com.haidiiii.docker;

import com.github.dockerjava.api.DockerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author fht
 * @since 2022-02-14 14:34
 */
@Configuration
@ConditionalOnDockerEnabled
@ConditionalOnProperty(value = "spring.cloud.docker.discovery.enabled", matchIfMissing = true)
public class DockerDiscoveryClientConfiguration {

    @Value("${spring.cloud.client.hostname}")
    private String hostname;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DockerProperties dockerProperties;
    private final DockerClient localDockerClient;
    private final DockerClient dockerClient;

    public DockerDiscoveryClientConfiguration(ApplicationEventPublisher applicationEventPublisher, DockerProperties dockerProperties, DockerClient localDockerClient, DockerClient dockerClient) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.dockerProperties = dockerProperties;
        this.localDockerClient = localDockerClient;
        this.dockerClient = dockerClient;
    }

    @Bean
    public DockerRegistration dockerRegistration() {
        return new DockerRegistration(hostname, dockerProperties, localDockerClient, dockerClient);
    }

    @Bean
    public DockerReactiveDiscoveryClient dockerReactiveDiscoveryClient() {
        return new DockerReactiveDiscoveryClient(dockerDiscoveryClient());
    }

    @Bean
    public DockerDiscoveryClient dockerDiscoveryClient() {
        return new DockerDiscoveryClient(applicationEventPublisher, dockerProperties, dockerClient);
    }
}
