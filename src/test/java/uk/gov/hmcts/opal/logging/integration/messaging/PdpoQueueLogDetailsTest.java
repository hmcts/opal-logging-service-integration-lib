package uk.gov.hmcts.opal.logging.integration.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.opal.logging.integration.dto.IdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

class PdpoQueueLogDetailsTest {

    private final ObjectMapper mapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    @Test
    void shouldSerialiseCompactIndividualsForQueuePayload() {
        PersonalDataProcessingLogDetails details = PersonalDataProcessingLogDetails.builder()
            .createdBy(participant("creator-123", "OPAL_USER_ID"))
            .businessIdentifier("BUS-999")
            .createdAt(OffsetDateTime.parse("2025-01-10T12:34:56.789Z"))
            .ipAddress("192.0.2.1")
            .category(PersonalDataProcessingCategory.COLLECTION)
            .individuals(List.of(
                participant("individual-1", "DEFENDANT"),
                participant("individual-2", "DEFENDANT"),
                participant("individual-3", "MINOR_CREDITOR")
            ))
            .build();

        String json = mapper.writeValueAsString(PdpoQueueLogDetails.fromLogDetails(details));
        JsonNode root = mapper.readTree(json);

        JsonNode individuals = root.get("individuals");
        assertFalse(individuals.isArray());
        assertEquals("individual-1", individuals.get("DEFENDANT").get(0).stringValue());
        assertEquals("individual-2", individuals.get("DEFENDANT").get(1).stringValue());
        assertEquals("individual-3", individuals.get("MINOR_CREDITOR").get(0).stringValue());
    }

    private ParticipantIdentifier participant(String identifier, String type) {
        return ParticipantIdentifier.builder()
            .identifier(identifier)
            .type(new TestIdentifierType(type))
            .build();
    }

    private record TestIdentifierType(String type) implements IdentifierType {
        @Override
        public String getType() {
            return type;
        }
    }
}
