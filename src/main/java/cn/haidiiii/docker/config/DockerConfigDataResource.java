package cn.haidiiii.docker.config;

import com.haidiiii.docker.DockerConfigProperties;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.core.style.ToStringCreator;

import java.util.Objects;

/**
 * @author fht
 * @since 2022-07-01 8:36
 */
public class DockerConfigDataResource extends ConfigDataResource {

    private final DockerConfigProperties properties;

    private final String context;

    private final boolean optional;

    private final DockerPropertySources dockerPropertySources;

    private final String profile;

    public DockerConfigDataResource(String context, DockerConfigProperties properties,
                                    DockerPropertySources dockerPropertySources, String profile) {
        this.properties = properties;
        this.context = context;
        this.optional = true;
        this.dockerPropertySources = dockerPropertySources;
        this.profile = profile;
    }

    public String getContext() {
        return this.context;
    }

    @Deprecated
    public boolean isOptional() {
        return this.optional;
    }

    public DockerConfigProperties getProperties() {
        return this.properties;
    }

    public DockerPropertySources getDockerPropertySources() {
        return this.dockerPropertySources;
    }

    String getProfile() {
        return this.profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DockerConfigDataResource that = (DockerConfigDataResource) o;
        return this.optional == that.optional && this.context.equals(that.context)
            && Objects.equals(this.profile, that.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.context, this.optional, this.profile);
    }

    @Override
    public String toString() {
        return new ToStringCreator(this).append("context", context).append("optional", optional)
            .append("properties", properties).append("profile", profile).toString();

    }

}
