package com.haidiiii.docker;

import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.Task;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author fht
 * @since 2022-03-10 11:14
 */
public class DockerServiceInstance implements ServiceInstance {

    private final String appName;
    private final Service service;
    private final Function<Service, List<Task>> tasksFunction;
    private final String host;
    private final int port;
    private final boolean secure;
    private final Map<String, String> metadata;

    public DockerServiceInstance(String appName, String host, int port, boolean secure, Service service, Function<Service, List<Task>> tasksFunction) {
        this.appName = appName;
        this.service = service;
        this.metadata = service.getSpec().getLabels();
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.tasksFunction = tasksFunction;
    }

    @Override
    public String getInstanceId() {
        return this.service.getId();
    }

    @Override
    public String getServiceId() {
        return this.appName;
    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    @Override
    public URI getUri() {
        return DefaultServiceInstance.getUri(this);
    }

    @Override
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    public Service getService() {
        return this.service;
    }

    public Function<Service, List<Task>> getTasksFunction() {
        return tasksFunction;
    }

    public List<Task> getTasks() {
        return this.tasksFunction.apply(this.service);
    }

}
