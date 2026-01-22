package uk.gov.hmcts.opal.logging.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Minimal Spring Boot application entrypoint to support library wiring during local runs.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@ConditionalOnProperty(name = "logging-service.enable-spring-support", havingValue = "true")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
