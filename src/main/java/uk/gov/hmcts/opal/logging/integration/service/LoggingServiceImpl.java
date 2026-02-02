package uk.gov.hmcts.opal.logging.integration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

/**
 * Delegates PDPO logging operations to the async and sync publishers.
 */
@Service
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {

    private final PdpoAsyncPublisher pdpoAsyncPublisher;
    private final PdpoSyncPublisher pdpoSyncPublisher;

    @Override
    public boolean personalDataAccessLogAsync(PersonalDataProcessingLogDetails logDetails) {
        return pdpoAsyncPublisher.publish(logDetails);
    }

    @Override
    public boolean personalDataAccessLogSync(PersonalDataProcessingLogDetails logDetails) {
        return pdpoSyncPublisher.publish(logDetails);
    }
}
