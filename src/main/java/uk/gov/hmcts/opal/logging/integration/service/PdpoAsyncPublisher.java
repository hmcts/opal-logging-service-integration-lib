package uk.gov.hmcts.opal.logging.integration.service;

import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

/**
 * Publishes Personal Data Processing log requests asynchronously via JMS.
 */
public interface PdpoAsyncPublisher {

    /**
     * Attempts to enqueue the supplied PDPO log details.
     *
     * @param logDetails payload to send to the Logging Service queue.
     * @return {@code true} if the payload was enqueued (or queued after retries), {@code false} otherwise.
     */
    boolean publish(PersonalDataProcessingLogDetails logDetails);
}
