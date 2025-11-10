package uk.gov.hmcts.opal.logging.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an individual whose personal data is included in a PDPO log entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalDataProcessingLogIndividual {

    private String individualIdentifier;

    private String individualType;
}
