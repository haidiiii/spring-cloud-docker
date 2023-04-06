package com.haidiiii.docker;

import com.github.dockerjava.api.model.UpdateOrder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import static com.haidiiii.docker.DockerProperties.PREFIX;

/**
 * @author fht
 * @since 2021-04-13 22:34
 */
@Data
@ConfigurationProperties(prefix = PREFIX)
public class DockerProperties {

    public static final String PREFIX = "spring.cloud.docker";

    private String internalHost = "tcp://host.docker.internal:2375";
    private Network Network = new Network();
    private Registry registry = new Registry();
    private Service service = new Service();

    @Data
    public static class Registry {
        private String host = "docker.haidiiii.com:5443";
        private String project = "saas";
        private String username;
        private String password;
    }

    @Data
    public static class Network {
        private String id;
        private String name = "core-infra";
    }

    @Data
    public static class Service {
        private String namespace = "saas";
        private String namePrefix = "saas_";
        private String tag = "latest";
        private Integer replicas = 1;
        private Integer healthCheckInterval = 30;
        private UpdateOrder updateOrder = UpdateOrder.START_FIRST;

        @DataSizeUnit(DataUnit.MEGABYTES)
        private DataSize reserveMemory = DataSize.ofMegabytes(1024);

        @DataSizeUnit(DataUnit.MEGABYTES)
        private DataSize limitMemory = DataSize.ofMegabytes(1024);

    }

}
