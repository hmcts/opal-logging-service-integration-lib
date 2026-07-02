package uk.gov.hmcts.opal.logging.integration.messaging;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import uk.gov.hmcts.opal.logging.integration.dto.IdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

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

    public static PdpoQueueLogDetails fromLogDetails(PersonalDataProcessingLogDetails details) {
        if (details == null) {
            return null;
        }
        return new PdpoQueueLogDetails(
            details.getCreatedBy(),
            details.getBusinessIdentifier(),
            details.getCreatedAt(),
            details.getIpAddress(),
            details.getCategory(),
            details.getRecipient(),
            compactIndividuals(details.getIndividuals()));
    }

    private static Map<String, List<String>> compactIndividuals(List<ParticipantIdentifier> individuals) {
        if (individuals == null || individuals.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> groupedIndividuals = new LinkedHashMap<>();
        individuals.stream()
            .filter(Objects::nonNull)
            .forEach(individual -> {
                String type = resolveType(individual.getType());
                groupedIndividuals.computeIfAbsent(type, ignored -> new java.util.ArrayList<>())
                    .add(individual.getIdentifier());
            });
        return groupedIndividuals;
    }

    private static String resolveType(IdentifierType type) {
        return type == null ? null : type.getType();
    }
}
