package uk.gov.hmcts.opal.logging.integration.service;

import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.logging.integration.client.PdpoSyncClient;
import uk.gov.hmcts.opal.logging.integration.config.PdpoSyncProperties;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdpoSyncPublisherImpl implements PdpoSyncPublisher {

    private final PdpoSyncClient pdpoSyncClient;
    private final PdpoSyncProperties properties;

    @Override
    public boolean publish(PersonalDataProcessingLogDetails logDetails) {
        try {
            log.debug("Sending PDPO log details={}", logDetails);
            ResponseEntity<Void> response = pdpoSyncClient.logPdpo(logDetails);
            if (response != null && response.getStatusCode() == HttpStatus.CREATED) {
                log.info("Sent PDPO log");
                return true;
            }
            String status = response == null ? "<null>" : String.valueOf(response.getStatusCode().value());
            log.warn("Non-retryable PDPO response status={}, logDetails={}", status, logDetails);
            return false;
        } catch (RetryableException ex) {
            log.error("Unable to send PDPO log after {} attempts, lastFailure={}, logDetails={}",
                properties.maxRetries(),
                "HTTP " + ex.status(),
                logDetails,
                ex);
            return false;
        } catch (FeignException ex) {
            log.warn("Non-retryable PDPO response status={} body={} logDetails={}",
                ex.status(),
                ex.contentUTF8(),
                logDetails,
                ex);
            return false;
        } catch (Exception ex) {
            log.error("Unable to send PDPO log, lastFailure={}, logDetails={}",
                ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                logDetails,
                ex);
            return false;
        }
    }
}
