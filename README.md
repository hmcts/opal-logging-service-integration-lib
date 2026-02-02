# Opal Logging Service Integration Library

Library that supplies Spring components and DTOs used by Opal services when emitting Personal Data Processing Log (PDPL) entries. It ships both the PDPL payload model and the client integrations required to send logs asynchronously (queue) or synchronously (HTTP).

## Contents
- `LoggingService` and `LoggingServiceImpl`: Spring beans exposing `personalDataAccessLogAsync` (queue) and `personalDataAccessLogSync` (HTTP).
- `PersonalDataProcessingLogDetails`: DTO representing the PDPL payload. Fields are annotated to serialise in snake_case and `created_at` is formatted as ISO 8601.
- `ParticipantIdentifier`: wraps a participant identifier and its type.
- `IdentifierType`: interface that downstream services implement (typically as enums) to describe their identifier types.
- `PersonalDataProcessingCategory`: enum containing the supported PDPL categories.

## Identifier Types

The library purposefully does **not** ship concrete enums for identifier types. Each service can have different domain concepts for `createdBy`, `recipient`, and `individuals`, so consumers are expected to supply their own enums implementing `IdentifierType`. This keeps the JSON payload consistent while allowing type-safe identifiers.

Example:

```java
public enum UserIdentifierType implements IdentifierType {
    OPAL_USER_ID,
    EXTERNAL_SERVICE;

    @Override
    public String getType() {
        return name(); // or a custom label per constant
    }
}

ParticipantIdentifier requester = ParticipantIdentifier.builder()
    .identifier("example-user-id")
    .type(UserIdentifierType.OPAL_USER_ID)
    .build();
```

You can define separate enums for recipients or individuals if their value sets differ.

## LoggingService usage

Inject `LoggingService` and call the required integration method:

```java
boolean queued = loggingService.personalDataAccessLogAsync(details);
boolean stored = loggingService.personalDataAccessLogSync(details);
```

The synchronous call posts to `/log/pdpo`. Feign clients must be enabled for the synchronous integration
(e.g. add `@EnableFeignClients(basePackages = "uk.gov.hmcts.opal.logging.integration")`).
In current environments the logging service only exposes this endpoint when
`opal.logging.test-support.enabled=true`.

## Configuration

### Async (queue)

Prefix: `logging-service.pdpl.async.*`

- `connection-string`
- `queue-name`
- `log-type`
- `max-retries`
- `retry-delay`
- `send-timeout`

### Sync (HTTP)

Prefix: `logging-service.pdpl.sync.*`

- `base-url`
- `endpoint` (default `/log/pdpo`)
- `max-retries` (defaults to 4 total attempts: 1 initial + 3 retries)
- `retry-delay`
- `connect-timeout`
- `read-timeout`

## Building & Testing

Standard Gradle lifecycle applies:

```bash
./gradlew build
```

Additional source sets (`integration`, `smoke`, `functional`) are available if you add tests for the integration entry points.

### Manual Azure Service Bus Connectivity Test

You can exercise a real Service Bus queue end-to-end using the manual smoke test
`PdpoAsyncPublisherConnectivityTest`. This is useful when validating the library against a
namespace such as `opal-servicebus-stg`.

1. Obtain the queue credentials (connection string + queue name) from the Azure Key Vault secrets
   `servicebus-connection-string` and `servicebus-logging-pdpl-queue-name`.
2. Export them as environment variables (or add them to your shell profile):
   ```bash
   export LOGGING_PDPL_ASYNC_CONNECTION_STRING='Endpoint=sb://opal-servicebus-stg.servicebus.windows.net/...'
   export LOGGING_PDPL_ASYNC_QUEUE='logging-pdpl'
   ```
3. Opt in to the manual test by setting `LOGGING_PDPL_ASYNC_CONNECTIVITY_TEST_ENABLED=true`.
4. Run one of the manual tests:
   ```bash
   LOGGING_PDPL_ASYNC_CONNECTIVITY_TEST_ENABLED=true ./gradlew test \
     --tests uk.gov.hmcts.opal.logging.integration.service.PdpoAsyncPublisherConnectivityTest
   LOGGING_PDPL_ASYNC_CONNECTIVITY_TEST_ENABLED=true ./gradlew test \
     --tests uk.gov.hmcts.opal.logging.integration.service.PdpoQueuePeekTest
   ```

The first command enqueues a PDPO payload. The second command peeks the queue and prints any
messages that are currently stored, which is useful when the Azure portal cannot display them. When the opt-in flag is absent the tests are skipped automatically so
regular CI runs do not depend on external infrastructure.
