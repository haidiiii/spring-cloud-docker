package com.haidiiii.docker;

import com.github.dockerjava.api.DockerClient;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When both property and docker classes are on the classpath.
 *
 * @author fht
 * @since 2022-01-24 9:03
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(ConditionalOnDockerEnabled.OnDockerEnabledCondition.class)
public @interface ConditionalOnDockerEnabled {

    /**
     * Verifies multiple conditions to see if docker should be enabled.
     */
    class OnDockerEnabledCondition extends AllNestedConditions {

        OnDockerEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        /**
         * Docker property is enabled.
         */
        @ConditionalOnProperty("spring.cloud.docker.enabled")
        static class FoundProperty {

        }

        /**
         * Docker client class found.
         */
        @ConditionalOnClass(DockerClient.class)
        static class FoundClass {

        }

    }

}
