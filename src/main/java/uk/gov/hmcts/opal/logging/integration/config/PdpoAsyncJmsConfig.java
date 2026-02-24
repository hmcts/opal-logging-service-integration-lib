package uk.gov.hmcts.opal.logging.integration.config;

import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * Wires the JMS infrastructure for enqueuing PDPO log messages onto Azure Service Bus.
 */
@EnableJms
@Configuration
public class PdpoAsyncJmsConfig {

    @Bean
    public ConnectionFactory pdpoJmsConnectionFactory(PdpoAsyncProperties properties) {
        ServiceBusConnectionStringParser.ConnectionDetails details =
            ServiceBusConnectionStringParser.parse(properties.connectionString());

        String remoteUri = "%s://%s?jms.sendTimeout=%d&amqp.idleTimeout=120000"
            .formatted(properties.protocol(), details.fullyQualifiedNamespace(), properties.sendTimeout().toMillis());

        JmsConnectionFactory qpidFactory = new JmsConnectionFactory(remoteUri);
        qpidFactory.setUsername(details.sharedAccessKeyName());
        qpidFactory.setPassword(details.sharedAccessKey());

        CachingConnectionFactory cachingFactory = new CachingConnectionFactory(qpidFactory);
        cachingFactory.setSessionCacheSize(5);
        cachingFactory.setCacheProducers(true);
        cachingFactory.setReconnectOnException(true);

        return cachingFactory;
    }

    @Bean
    public JacksonJsonMessageConverter pdpoMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_pdpoType");
        return converter;
    }

    @Bean
    public JmsTemplate pdpoJmsTemplate(
        ConnectionFactory pdpoJmsConnectionFactory,
        JacksonJsonMessageConverter pdpoMessageConverter,
        PdpoAsyncProperties properties
    ) {
        JmsTemplate jmsTemplate = new JmsTemplate(pdpoJmsConnectionFactory);
        jmsTemplate.setDefaultDestinationName(properties.queueName());
        jmsTemplate.setMessageConverter(pdpoMessageConverter);
        jmsTemplate.setDeliveryPersistent(true);
        jmsTemplate.setExplicitQosEnabled(true);
        return jmsTemplate;
    }
}
