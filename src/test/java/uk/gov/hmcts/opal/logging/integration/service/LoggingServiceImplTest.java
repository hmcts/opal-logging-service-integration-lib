package uk.gov.hmcts.opal.logging.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

@ExtendWith(MockitoExtension.class)
class LoggingServiceImplTest {

    @Mock
    private PdpoAsyncPublisher pdpoAsyncPublisher;

    @InjectMocks
    private LoggingServiceImpl loggingService;

    @Test
    void shouldDelegateToPublisher() {
        PersonalDataProcessingLogDetails details = PersonalDataProcessingLogDetails.builder()
            .businessIdentifier("BUS-789")
            .createdAt(OffsetDateTime.now())
            .build();

        when(pdpoAsyncPublisher.publish(details)).thenReturn(true);

        boolean result = loggingService.personalDataAccessLogAsync(details);

        assertThat(result).isTrue();
        verify(pdpoAsyncPublisher).publish(details);
    }
}
