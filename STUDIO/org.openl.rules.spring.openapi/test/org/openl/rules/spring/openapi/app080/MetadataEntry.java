package org.openl.rules.spring.openapi.app080;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadata entry for complex nested objects in ModelAttribute.
 */
@Schema(description = "Key-value metadata entry")
public record MetadataEntry(
        @Schema(description = "Metadata key", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String key,
        @Schema(description = "Metadata value", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String value
) {
}
