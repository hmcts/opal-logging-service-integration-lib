package uk.gov.hmcts.opal.logging.integration.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.opal.logging.integration.dto.IdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;
import uk.gov.hmcts.opal.logging.integration.messaging.PdpoQueueLogDetails;

class PdpoQueueLogDetailsMapperTest {

    private final PdpoQueueLogDetailsMapper pdpoQueueLogDetailsMapper =
        Mappers.getMapper(PdpoQueueLogDetailsMapper.class);

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    @Test
    void shouldMapLogDetailsToQueueLogDetails() {
        ParticipantIdentifier createdBy = participant("creator-123", "OPAL_USER_ID");
        ParticipantIdentifier recipient = participant("recipient-456", "EXTERNAL_SERVICE");
        OffsetDateTime createdAt = OffsetDateTime.parse("2025-01-10T12:34:56.789Z");
        PersonalDataProcessingLogDetails details = PersonalDataProcessingLogDetails.builder()
            .createdBy(createdBy)
            .businessIdentifier("BUS-999")
            .createdAt(createdAt)
            .ipAddress("192.0.2.1")
            .category(PersonalDataProcessingCategory.DISCLOSURE)
            .recipient(recipient)
            .individuals(List.of(participant("individual-1", "DEFENDANT")))
            .build();

        PdpoQueueLogDetails result = pdpoQueueLogDetailsMapper.toQueueLogDetails(details);

        assertThat(result.createdBy()).isEqualTo(createdBy);
        assertThat(result.businessIdentifier()).isEqualTo("BUS-999");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.ipAddress()).isEqualTo("192.0.2.1");
        assertThat(result.category()).isEqualTo(PersonalDataProcessingCategory.DISCLOSURE);
        assertThat(result.recipient()).isEqualTo(recipient);
        assertThat(result.individuals()).containsExactly(entry("DEFENDANT", List.of("individual-1")));
    }

    @Test
    void shouldGroupIndividualsByTypeInQueuePayload() {
        PersonalDataProcessingLogDetails details = sampleDetailsWithGroupedIndividuals();

        PdpoQueueLogDetails result = pdpoQueueLogDetailsMapper.toQueueLogDetails(details);

        assertThat(result.individuals()).containsExactly(
            entry("DEFENDANT", List.of("individual-1", "individual-2")),
            entry("MINOR_CREDITOR", List.of("individual-3")));
    }

    @Test
    void shouldSerialiseCompactIndividualsForQueuePayload() {
        PersonalDataProcessingLogDetails details = sampleDetailsWithGroupedIndividuals();

        String json = objectMapper.writeValueAsString(pdpoQueueLogDetailsMapper.toQueueLogDetails(details));
        JsonNode individuals = objectMapper.readTree(json).get("individuals");

        assertFalse(individuals.isArray());
        assertThat(individuals.get("DEFENDANT").get(0).stringValue()).isEqualTo("individual-1");
        assertThat(individuals.get("DEFENDANT").get(1).stringValue()).isEqualTo("individual-2");
        assertThat(individuals.get("MINOR_CREDITOR").get(0).stringValue()).isEqualTo("individual-3");
    }

    @Test
    void shouldMapNullAndEmptyIndividualsToEmptyMap() {
        assertThat(pdpoQueueLogDetailsMapper.mapIndividuals(null)).isEmpty();
        assertThat(pdpoQueueLogDetailsMapper.mapIndividuals(List.of())).isEmpty();
    }

    @Test
    void shouldIgnoreNullIndividuals() {
        List<ParticipantIdentifier> individuals = new ArrayList<>();
        individuals.add(participant("individual-1", "DEFENDANT"));
        individuals.add(null);
        individuals.add(participant("individual-2", "DEFENDANT"));

        assertThat(pdpoQueueLogDetailsMapper.mapIndividuals(individuals))
            .containsExactly(entry("DEFENDANT", List.of("individual-1", "individual-2")));
    }

    @Test
    void shouldMapNullDetailsToNull() {
        assertThat(pdpoQueueLogDetailsMapper.toQueueLogDetails(null)).isNull();
    }

    private ParticipantIdentifier participant(String identifier, String type) {
        return ParticipantIdentifier.builder()
            .identifier(identifier)
            .type(new TestIdentifierType(type))
            .build();
    }

    private PersonalDataProcessingLogDetails sampleDetailsWithGroupedIndividuals() {
        return PersonalDataProcessingLogDetails.builder()
            .individuals(List.of(
                participant("individual-1", "DEFENDANT"),
                participant("individual-2", "DEFENDANT"),
                participant("individual-3", "MINOR_CREDITOR")
            ))
            .build();
    }

    private record TestIdentifierType(String type) implements IdentifierType {
        @Override
        public String getType() {
            return type;
        }
    }
}
