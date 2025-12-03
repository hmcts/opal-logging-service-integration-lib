package uk.gov.hmcts.opal.logging.integration.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;

class PdpoAsyncJmsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withPropertyValues(
            "logging-service.pdpl.async.connection-string="
                + "Endpoint=sb://example.servicebus.windows.net/;"
                + "SharedAccessKeyName=OpalPdpo;"
                + "SharedAccessKey=secret=",
            "logging-service.pdpl.async.queue-name=pdpo-queue"
        )
        .withUserConfiguration(TestConfig.class);

    @Test
    void shouldExposeJmsBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ConnectionFactory.class);
            ConnectionFactory factory = context.getBean(ConnectionFactory.class);
            assertThat(factory).isInstanceOf(CachingConnectionFactory.class);

            MappingJackson2MessageConverter converter = context.getBean(MappingJackson2MessageConverter.class);
            assertThat(converter).isNotNull();

            JmsTemplate template = context.getBean(JmsTemplate.class);
            assertThat(template.getDefaultDestinationName()).isEqualTo("pdpo-queue");
            assertThat(template.isExplicitQosEnabled()).isTrue();
        });
    }

    @Configuration
    @EnableConfigurationProperties(PdpoAsyncProperties.class)
    @Import(PdpoAsyncJmsConfig.class)
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
