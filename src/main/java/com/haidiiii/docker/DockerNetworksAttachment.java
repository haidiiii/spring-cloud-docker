package com.haidiiii.docker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * docker client 未实现，待docker client实现后删除
 *
 * @author fht
 * @since 2022-03-10 14:21
 */
@Data
public class DockerNetworksAttachment implements Serializable {
    private static final long serialVersionUID = -474151803648873566L;

    @JsonProperty("Network")
    private Network network;

    @JsonProperty("Addresses")
    private List<String> addresses;

    @Data
    public static class Network implements Serializable {
        private static final long serialVersionUID = -2011530703042690183L;

        @JsonProperty("ID")
        private String id;

    }

}
