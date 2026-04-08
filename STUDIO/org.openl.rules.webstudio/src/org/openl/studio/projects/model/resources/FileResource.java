package org.openl.studio.projects.model.resources;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a file resource in the project.
 *
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonPropertyOrder({"path", "name", "type", "basePath", "extension", "size", "lastModified"})
@Schema(description = "A file resource in the project")
public class FileResource extends Resource {

    @Schema(description = "File size in bytes")
    private final Long size;

    @Schema(description = "File extension without the dot (e.g., 'xlsx', 'xml')")
    private final String extension;

    @Schema(description = "Last modification timestamp")
    private final ZonedDateTime lastModified;
}
