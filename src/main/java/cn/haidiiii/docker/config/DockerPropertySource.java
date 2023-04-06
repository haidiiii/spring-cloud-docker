package cn.haidiiii.docker.config;

import com.github.dockerjava.api.DockerClient;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author fht
 * @since 2022-01-24 9:22
 */
public class DockerPropertySource extends EnumerablePropertySource<DockerClient> {

    private final Map<String, Object> properties = new LinkedHashMap<>();

    private final String context;

    public DockerPropertySource(String context, DockerClient source) {
        super(context, source);
        this.context = context;
    }

    public void init() {
        parseValue(new File(context));
    }

    @SneakyThrows
    protected void parseValue(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }

        String value = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        if (value == null) {
            return;
        }

        Properties props = generateProperties(value);
        for (Map.Entry entry : props.entrySet()) {
            this.properties.put(entry.getKey().toString(), entry.getValue());
        }
    }

    protected Properties generateProperties(String value) {
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ByteArrayResource(value.getBytes(StandardCharsets.UTF_8)));
        return yaml.getObject();
    }


    @Override
    public Object getProperty(String name) {
        return this.properties.get(name);
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> strings = this.properties.keySet();
        return strings.toArray(new String[0]);
    }

}
