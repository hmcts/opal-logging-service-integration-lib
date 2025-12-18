package uk.gov.hmcts.opal.logging.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.opal.logging.integration.Application;

@SpringBootTest(classes = Application.class)
class LoggingServiceContextTest {

    @Autowired
    private LoggingService loggingService;

    @DisplayName("Spring context loads and exposes LoggingService bean")
    @Test
    void shouldExposeLoggingServiceBean() {
        assertThat(loggingService)
            .as("LoggingService bean should be available in the Spring context")
            .isInstanceOf(LoggingServiceImpl.class);
    }
}
