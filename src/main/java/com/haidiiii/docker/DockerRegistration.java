package com.haidiiii.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.ServiceSpec;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fht
 * @since 2022-01-29 15:56
 */
public class DockerRegistration implements Registration {

    private final InspectContainerResponse container;
    private final String containerIp;
    private final Service service;
    private final String appName;
    private final String nodeId;
    private final String serviceId;
    private final String serviceName;
    private final String taskId;
    private final String taskName;
    private final String containerId;
    private final String containerName;
    private final Integer port;
    private final Map<String, String> metadata = new HashMap<>();

    public DockerRegistration(String hostname, DockerProperties dockerProperties, DockerClient localDockerClient, DockerClient managerDockerClient) {
        this.container = localDockerClient.inspectContainerCmd(hostname).exec();
        Map<String, String> containerLabels = this.container.getConfig().getLabels();
        this.nodeId = containerLabels.get(DockerConstants.NODE_ID);
        this.serviceId = containerLabels.get(DockerConstants.SERVICE_ID);
        this.serviceName = containerLabels.get(DockerConstants.SERVICE_NAME);
        this.taskId = containerLabels.get(DockerConstants.TASK_ID);
        this.taskName = containerLabels.get(DockerConstants.TASK_NAME);

        this.service = managerDockerClient.inspectServiceCmd(serviceId).exec();
        ServiceSpec serviceSpec = service.getSpec();

        this.metadata.putAll(serviceSpec.getLabels());
        this.metadata.putAll(containerLabels);
        this.appName = this.metadata.get(DockerConstants.APP_NAME);
        this.containerId = this.container.getId();
        this.containerName = this.container.getName();
        this.containerIp = this.container.getNetworkSettings().getNetworks().get(dockerProperties.getNetwork().getName()).getIpAddress();
        this.port = NumberUtils.toInt(this.metadata.get(DockerConstants.SERVER_PORT), 80);
    }

    @Override
    public String getInstanceId() {
        return this.containerName;
    }

    @Override
    public String getServiceId() {
        return this.appName;
    }

    @Override
    public String getHost() {
        return this.containerIp;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public URI getUri() {
        return DefaultServiceInstance.getUri(this);
    }

    @Override
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getDockerServiceId() {
        return this.serviceId;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getTaskId() {
        return this.taskId;
    }

    public String getTaskName() {
        return this.taskName;
    }

    public String getContainerId() {
        return this.containerId;
    }

    public String getContainerName() {
        return this.containerName;
    }

    public InspectContainerResponse getContainer() {
        return this.container;
    }

    public Service getService() {
        return this.service;
    }
}
