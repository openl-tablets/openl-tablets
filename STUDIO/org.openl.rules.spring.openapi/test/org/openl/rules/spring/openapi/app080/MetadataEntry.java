package org.openl.rules.spring.openapi.app080;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadata entry for complex nested objects in ModelAttribute.
 */
@Schema(description = "Key-value metadata entry")
public class MetadataEntry {

    @Schema(description = "Metadata key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String key;

    @Schema(description = "Metadata value", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
