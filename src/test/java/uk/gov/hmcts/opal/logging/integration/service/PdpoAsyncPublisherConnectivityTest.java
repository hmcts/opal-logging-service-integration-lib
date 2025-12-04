package uk.gov.hmcts.opal.logging.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.opal.logging.integration.Application;
import uk.gov.hmcts.opal.logging.integration.dto.IdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

/**
 * Manual smoke test that can be executed locally once Azure Service Bus credentials are supplied.
 * Skipped automatically when the connection string environment variable is not present.
 */
@SpringBootTest(classes = Application.class)
class PdpoAsyncPublisherConnectivityTest {

    private static final String CONNECTION_STRING_ENV = "LOGGING_PDPL_ASYNC_CONNECTION_STRING";
    private static final String ENABLED_ENV = "LOGGING_PDPL_ASYNC_CONNECTIVITY_TEST_ENABLED";

    @Autowired
    private LoggingService loggingService;

    @Test
    void shouldPublishToRealQueueWhenConnectionStringPresent() {
        assumeTrue(System.getenv(CONNECTION_STRING_ENV) != null,
            CONNECTION_STRING_ENV + " must be defined for this manual connectivity test");
        assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault(ENABLED_ENV, "false")),
            ENABLED_ENV + " must be set to true to run the manual connectivity test");

        PersonalDataProcessingLogDetails details = PersonalDataProcessingLogDetails.builder()
            .businessIdentifier("manual-test-" + UUID.randomUUID())
            .createdAt(OffsetDateTime.now())
            .category(PersonalDataProcessingCategory.COLLECTION)
            .createdBy(ParticipantIdentifier.builder()
                .identifier("manual-user")
                .type(new SimpleIdentifierType("OPAL_USER_ID"))
                .build())
            .ipAddress("127.0.0.1")
            .build();

        boolean result = loggingService.personalDataAccessLogAsync(details);

        assertThat(result).isTrue();
    }

    private record SimpleIdentifierType(String type) implements IdentifierType {
        @Override
        public String getType() {
            return type;
        }
    }
}
