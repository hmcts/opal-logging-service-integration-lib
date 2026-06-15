package uk.gov.hmcts.opal.logging.integration.config;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires the JMS infrastructure for enqueuing PDPO log messages onto Azure Service Bus.
 */
@EnableJms
@Configuration
public class PdpoAsyncJmsConfig {

    @Bean("pdpoJmsConnectionFactory")
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
        // Service Bus can forcibly detach an idle producer link. Do not reuse cached producers after that.
        cachingFactory.setCacheProducers(false);
        cachingFactory.setReconnectOnException(true);

        return cachingFactory;
    }

    @Bean
    public MessageConverter pdpoMessageConverter(ObjectMapper objectMapper) {
        return new PdpoJacksonMessageConverter(objectMapper);
    }

    @Bean("pdpoJmsTemplate")
    public JmsTemplate pdpoJmsTemplate(
        ConnectionFactory pdpoJmsConnectionFactory,
        @Qualifier("pdpoMessageConverter") MessageConverter pdpoMessageConverter,
        PdpoAsyncProperties properties
    ) {
        JmsTemplate jmsTemplate = new JmsTemplate(pdpoJmsConnectionFactory);
        jmsTemplate.setDefaultDestinationName(properties.queueName());
        jmsTemplate.setMessageConverter(pdpoMessageConverter);
        jmsTemplate.setDeliveryPersistent(true);
        jmsTemplate.setExplicitQosEnabled(true);
        return jmsTemplate;
    }

    private static final class PdpoJacksonMessageConverter implements MessageConverter {
        private final ObjectMapper objectMapper;

        private PdpoJacksonMessageConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
            try {
                TextMessage message = session.createTextMessage(objectMapper.writeValueAsString(object));
                message.setStringProperty("_pdpoType", object.getClass().getName());
                return message;
            } catch (JacksonException ex) {
                throw new MessageConversionException("Unable to serialise PDPO JMS payload", ex);
            }
        }

        @Override
        public Object fromMessage(Message message) throws JMSException, MessageConversionException {
            if (message instanceof TextMessage textMessage) {
                return textMessage.getText();
            }
            throw new MessageConversionException("Unsupported PDPO JMS message type: " + message.getClass().getName());
        }
    }
}
