package cn.haidiiii.docker.config;

import com.haidiiii.docker.DockerConfigProperties;
import com.haidiiii.docker.DockerProperties;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.cloud.commons.ConfigDataMissingEnvironmentPostProcessor;
import org.springframework.core.env.Environment;

import static org.springframework.cloud.util.PropertyUtils.bootstrapEnabled;
import static org.springframework.cloud.util.PropertyUtils.useLegacyProcessing;

/**
 * @author fht
 * @since 2022-07-01 8:30
 */
public class DockerConfigDataMissingEnvironmentPostProcessor extends ConfigDataMissingEnvironmentPostProcessor {

    /**
     * Order of post processor, set to run after
     * {@link ConfigDataEnvironmentPostProcessor}.
     */
    public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 1000;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected boolean shouldProcessEnvironment(Environment environment) {
        // don't run if using bootstrap or legacy processing
        if (bootstrapEnabled(environment) || useLegacyProcessing(environment)) {
            return false;
        }
        boolean coreEnabled = environment.getProperty(DockerProperties.PREFIX + ".enabled", Boolean.class, false);
        boolean configEnabled = environment.getProperty(DockerConfigProperties.PREFIX + ".enabled", Boolean.class, true);
        boolean importCheckEnabled = environment.getProperty(DockerConfigProperties.PREFIX + ".import-check.enabled",
            Boolean.class, true);
        if (!coreEnabled || !configEnabled || !importCheckEnabled) {
            return false;
        }
        return true;
    }

    @Override
    protected String getPrefix() {
        return DockerConfigDataLocationResolver.PREFIX;
    }

    static class ImportExceptionFailureAnalyzer extends AbstractFailureAnalyzer<ImportException> {

        @Override
        protected FailureAnalysis analyze(Throwable rootFailure, ImportException cause) {
            String description;
            if (cause.missingPrefix) {
                description = "The spring.config.import property is missing a " + DockerConfigDataLocationResolver.PREFIX + " entry";
            } else {
                description = "No spring.config.import property has been defined";
            }
            String action = "Add a spring.config.import=docker: property to your configuration.\n"
                + "\tIf configuration is not required add spring.config.import=optional:docker: instead.\n"
                + "\tTo disable this check, set spring.cloud.docker.config.enabled=false or \n"
                + "\tspring.cloud.docker.config.import-check.enabled=false.";
            return new FailureAnalysis(description, action, cause);
        }

    }

}
