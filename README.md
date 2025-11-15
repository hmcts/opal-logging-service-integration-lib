# Opal Logging Service Integration Library

Library that supplies Spring components and DTOs used by Opal services when emitting Personal Data Processing Log (PDPL) entries. It currently ships placeholder service wiring plus the PDPL payload model so downstream services can start building against the contract before the concrete integration exists.

## Contents
- `LoggingService` and `LoggingServiceImpl`: empty Spring beans (the implementation is annotated with `@Service`) so consuming services can inject the dependency now. Behaviour will be added once the logging service API is finalised.
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

## Building & Testing

Standard Gradle lifecycle applies:

```bash
./gradlew build
```

Additional source sets (`integration`, `smoke`, `functional`) are available if you add tests for the integration entry points.
