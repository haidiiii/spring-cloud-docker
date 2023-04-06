package com.haidiiii.docker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * @author fht
 * @since 2022-01-12 9:51
 */
@Slf4j
public class DockerReactiveDiscoveryClient implements ReactiveDiscoveryClient {

    private final DockerDiscoveryClient dockerDiscoveryClient;

    public DockerReactiveDiscoveryClient(DockerDiscoveryClient dockerDiscoveryClient) {
        this.dockerDiscoveryClient = dockerDiscoveryClient;
    }

    @Override
    public String description() {
        return "Spring Cloud Docker Reactive Discovery Client";
    }

    @Override
    public Flux<ServiceInstance> getInstances(String serviceId) {
        return Flux.defer(() -> Flux.fromIterable(dockerDiscoveryClient.getInstances(serviceId))).onErrorResume(exception -> {
            log.error("Error getting instances from docker.", exception);
            return Flux.empty();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<String> getServices() {
        return Flux.defer(() -> Flux.fromIterable(dockerDiscoveryClient.getServices())).onErrorResume(exception -> {
            log.error("Error getting services from Docker.", exception);
            return Flux.empty();
        }).subscribeOn(Schedulers.boundedElastic());
    }

}
