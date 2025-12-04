package uk.gov.hmcts.opal.logging.integration.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PdpoAsyncPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class)
        .withPropertyValues(
            "logging-service.pdpl.async.connection-string=Endpoint=sb://example/",
            "logging-service.pdpl.async.queue-name=pdpo-queue",
            "logging-service.pdpl.async.log-type=PDPO",
            "logging-service.pdpl.async.max-retries=5",
            "logging-service.pdpl.async.retry-delay=PT2S",
            "logging-service.pdpl.async.send-timeout=PT30S"
        );

    @Test
    void shouldBindConfigurationProperties() {
        contextRunner.run(context -> {
            PdpoAsyncProperties properties = context.getBean(PdpoAsyncProperties.class);

            assertThat(properties.connectionString()).isEqualTo("Endpoint=sb://example/");
            assertThat(properties.queueName()).isEqualTo("pdpo-queue");
            assertThat(properties.logType()).isEqualTo("PDPO");
            assertThat(properties.maxRetries()).isEqualTo(5);
            assertThat(properties.retryDelay()).isEqualTo(Duration.ofSeconds(2));
            assertThat(properties.sendTimeout()).isEqualTo(Duration.ofSeconds(30));
        });
    }

    @Configuration
    @EnableConfigurationProperties(PdpoAsyncProperties.class)
    static class TestConfiguration {
        // No beans required; we only need the properties binding enabled.
    }
}
