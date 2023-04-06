package cn.haidiiii.docker;

import com.haidiiii.docker.ConditionalOnDockerEnabled;
import com.haidiiii.docker.DockerProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.PeerNode;
import com.github.dockerjava.api.model.SwarmInfo;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * @author fht
 * @since 2021-04-13 22:35
 */
@Configuration
@ConditionalOnDockerEnabled
@EnableConfigurationProperties(DockerProperties.class)
public class DockerConfiguration {

    @Bean
    public DockerClient localDockerClient(DockerProperties dockerProperties) {
        return createDockerClient(dockerProperties);
    }

    @Bean
    public DockerClient dockerClient(DockerProperties dockerProperties) {
        DockerClient localDockerClient = localDockerClient(dockerProperties);
        Info info = localDockerClient.infoCmd().exec();
        SwarmInfo swarmInfo = info.getSwarm();
        Boolean controlAvailable = swarmInfo.getControlAvailable();
        if (controlAvailable != null && controlAvailable) {
            init(dockerProperties, localDockerClient);
            return localDockerClient;
        }

        List<PeerNode> managers = swarmInfo.getRemoteManagers();
        PeerNode manager = managers.get(0);
        if (managers.size() > 1) {
            manager = managers.get(RandomUtils.nextInt(0, managers.size()));
        }
        String managerHost = "tcp://" + StringUtils.substringBefore(manager.getAddr(), ":") + ":2375";
        DockerClient dockerClient = createDockerClient(managerHost, dockerProperties);
        init(dockerProperties, dockerClient);
        return dockerClient;
    }

    private static void init(DockerProperties dockerProperties, DockerClient dockerClient) {
        if (StringUtils.isBlank(dockerProperties.getNetwork().getId())) {
            dockerClient
                .listNetworksCmd()
                .withNameFilter(dockerProperties.getNetwork().getName())
                .exec()
                .stream()
                .findFirst()
                .ifPresent(network -> dockerProperties.getNetwork().setId(network.getId()));
        }
    }

    public static DockerClient createDockerClient(DockerProperties dockerProperties) {
        return createDockerClient(dockerProperties.getInternalHost(), dockerProperties);
    }

    private static DockerClient createDockerClient(String host, DockerProperties dockerProperties) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(host)
            .withRegistryUsername(dockerProperties.getRegistry().getUsername())
            .withRegistryPassword(dockerProperties.getRegistry().getPassword())
            .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(1000)
            .connectionTimeout(Duration.ofSeconds(10))
            .responseTimeout(Duration.ofSeconds(30))
            .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    @Bean
    public AuthConfig dockerAuthConfig(DockerProperties dockerProperties) {
        DockerProperties.Registry registry = dockerProperties.getRegistry();
        return new AuthConfig().withRegistryAddress(registry.getHost()).withUsername(registry.getUsername()).withPassword(registry.getPassword());
    }

}
