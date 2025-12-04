package uk.gov.hmcts.opal.logging.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import jakarta.jms.Message;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import uk.gov.hmcts.opal.logging.integration.config.PdpoAsyncProperties;
import uk.gov.hmcts.opal.logging.integration.dto.IdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;
import uk.gov.hmcts.opal.logging.integration.messaging.PdpoLogMessage;

@ExtendWith(MockitoExtension.class)
class PdpoAsyncPublisherImplTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Captor
    private ArgumentCaptor<Object> payloadCaptor;

    @Captor
    private ArgumentCaptor<MessagePostProcessor> postProcessorCaptor;

    private PdpoAsyncProperties properties;

    @InjectMocks
    private PdpoAsyncPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        properties = new PdpoAsyncProperties(
            "Endpoint=sb://example/",
            "pdpo-queue",
            "PDPO",
            3,
            Duration.ZERO,
            Duration.ofSeconds(5)
        );
        publisher = new PdpoAsyncPublisherImpl(jmsTemplate, properties);
    }

    @Test
    void shouldPublishMessageOnce() throws Exception {
        PersonalDataProcessingLogDetails details = sampleDetails();

        boolean result = publisher.publish(details);

        assertThat(result).isTrue();

        verify(jmsTemplate).convertAndSend(eq("pdpo-queue"), payloadCaptor.capture(), postProcessorCaptor.capture());

        assertThat(payloadCaptor.getValue()).isInstanceOf(PdpoLogMessage.class);
        PdpoLogMessage message = (PdpoLogMessage) payloadCaptor.getValue();
        assertThat(message.logType()).isEqualTo("PDPO");
        assertThat(message.details()).isEqualTo(details);

        Message jmsMessage = mock(Message.class);
        postProcessorCaptor.getValue().postProcessMessage(jmsMessage);
        verify(jmsMessage).setStringProperty("logType", "PDPO");
        verify(jmsMessage).setStringProperty("createdByType", "OPAL_USER_ID");
        verifyNoMoreInteractions(jmsMessage);
    }

    @Test
    void shouldRetryAndSucceed() {
        PersonalDataProcessingLogDetails details = sampleDetails();
        doThrow(new JmsException("boom") { })
            .doNothing()
            .when(jmsTemplate)
            .convertAndSend(eq("pdpo-queue"), any(), any(MessagePostProcessor.class));

        boolean result = publisher.publish(details);

        assertThat(result).isTrue();
        verify(jmsTemplate, Mockito.times(2))
            .convertAndSend(eq("pdpo-queue"), any(), any(MessagePostProcessor.class));
    }

    @Test
    void shouldReturnFalseWhenAllRetriesFail() {
        PersonalDataProcessingLogDetails details = sampleDetails();
        doThrow(new JmsException("boom") { })
            .when(jmsTemplate)
            .convertAndSend(eq("pdpo-queue"), any(), any(MessagePostProcessor.class));

        boolean result = publisher.publish(details);

        assertThat(result).isFalse();
        verify(jmsTemplate, Mockito.times(properties.maxRetries()))
            .convertAndSend(eq("pdpo-queue"), any(), any(MessagePostProcessor.class));
    }

    private PersonalDataProcessingLogDetails sampleDetails() {
        ParticipantIdentifier createdBy = ParticipantIdentifier.builder()
            .identifier("creator-1")
            .type(new TestIdentifierType("OPAL_USER_ID"))
            .build();

        return PersonalDataProcessingLogDetails.builder()
            .createdBy(createdBy)
            .businessIdentifier("BUS-123")
            .createdAt(OffsetDateTime.parse("2025-01-10T12:34:56.789Z"))
            .ipAddress("192.0.2.1")
            .category(PersonalDataProcessingCategory.COLLECTION)
            .individuals(List.of(createdBy))
            .build();
    }

    private record TestIdentifierType(String type) implements IdentifierType {
        @Override
        public String getType() {
            return type;
        }
    }
}
