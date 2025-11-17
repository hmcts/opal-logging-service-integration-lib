package uk.gov.hmcts.opal.logging.integration.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersonalDataProcessingLogDetailsTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private enum TestIdentifierType implements IdentifierType {
        OPAL_USER_ID,
        EXTERNAL_SERVICE;

        @Override
        public String getType() {
            return name();
        }
    }

    @Test
    void shouldSerialiseToSnakeCaseJson() throws Exception {
        ParticipantIdentifier createdBy = ParticipantIdentifier.builder()
            .identifier("creator-123")
            .type(TestIdentifierType.OPAL_USER_ID)
            .build();

        ParticipantIdentifier recipient = ParticipantIdentifier.builder()
            .identifier("recipient-456")
            .type(TestIdentifierType.EXTERNAL_SERVICE)
            .build();

        ParticipantIdentifier individual = ParticipantIdentifier.builder()
            .identifier("individual-789")
            .type(TestIdentifierType.EXTERNAL_SERVICE)
            .build();

        OffsetDateTime createdAt = OffsetDateTime.parse("2025-01-10T12:34:56.789Z");

        PersonalDataProcessingLogDetails details = PersonalDataProcessingLogDetails.builder()
            .createdBy(createdBy)
            .businessIdentifier("BUS-999")
            .createdAt(createdAt)
            .ipAddress("192.0.2.1")
            .category(PersonalDataProcessingCategory.DISCLOSURE_TRANSFERS)
            .recipient(recipient)
            .individuals(List.of(individual))
            .build();

        String json = mapper.writeValueAsString(details);

        JsonNode root = mapper.readTree(json);

        assertEquals("creator-123", root.get("created_by").get("identifier").asText());
        assertEquals("OPAL_USER_ID", root.get("created_by").get("type").asText());

        assertEquals("recipient-456", root.get("recipient").get("identifier").asText());
        assertEquals("EXTERNAL_SERVICE", root.get("recipient").get("type").asText());

        assertEquals("BUS-999", root.get("business_identifier").asText());
        assertEquals("192.0.2.1", root.get("ip_address").asText());
        assertEquals("DISCLOSURE_TRANSFERS", root.get("category").asText());
        assertEquals("2025-01-10T12:34:56.789Z", root.get("created_at").asText());

        JsonNode individuals = root.get("individuals");
        assertNotNull(individuals);
        assertEquals(1, individuals.size());
        assertEquals("individual-789", individuals.get(0).get("identifier").asText());
        assertEquals("EXTERNAL_SERVICE", individuals.get(0).get("type").asText());
    }
}
