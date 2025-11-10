package uk.gov.hmcts.opal.logging.integration.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Captures personal data processing logging details for transmission to the logging service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalDataProcessingLogDetails {

    private String createdByIdentifier;

    private String createdByIdentifierType;

    private String businessIdentifier;

    private OffsetDateTime createdAt;

    private String ipAddress;

    private String category;

    private String recipientIdentifier;

    private String recipientIdentifierType;

    @Builder.Default
    private List<PersonalDataProcessingLogIndividual> individuals = new ArrayList<>();
}
