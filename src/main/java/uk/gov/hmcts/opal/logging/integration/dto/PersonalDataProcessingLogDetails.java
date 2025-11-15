package uk.gov.hmcts.opal.logging.integration.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Captures personal data processing logging details for transmission to the logging service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalDataProcessingLogDetails {
    @JsonProperty("created_by")
    private ParticipantIdentifier createdBy;

    @JsonProperty("business_identifier")
    private String businessIdentifier;

    @JsonProperty("created_at")
    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    private OffsetDateTime createdAt;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("category")
    private PersonalDataProcessingCategory category;

    @JsonProperty("recipient")
    private ParticipantIdentifier recipient;

    @JsonProperty("individuals")
    @Builder.Default
    private List<ParticipantIdentifier> individuals = new ArrayList<>();
}
