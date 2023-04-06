package com.haidiiii.docker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author fht
 * @since 2022-01-24 9:07
 */
@Data
@Validated
@ConfigurationProperties(DockerConfigProperties.PREFIX)
public class DockerConfigProperties {

    /**
     * Prefix for configuration properties.
     */
    public static final String PREFIX = "spring.cloud.docker.config";

    private boolean enabled = true;

    private List<String> prefixes = new ArrayList<>(Collections.singletonList("/apps/saas/config"));
    private String defaultContext = "application";
    private String profileSeparator = "-";

    /**
     * Throw exceptions during config lookup if true, otherwise, log warnings.
     */
    private boolean failFast = true;

    private String name;

}
