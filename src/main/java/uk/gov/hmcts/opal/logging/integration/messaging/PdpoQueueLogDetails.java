package uk.gov.hmcts.opal.logging.integration.messaging;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;

/**
 * Queue-specific PDPO payload that compacts individuals by identifier type.
 */
public record PdpoQueueLogDetails(
    @JsonProperty("created_by")
    ParticipantIdentifier createdBy,
    @JsonProperty("business_identifier")
    String businessIdentifier,
    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime createdAt,
    @JsonProperty("ip_address")
    String ipAddress,
    @JsonProperty("category")
    PersonalDataProcessingCategory category,
    @JsonProperty("recipient")
    ParticipantIdentifier recipient,
    @JsonProperty("individuals")
    Map<String, List<String>> individuals) {
}
