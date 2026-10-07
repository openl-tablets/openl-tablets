package org.openl.studio.projects.model.files;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a file in a mount.
 *
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonPropertyOrder({"path", "name", "type", "basePath", "extension", "size", "lastModified", "content"})
@Schema(description = "A file resource in the mount")
public class FileNode extends FsNode {

    @Schema(description = "File size in bytes")
    private final Long size;

    @Schema(description = "File extension without the dot (e.g., 'xlsx', 'xml')")
    private final String extension;

    @Schema(description = "Last modification timestamp")
    private final ZonedDateTime lastModified;

    /**
     * Raw UTF-8 file content. Populated only by content-bearing lookups; omitted otherwise.
     */
    @Schema(description = "Raw file content (UTF-8). Present only for content-bearing lookups.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String content;

    @Override
    public String getType() {
        return "file";
    }
}
