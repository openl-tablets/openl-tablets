package org.openl.studio.projects.model.files;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a folder in a mount.
 *
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonPropertyOrder({"path", "name", "type", "basePath", "children"})
@Schema(description = "A folder resource in the mount")
public class FolderNode extends FsNode {

    @Schema(description = "Child resources (files and folders). Only populated in NESTED view mode.")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Singular
    private final List<FsNode> children;

    /**
     * Creates a copy of this folder with the specified children.
     * Used for building nested structures.
     *
     * @param children the children to set
     * @return a new FolderNode with the specified children
     */
    public FolderNode withChildren(List<FsNode> children) {
        return FolderNode.builder()
                .path(getPath())
                .name(getName())
                .basePath(getBasePath())
                .children(children)
                .build();
    }

    @Override
    public String getType() {
        return "folder";
    }
}
