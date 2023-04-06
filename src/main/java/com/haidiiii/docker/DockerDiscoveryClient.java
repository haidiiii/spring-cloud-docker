package com.haidiiii.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author fht
 * @since 2022-01-28 9:22
 */
@SuppressWarnings("unchecked")
public class DockerDiscoveryClient implements DiscoveryClient {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Cache<String, List<?>> dockerServiceCache;
    private final DockerProperties dockerProperties;
    private final DockerClient dockerClient;

    public DockerDiscoveryClient(ApplicationEventPublisher applicationEventPublisher, DockerProperties dockerProperties, DockerClient dockerClient) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.dockerProperties = dockerProperties;
        this.dockerClient = dockerClient;

        this.dockerServiceCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(Duration.ofSeconds(dockerProperties.getService().getHealthCheckInterval()))
            .build();
    }

    @Override
    public String description() {
        return "Spring Cloud Docker Discovery Client";
    }

    @Override
    @SneakyThrows
    public List<ServiceInstance> getInstances(String serviceId) {
        return (List<ServiceInstance>) dockerServiceCache.get("docker-service-" + serviceId, () ->
            getDockerServices()
                .stream()
                .filter(service -> StringUtils.equals(service.getSpec().getLabels().get(DockerConstants.APP_NAME), serviceId))
                .filter(this::isDockerServiceActive)
                .map(service -> new DockerServiceInstance(serviceId, getHost(service), getPort(service), false, service, this::getDockerTasks))
                .collect(Collectors.toList())
        );
    }

    @Override
    @SneakyThrows
    public List<String> getServices() {
        return (List<String>) dockerServiceCache.get("app-names", () -> getDockerServices()
            .stream()
            .filter(service -> service.getSpec().getLabels().get(DockerConstants.APP_NAME) != null)
            .map(service -> service.getSpec().getLabels().get(DockerConstants.APP_NAME))
            .sorted()
            .collect(Collectors.toList()));
    }

    private int getPort(Service service) {
        String portStr = service.getSpec().getLabels().get(DockerConstants.SERVER_PORT);
        return NumberUtils.toInt(portStr, 80);
    }

    private String getHost(Service service) {
        if (service.getEndpoint() != null) {
            for (EndpointVirtualIP virtualIP : service.getEndpoint().getVirtualIPs()) {
                if (StringUtils.equals(virtualIP.getNetworkID(), dockerProperties.getNetwork().getId())) {
                    return StringUtils.substringBefore(virtualIP.getAddr(), "/");
                }
            }
        }
        return null;
    }

    @SneakyThrows
    private List<Task> getDockerTasks(Service service) {
        String serviceId = service.getId();
        return (List<Task>) dockerServiceCache
            .get("docker-tasks-" + serviceId, () ->
                dockerClient
                    .listTasksCmd()
                    .withLabelFilter(Collections.singletonMap(DockerConstants.STACK_NAMESPACE, dockerProperties.getService().getNamespace()))
                    .withStateFilter(TaskState.RUNNING)
                    .withServiceFilter(serviceId)
                    .exec()
            );
    }

    @SneakyThrows
    private List<Service> getDockerServices() {
        return (List<Service>) dockerServiceCache
            .get("docker-services", () -> {
                    List<Service> services = dockerClient
                        .listServicesCmd()
                        .withLabelFilter(Collections.singletonMap(DockerConstants.STACK_NAMESPACE, dockerProperties.getService().getNamespace()))
                        .exec()
                        .stream()
                        .sorted(Comparator.comparing(this::getServiceVersion).reversed())
                        .collect(Collectors.toList());
                    if (services.size() > 0) {
                        Service service = services.get(0);
                        applicationEventPublisher.publishEvent(new HeartbeatEvent(DockerDiscoveryClient.this, getServiceVersion(service)));
                    }
                    return services;
                }
            );
    }

    private Long getServiceVersion(Service service) {
        return service.getVersion().getIndex();
    }

    private boolean isDockerServiceActive(Service service) {
        ServiceSpec spec = service.getSpec();
        Map<String, String> labels = spec.getLabels();
        String isDown = labels.get("down");
        if (StringUtils.equals(isDown, "1")) { //标记为down的服务，不可用
            return false;
        }
        ServiceModeConfig mode = spec.getMode();
        if (mode.getMode() == ServiceMode.GLOBAL) {
            return true;
        }
        return mode.getReplicated().getReplicas() > 0;
    }
}
