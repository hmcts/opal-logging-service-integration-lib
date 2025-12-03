package uk.gov.hmcts.opal.logging.integration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

/**
 * Delegates PDPO logging operations to the async publisher.
 */
@Service
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {

    private final PdpoAsyncPublisher pdpoAsyncPublisher;

    @Override
    public boolean personalDataAccessLogAsync(PersonalDataProcessingLogDetails logDetails) {
        return pdpoAsyncPublisher.publish(logDetails);
    }
}
