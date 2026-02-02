package uk.gov.hmcts.opal.logging.integration.client.config;

import feign.Request;
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
            true
        );
    }
}
