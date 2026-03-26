package org.openl.studio.projects.model.resources;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Base class representing a project resource (file or folder).
 *
 */
@Getter
@SuperBuilder
@JsonPropertyOrder({"path"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FileResource.class, name = "file"),
        @JsonSubTypes.Type(value = FolderResource.class, name = "folder")
})
@Schema(description = "Base resource representing a file or folder in the project")
public abstract class Resource {

    @Schema(description = "Project-relative path (e.g. 'folder/rules.xlsx')")
    private final String path;

    @Schema(description = "Simple file or folder name")
    private final String name;

    @Schema(description = "Parent directory path (project-relative)")
    private final String basePath;
}
