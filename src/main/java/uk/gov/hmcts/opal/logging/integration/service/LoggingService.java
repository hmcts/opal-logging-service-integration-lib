package uk.gov.hmcts.opal.logging.integration.service;

import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

/**
 * Logging operations exposed to calling services.
 */
public interface LoggingService {

    /**
     * Enqueues a Personal Data Processing log entry onto the logging service queue.
     *
     * @param logDetails payload describing the PDPO interaction.
     * @return {@code true} if the log was successfully queued, {@code false} otherwise.
     */
    boolean personalDataAccessLogAsync(PersonalDataProcessingLogDetails logDetails);
}
