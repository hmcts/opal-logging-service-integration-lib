package uk.gov.hmcts.opal.logging.integration.service;

import feign.FeignException;
import java.time.Duration;
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
        String lastFailure = null;

        for (int attempt = 1; attempt <= properties.maxRetries(); attempt++) {
            try {
                log.debug("Sending PDPO log details={} (attempt {}/{})",
                    logDetails,
                    attempt,
                    properties.maxRetries());
                ResponseEntity<Void> response = pdpoSyncClient.logPdpo(logDetails);
                if (response != null && response.getStatusCode() == HttpStatus.CREATED) {
                    log.info("Sent PDPO log (attempt {}/{})", attempt, properties.maxRetries());
                    return true;
                }

                RetryDecision decision = evaluateRetry(response);
                lastFailure = failureMessage(decision);
                if (!decision.retryable()) {
                    log.warn("Non-retryable PDPO response status={}, logDetails={}",
                        decision.statusLabel(),
                        logDetails);
                    return false;
                }
                log.warn("Retryable PDPO response status={} (attempt {}/{})",
                    decision.statusLabel(),
                    attempt,
                    properties.maxRetries());
            } catch (FeignException ex) {
                RetryDecision decision = evaluateRetry(ex);
                lastFailure = failureMessage(decision);
                if (!decision.retryable()) {
                    log.warn("Non-retryable PDPO response status={} body={} logDetails={}",
                        decision.statusLabel(),
                        decision.body(),
                        logDetails,
                        ex);
                    return false;
                }
                log.warn("Failed to send PDPO log status={} body={} (attempt {}/{})",
                    decision.statusLabel(),
                    decision.body(),
                    attempt,
                    properties.maxRetries(),
                    ex);
            } catch (Exception ex) {
                lastFailure = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                log.warn("Failed to send PDPO log (attempt {}/{})",
                    attempt,
                    properties.maxRetries(),
                    ex);
            }
            pauseBetweenAttempts();
        }

        log.error("Unable to send PDPO log after {} attempts, lastFailure={}, logDetails={}",
            properties.maxRetries(),
            lastFailure,
            logDetails);
        return false;
    }

    private RetryDecision evaluateRetry(ResponseEntity<Void> response) {
        if (response == null) {
            return RetryDecision.retryable(null, null);
        }
        String statusLabel = String.valueOf(response.getStatusCode().value());
        // Treat 429 as retryable (rate limiting) alongside 5xx.
        if (response.getStatusCode().is5xxServerError() || response.getStatusCode().value() == 429) {
            return RetryDecision.retryable(statusLabel, null);
        }
        return RetryDecision.notRetryable(statusLabel, null);
    }

    private RetryDecision evaluateRetry(FeignException ex) {
        String statusLabel = String.valueOf(ex.status());
        String body = ex.contentUTF8();
        // Feign uses -1 when no HTTP response is available (e.g., network error).
        if (ex.status() >= 500 || ex.status() == 429 || ex.status() == -1) {
            return RetryDecision.retryable(statusLabel, body);
        }
        return RetryDecision.notRetryable(statusLabel, body);
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

    private String failureMessage(RetryDecision decision) {
        if (decision.statusLabel() == null) {
            return "null response";
        }
        if (decision.body() == null) {
            return "HTTP " + decision.statusLabel();
        }
        return "HTTP " + decision.statusLabel() + " body=" + decision.body();
    }

    private record RetryDecision(boolean retryable, String statusLabel, String body) {
        static RetryDecision retryable(String statusLabel, String body) {
            return new RetryDecision(true, statusLabel, body);
        }

        static RetryDecision notRetryable(String statusLabel, String body) {
            return new RetryDecision(false, statusLabel, body);
        }
    }
}
