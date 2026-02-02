package uk.gov.hmcts.opal.logging.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.opal.logging.integration.client.PdpoSyncClient;
import uk.gov.hmcts.opal.logging.integration.config.PdpoSyncProperties;
import uk.gov.hmcts.opal.logging.integration.dto.IdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

@ExtendWith(MockitoExtension.class)
class PdpoSyncPublisherImplTest {

    @Mock
    private PdpoSyncClient pdpoSyncClient;

    private PdpoSyncProperties properties;

    private PdpoSyncPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        properties = new PdpoSyncProperties(
            "https://logging-service",
            "/log/pdpo",
            4,
            Duration.ZERO,
            Duration.ofSeconds(2),
            Duration.ofSeconds(5)
        );
        publisher = new PdpoSyncPublisherImpl(pdpoSyncClient, properties);
    }

    @Test
    void shouldSendLogOnce() {
        PersonalDataProcessingLogDetails details = sampleDetails();
        when(pdpoSyncClient.logPdpo(details))
            .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        boolean result = publisher.publish(details);

        assertThat(result).isTrue();
        verify(pdpoSyncClient).logPdpo(details);
    }

    @Test
    void shouldReturnFalseForUnexpectedStatus() {
        PersonalDataProcessingLogDetails details = sampleDetails();
        when(pdpoSyncClient.logPdpo(details))
            .thenReturn(ResponseEntity.ok().build());

        boolean result = publisher.publish(details);

        assertThat(result).isFalse();
        verify(pdpoSyncClient).logPdpo(details);
    }

    @Test
    void shouldHandleNullResponse() {
        PersonalDataProcessingLogDetails details = sampleDetails();
        when(pdpoSyncClient.logPdpo(details))
            .thenReturn(null);

        boolean result = publisher.publish(details);

        assertThat(result).isFalse();
        verify(pdpoSyncClient).logPdpo(details);
    }

    @Test
    void shouldFailFastOnFeignClientError() {
        PersonalDataProcessingLogDetails details = sampleDetails();
        when(pdpoSyncClient.logPdpo(details))
            .thenThrow(notFoundException());

        boolean result = publisher.publish(details);

        assertThat(result).isFalse();
        verify(pdpoSyncClient).logPdpo(details);
    }

    @Test
    void shouldReturnFalseOnRetryableException() {
        PersonalDataProcessingLogDetails details = sampleDetails();
        when(pdpoSyncClient.logPdpo(details))
            .thenThrow(retryableException());

        boolean result = publisher.publish(details);

        assertThat(result).isFalse();
        verify(pdpoSyncClient).logPdpo(details);
    }

    @Test
    void shouldFailFastOnClientErrorResponse() {
        PersonalDataProcessingLogDetails details = sampleDetails();
        when(pdpoSyncClient.logPdpo(details))
            .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        boolean result = publisher.publish(details);

        assertThat(result).isFalse();
        verify(pdpoSyncClient).logPdpo(details);
    }

    @Test
    void shouldIncludeRecipientSummaryOnFailure() {
        PersonalDataProcessingLogDetails details = disclosureDetails();
        when(pdpoSyncClient.logPdpo(details))
            .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        boolean result = publisher.publish(details);

        assertThat(result).isFalse();
        verify(pdpoSyncClient).logPdpo(details);
    }

    private FeignException notFoundException() {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "/log/pdpo",
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            new RequestTemplate()
        );
        return new FeignException.NotFound("not found", request, null, Map.of());
    }

    private RetryableException retryableException() {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "/log/pdpo",
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            new RequestTemplate()
        );
        return new RetryableException(
            503,
            "retryable",
            Request.HttpMethod.POST,
            (Long) null,
            request
        );
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

    private PersonalDataProcessingLogDetails disclosureDetails() {
        ParticipantIdentifier createdBy = ParticipantIdentifier.builder()
            .identifier("creator-1")
            .type(new TestIdentifierType("OPAL_USER_ID"))
            .build();

        ParticipantIdentifier recipient = ParticipantIdentifier.builder()
            .identifier("recipient-1")
            .type(new TestIdentifierType("EXTERNAL_SERVICE"))
            .build();

        return PersonalDataProcessingLogDetails.builder()
            .createdBy(createdBy)
            .businessIdentifier("BUS-999")
            .createdAt(OffsetDateTime.parse("2025-01-10T12:34:56.789Z"))
            .ipAddress("192.0.2.1")
            .category(PersonalDataProcessingCategory.DISCLOSURE)
            .recipient(recipient)
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
