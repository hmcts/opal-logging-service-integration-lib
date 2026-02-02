package uk.gov.hmcts.opal.logging.integration.service;

import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

public interface PdpoSyncPublisher {

    /**
     * Attempts to submit the supplied PDPO log details to the logging service.
     *
     * @param logDetails payload describing the PDPO interaction.
     * @return {@code true} if the log was accepted, {@code false} otherwise.
     */
    boolean publish(PersonalDataProcessingLogDetails logDetails);
}
