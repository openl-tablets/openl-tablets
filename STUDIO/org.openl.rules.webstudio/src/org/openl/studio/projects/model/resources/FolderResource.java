package org.openl.studio.projects.model.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a folder resource in the project.
 *
 */
@JsonPropertyOrder({"id", "name", "type", "path", "basePath", "children"})
@JsonDeserialize(builder = FolderResource.Builder.class)
@Schema(description = "A folder resource in the project")
public class FolderResource extends Resource {

    @Schema(description = "Child resources (files and folders). Only populated in NESTED view mode.")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public final List<Resource> children;

    private FolderResource(Builder builder) {
        super(builder);
        this.children = builder.children != null
                ? List.copyOf(builder.children)
                : List.of();
    }

    /**
     * Creates a copy of this folder with the specified children.
     * Used for building nested structures.
     *
     * @param children the children to set
     * @return a new FolderResource with the specified children
     */
    public FolderResource withChildren(List<Resource> children) {
        return new Builder()
                .id(this.id)
                .name(this.name)
                .basePath(this.basePath)
                .children(children)
                .build();
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends Resource.Builder<Builder> {

        private List<Resource> children;

        private Builder() {
        }

        public Builder children(List<Resource> children) {
            this.children = children;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public FolderResource build() {
            return new FolderResource(this);
        }
    }
}
