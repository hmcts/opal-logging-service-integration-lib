package uk.gov.hmcts.opal.logging.integration.client.config;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.opal.logging.integration.config.PdpoSyncProperties;

@Configuration
public class PdpoSyncFeignConfig {

    @Bean
    public Request.Options pdpoSyncRequestOptions(PdpoSyncProperties properties) {
        return new Request.Options(
            properties.connectTimeout(),
            properties.readTimeout(),
            false
        );
    }

    @Bean
    public Retryer pdpoSyncRetryer(PdpoSyncProperties properties) {
        long delayMillis = toMillisOrDefault(properties.retryDelay(), 1000L);
        return new Retryer.Default(
            delayMillis,
            delayMillis,
            properties.maxRetries()
        );
    }

    @Bean
    public ErrorDecoder pdpoSyncErrorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            int status = response.status();
            if (status >= 500 || status == 429) {
                return new RetryableException(
                    status,
                    "Retryable PDPO response status=" + status,
                    response.request().httpMethod(),
                    (Long) null,
                    response.request()
                );
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }

    private long toMillisOrDefault(Duration duration, long fallbackMillis) {
        if (duration == null) {
            return fallbackMillis;
        }
        long millis = duration.toMillis();
        return millis > 0 ? millis : fallbackMillis;
    }
}
