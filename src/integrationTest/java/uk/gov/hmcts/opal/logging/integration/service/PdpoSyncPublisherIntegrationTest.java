package uk.gov.hmcts.opal.logging.integration.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.opal.logging.integration.Application;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

@SpringBootTest(classes = Application.class)
class PdpoSyncPublisherIntegrationTest {

    private static final String PDPO_ENDPOINT = "/log/pdpo";
    private static final String RETRY_SCENARIO = "pdpo-retry";

    @RegisterExtension
    static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
        .options(WireMockConfiguration.wireMockConfig().dynamicPort())
        .build();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("logging-service.pdpl.sync.base-url", () -> WIREMOCK.getRuntimeInfo().getHttpBaseUrl());
        registry.add("logging-service.pdpl.sync.max-attempts", () -> "3");
        registry.add("logging-service.pdpl.sync.retry-delay", () -> "PT0.01S");
        registry.add("logging-service.pdpl.sync.endpoint", () -> PDPO_ENDPOINT);
    }

    private final LoggingService loggingService;

    PdpoSyncPublisherIntegrationTest(@Autowired LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    @BeforeEach
    void resetWireMock() {
        WIREMOCK.resetAll();
    }

    @Test
    void shouldSendLogSynchronously() {
        WIREMOCK.stubFor(post(urlEqualTo(PDPO_ENDPOINT))
            .willReturn(aResponse().withStatus(201)));

        boolean result = loggingService.personalDataAccessLogSync(sampleDetails());

        assertThat(result).isTrue();
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(PDPO_ENDPOINT)));
    }

    @Test
    void shouldRetryOnServerErrorThenSucceed() {
        WIREMOCK.stubFor(post(urlEqualTo(PDPO_ENDPOINT))
            .inScenario(RETRY_SCENARIO)
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("Second"));
        WIREMOCK.stubFor(post(urlEqualTo(PDPO_ENDPOINT))
            .inScenario(RETRY_SCENARIO)
            .whenScenarioStateIs("Second")
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("Third"));
        WIREMOCK.stubFor(post(urlEqualTo(PDPO_ENDPOINT))
            .inScenario(RETRY_SCENARIO)
            .whenScenarioStateIs("Third")
            .willReturn(aResponse().withStatus(201)));

        boolean result = loggingService.personalDataAccessLogSync(sampleDetails());

        assertThat(result).isTrue();
        WIREMOCK.verify(3, postRequestedFor(urlEqualTo(PDPO_ENDPOINT)));
    }

    @Test
    void shouldReturnFalseAfterRetriesExhausted() {
        WIREMOCK.stubFor(post(urlEqualTo(PDPO_ENDPOINT))
            .willReturn(aResponse().withStatus(500)));

        boolean result = loggingService.personalDataAccessLogSync(sampleDetails());

        assertThat(result).isFalse();
        WIREMOCK.verify(3, postRequestedFor(urlEqualTo(PDPO_ENDPOINT)));
    }

    private PersonalDataProcessingLogDetails sampleDetails() {
        ParticipantIdentifier createdBy = ParticipantIdentifier.builder()
            .identifier("creator-1")
            .build();

        return PersonalDataProcessingLogDetails.builder()
            .createdBy(createdBy)
            .businessIdentifier("BUS-123")
            .createdAt(OffsetDateTime.parse("2025-01-10T12:34:56.789Z"))
            .ipAddress("192.0.2.1")
            .category(PersonalDataProcessingCategory.COLLECTION)
            .build();
    }
}
