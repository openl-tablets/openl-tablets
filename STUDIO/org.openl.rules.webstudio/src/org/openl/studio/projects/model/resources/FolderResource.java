package org.openl.studio.projects.model.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a folder resource in the project.
 *
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonPropertyOrder({"path", "name", "type", "basePath", "children"})
@Schema(description = "A folder resource in the project")
public class FolderResource extends Resource {

    @Schema(description = "Child resources (files and folders). Only populated in NESTED view mode.")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Singular
    private final List<Resource> children;

    /**
     * Creates a copy of this folder with the specified children.
     * Used for building nested structures.
     *
     * @param children the children to set
     * @return a new FolderResource with the specified children
     */
    public FolderResource withChildren(List<Resource> children) {
        return FolderResource.builder()
                .path(getPath())
                .name(getName())
                .basePath(getBasePath())
                .children(children)
                .build();
    }
}
