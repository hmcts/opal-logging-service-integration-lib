package uk.gov.hmcts.opal.logging.integration.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.opal.logging.integration.dto.IdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;
import uk.gov.hmcts.opal.logging.integration.messaging.PdpoQueueLogDetails;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PdpoQueueLogDetailsMapper {

    PdpoQueueLogDetails toQueueLogDetails(PersonalDataProcessingLogDetails details);

    default Map<String, List<String>> mapIndividuals(List<ParticipantIdentifier> individuals) {
        if (individuals == null || individuals.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> groupedIndividuals = new LinkedHashMap<>();
        individuals.stream()
            .filter(Objects::nonNull)
            .forEach(individual -> {
                String type = resolveType(individual.getType());
                groupedIndividuals.computeIfAbsent(type, ignored -> new ArrayList<>())
                    .add(individual.getIdentifier());
            });
        return groupedIndividuals;
    }

    private String resolveType(IdentifierType type) {
        return type == null ? null : type.getType();
    }
}
