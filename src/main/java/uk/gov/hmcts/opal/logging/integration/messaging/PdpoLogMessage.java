package uk.gov.hmcts.opal.logging.integration.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Envelope sent to the Logging Service queue so the consumer can detect the log type.
 */
public record PdpoLogMessage(
    @JsonProperty("log_type") String logType,
    @JsonProperty("details") PdpoQueueLogDetails details
) {
}
