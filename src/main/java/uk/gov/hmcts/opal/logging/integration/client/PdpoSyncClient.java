package uk.gov.hmcts.opal.logging.integration.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.opal.logging.integration.client.config.PdpoSyncFeignConfig;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;

@FeignClient(
    name = "pdpoLoggingClient",
    url = "${logging-service.pdpl.sync.base-url:http://localhost:9999}",
    configuration = PdpoSyncFeignConfig.class
)
public interface PdpoSyncClient {

    @PostMapping("${logging-service.pdpl.sync.endpoint:/log/pdpo}")
    ResponseEntity<Void> logPdpo(@RequestBody PersonalDataProcessingLogDetails logDetails);
}
