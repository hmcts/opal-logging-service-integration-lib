package uk.gov.hmcts.opal.logging.integration.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PdpoSyncPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class)
        .withPropertyValues(
            "logging-service.pdpl.sync.base-url=https://logging-service",
            "logging-service.pdpl.sync.endpoint=/log/pdpo",
            "logging-service.pdpl.sync.max-attempts=5",
            "logging-service.pdpl.sync.retry-delay=PT3S",
            "logging-service.pdpl.sync.connect-timeout=PT2S",
            "logging-service.pdpl.sync.read-timeout=PT4S"
        );

    @Test
    void shouldBindConfigurationProperties() {
        contextRunner.run(context -> {
            PdpoSyncProperties properties = context.getBean(PdpoSyncProperties.class);

            assertThat(properties.baseUrl()).isEqualTo("https://logging-service");
            assertThat(properties.endpoint()).isEqualTo("/log/pdpo");
            assertThat(properties.maxAttempts()).isEqualTo(5);
            assertThat(properties.retryDelay()).isEqualTo(Duration.ofSeconds(3));
            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(4));
        });
    }

    @Configuration
    @EnableConfigurationProperties(PdpoSyncProperties.class)
    static class TestConfiguration {
        // No beans required; we only need the properties binding enabled.
    }
}
