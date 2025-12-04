package uk.gov.hmcts.opal.logging.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.azure.core.amqp.models.AmqpAnnotatedMessage;
import com.azure.core.amqp.models.AmqpMessageBody;
import com.azure.core.amqp.models.AmqpMessageBodyType;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Utility test that peeks raw messages from the PDPO queue using the Azure SDK.
 * This provides a fallback when the Azure portal cannot display messages.
 */
class PdpoQueuePeekTest {

    private static final String CONNECTION_STRING_ENV = "LOGGING_PDPL_ASYNC_CONNECTION_STRING";
    private static final String QUEUE_NAME_ENV = "LOGGING_PDPL_ASYNC_QUEUE";
    private static final String ENABLED_ENV = "LOGGING_PDPL_ASYNC_CONNECTIVITY_TEST_ENABLED";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ServiceBusReceiverClient receiverClient;

    @AfterEach
    void tearDown() {
        if (receiverClient != null) {
            receiverClient.close();
        }
    }

    @Test
    void shouldPeekMessagesUsingAzureClient() {
        String connectionString = System.getenv(CONNECTION_STRING_ENV);
        String queueName = System.getenv(QUEUE_NAME_ENV);
        boolean enabled = Boolean.parseBoolean(System.getenv().getOrDefault(ENABLED_ENV, "false"));

        assumeTrue(connectionString != null && queueName != null && enabled,
            () -> "Set " + CONNECTION_STRING_ENV + ", " + QUEUE_NAME_ENV + " and "
                + ENABLED_ENV + "=true to peek messages");

        receiverClient = new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .receiver()
            .queueName(queueName)
            .buildClient();

        List<ServiceBusReceivedMessage> messages = receiverClient.peekMessages(5)
            .stream()
            .collect(Collectors.toList());

        assertThat(messages)
            .as("Expect at least one message when peeking PDPO queue")
            .isNotEmpty();

        messages.forEach(msg -> System.out.println("Peeked message: " + describeMessage(msg)));
    }

    private String describeMessage(ServiceBusReceivedMessage message) {
        return "seq=" + message.getSequenceNumber()
            + ", enqueued=" + message.getEnqueuedTime().toString()
            + ", body=" + extractBodySummary(message);
    }

    private String extractBodySummary(ServiceBusReceivedMessage message) {
        try {
            return message.getBody().toString();
        } catch (UnsupportedOperationException ex) {
            AmqpAnnotatedMessage annotated = message.getRawAmqpMessage();
            if (annotated == null || annotated.getBody() == null) {
                return "<no body>";
            }
            AmqpMessageBody body = annotated.getBody();
            if (body.getBodyType() == AmqpMessageBodyType.VALUE) {
                Object value = body.getValue();
                try {
                    return OBJECT_MAPPER.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    return String.valueOf(value);
                }
            }
            return "<body type=" + body.getBodyType() + ">";
        }
    }
}
