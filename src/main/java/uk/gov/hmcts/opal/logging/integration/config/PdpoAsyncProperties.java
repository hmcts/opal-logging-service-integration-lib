package uk.gov.hmcts.opal.logging.integration.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the asynchronous PDPO logging publisher.
 * Values are injected from {@code logging-service.pdpl.async.*}.
 */
@Validated
@ConfigurationProperties(prefix = "logging-service.pdpl.async")
public record PdpoAsyncProperties(
    @DefaultValue("amqps") String protocol,
    @NotBlank String connectionString,
    @NotBlank String queueName,
    @DefaultValue("PDPO") @NotBlank String logType,
    @DefaultValue("3") @Min(1) int maxRetries,
    @DefaultValue("PT1S") Duration retryDelay,
    @DefaultValue("PT10S") Duration sendTimeout
) {
}
