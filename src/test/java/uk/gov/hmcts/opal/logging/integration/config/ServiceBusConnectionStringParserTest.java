package uk.gov.hmcts.opal.logging.integration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ServiceBusConnectionStringParserTest {

    private static final String CONNECTION_STRING =
        "Endpoint=sb://example.servicebus.windows.net/;"
            + "SharedAccessKeyName=OpalPdpo;"
            + "SharedAccessKey=superSecret="; // '=' in key should still be handled

    @Test
    void shouldParseValidConnectionString() {
        ServiceBusConnectionStringParser.ConnectionDetails details =
            ServiceBusConnectionStringParser.parse(CONNECTION_STRING);

        assertThat(details.fullyQualifiedNamespace()).isEqualTo("example.servicebus.windows.net");
        assertThat(details.sharedAccessKeyName()).isEqualTo("OpalPdpo");
        assertThat(details.sharedAccessKey()).isEqualTo("superSecret=");
    }

    @Test
    void shouldRejectMissingSegments() {
        assertThatThrownBy(() -> ServiceBusConnectionStringParser.parse("SharedAccessKeyName=Opal"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Endpoint");

        assertThatThrownBy(() -> ServiceBusConnectionStringParser.parse("Endpoint=sb://example/"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SharedAccessKeyName");

        assertThatThrownBy(() -> ServiceBusConnectionStringParser.parse(
            "Endpoint=sb://example/;SharedAccessKeyName=Opal"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SharedAccessKey");
    }
}
