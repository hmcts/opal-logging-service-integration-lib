package uk.gov.hmcts.opal.logging.integration.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the synchronous PDPO logging client.
 * Values are injected from {@code logging-service.pdpl.sync.*}.
 */
@Validated
@ConfigurationProperties(prefix = "logging-service.pdpl.sync")
public record PdpoSyncProperties(
    @DefaultValue("http://localhost:9999") @NotBlank String baseUrl,
    @DefaultValue("/log/pdpo") @NotBlank String endpoint,
    @DefaultValue("4") @Min(1) int maxAttempts,
    @DefaultValue("PT15S") Duration retryDelay,
    @DefaultValue("PT2S") Duration connectTimeout,
    @DefaultValue("PT5S") Duration readTimeout
) {
}
