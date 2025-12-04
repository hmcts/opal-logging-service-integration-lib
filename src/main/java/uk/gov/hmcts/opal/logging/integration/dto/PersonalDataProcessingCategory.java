package uk.gov.hmcts.opal.logging.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/**
 * Enumerates the set of categories supported by PDPO logging.
 * Serialises using the exact strings defined in the logging service OpenAPI contract.
 */
public enum PersonalDataProcessingCategory {
    COLLECTION("Collection"),
    ALTERATION("Alteration"),
    CONSULTATION("Consultation"),
    DISCLOSURE("Disclosure"),
    COMBINATION("Combination"),
    ERASURE("Erasure");

    private final String jsonValue;

    PersonalDataProcessingCategory(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static PersonalDataProcessingCategory fromJsonValue(String value) {
        return Arrays.stream(values())
            .filter(v -> v.jsonValue.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown PDPO category: " + value));
    }
}
