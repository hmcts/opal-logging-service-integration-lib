package uk.gov.hmcts.opal.logging.integration.service;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.logging.integration.config.PdpoAsyncProperties;
import uk.gov.hmcts.opal.logging.integration.dto.IdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;
import uk.gov.hmcts.opal.logging.integration.messaging.PdpoLogMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdpoAsyncPublisherImpl implements PdpoAsyncPublisher {

    private final JmsTemplate jmsTemplate;
    private final PdpoAsyncProperties properties;

    @Override
    public boolean publish(PersonalDataProcessingLogDetails logDetails) {
        for (int attempt = 1; attempt <= properties.maxRetries(); attempt++) {
            try {
                send(logDetails);
                log.info("Enqueued PDPO log for businessIdentifier={} (attempt {}/{})",
                    safeValue(logDetails.getBusinessIdentifier()), attempt, properties.maxRetries());
                return true;
            } catch (JmsException ex) {
                log.warn("Failed to enqueue PDPO log for businessIdentifier={} (attempt {}/{})",
                    safeValue(logDetails.getBusinessIdentifier()), attempt, properties.maxRetries(), ex);
                pauseBetweenAttempts();
            }
        }

        String businessIdentifierSummary = safeValue(Optional.ofNullable(logDetails)
            .map(PersonalDataProcessingLogDetails::getBusinessIdentifier)
            .orElse(null));
        Map<String, Object> logDetailsSummary = buildLogDetailsSummary(logDetails);

        log.error("Unable to enqueue PDPO log after {} attempts for businessIdentifier={}, logDetails={}",
            properties.maxRetries(),
            businessIdentifierSummary,
            logDetailsSummary);
        return false;
    }

    private void send(PersonalDataProcessingLogDetails logDetails) throws JmsException {
        PdpoLogMessage message = new PdpoLogMessage(properties.logType(), logDetails);
        jmsTemplate.convertAndSend(
            properties.queueName(),
            message,
            jmsMessage -> {
                setStringProperty(jmsMessage, "logType", properties.logType());
                Optional.ofNullable(logDetails.getCreatedBy())
                    .map(ParticipantIdentifier::getType)
                    .map(IdentifierType::getType)
                    .ifPresent(type -> setStringProperty(jmsMessage, "createdByType", type));
                return jmsMessage;
            }
        );
    }

    private void setStringProperty(Message message, String key, String value) {
        try {
            message.setStringProperty(key, value);
        } catch (JMSException ex) {
            throw new JmsException("Unable to set JMS property: " + key, ex) {
            };
        }
    }

    private void pauseBetweenAttempts() {
        Duration delay = properties.retryDelay();
        if (delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private String safeValue(String value) {
        return value == null ? "<null>" : value;
    }

    private Map<String, Object> buildLogDetailsSummary(PersonalDataProcessingLogDetails logDetails) {
        if (logDetails == null) {
            return Map.of(
                "logType", properties.logType(),
                "details", "<null>"
            );
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("logType", properties.logType());
        summary.put("businessIdentifier", safeValue(logDetails.getBusinessIdentifier()));
        summary.put("createdByIdentifier", safeParticipantIdentifier(logDetails.getCreatedBy()));
        summary.put("createdByIdentifierType", safeParticipantType(logDetails.getCreatedBy()));
        summary.put("category", safeCategory(logDetails.getCategory()));
        summary.put("createdAt", safeTimestamp(logDetails.getCreatedAt()));
        summary.put("ipAddress", safeValue(logDetails.getIpAddress()));

        if (shouldIncludeRecipient(logDetails)) {
            summary.put("recipientIdentifier", safeParticipantIdentifier(logDetails.getRecipient()));
            summary.put("recipientIdentifierType", safeParticipantType(logDetails.getRecipient()));
        }

        summary.put("individuals", buildIndividualsSummary(logDetails.getIndividuals()));
        return summary;
    }

    private boolean shouldIncludeRecipient(PersonalDataProcessingLogDetails logDetails) {
        return logDetails.getRecipient() != null
            && logDetails.getCategory() == PersonalDataProcessingCategory.DISCLOSURE;
    }

    private String safeParticipantIdentifier(ParticipantIdentifier participant) {
        if (participant == null) {
            return "<null>";
        }
        return safeValue(participant.getIdentifier());
    }

    private String safeParticipantType(ParticipantIdentifier participant) {
        if (participant == null) {
            return "<null>";
        }
        IdentifierType type = participant.getType();
        return type == null ? "<null>" : safeValue(type.getType());
    }

    private List<Map<String, String>> buildIndividualsSummary(List<ParticipantIdentifier> individuals) {
        if (individuals == null || individuals.isEmpty()) {
            return List.of();
        }

        return individuals.stream()
            .map(this::summariseParticipant)
            .toList();
    }

    private Map<String, String> summariseParticipant(ParticipantIdentifier participant) {
        Map<String, String> participantSummary = new LinkedHashMap<>();
        participantSummary.put("identifier", safeParticipantIdentifier(participant));
        participantSummary.put("type", safeParticipantType(participant));
        return participantSummary;
    }

    private String safeTimestamp(OffsetDateTime timestamp) {
        return timestamp == null ? "<null>" : timestamp.toString();
    }

    private String safeCategory(PersonalDataProcessingCategory category) {
        return category == null ? "<null>" : category.getJsonValue();
    }
}
