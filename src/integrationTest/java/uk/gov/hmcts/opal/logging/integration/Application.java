package uk.gov.hmcts.opal.logging.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Minimal Spring Boot application entrypoint to support library wiring during local runs.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableFeignClients(basePackages = "uk.gov.hmcts.opal.logging.integration")
@ConditionalOnProperty(name = "logging-service.enable-spring-support", havingValue = "true")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
