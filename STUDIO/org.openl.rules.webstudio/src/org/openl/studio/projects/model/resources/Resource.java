package org.openl.studio.projects.model.resources;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Base class representing a project resource (file or folder).
 *
 */
@JsonPropertyOrder({"id"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FileResource.class, name = "file"),
        @JsonSubTypes.Type(value = FolderResource.class, name = "folder")
})
@Schema(description = "Base resource representing a file or folder in the project",
        subTypes = {FileResource.class, FolderResource.class},
        discriminatorProperty = "type")
public abstract class Resource {

    @Schema(description = "Unique identifier")
    public final String id;

    @Schema(description = "Simple file or folder name")
    public final String name;

    @Schema(description = "Parent directory path (project-relative)")
    public final String basePath;

    protected Resource(Builder<?> builder) {
        this.id = Objects.requireNonNull(builder.id, "id cannot be null");
        this.name = Objects.requireNonNull(builder.name, "name cannot be null");
        this.basePath = builder.basePath;
    }

    /**
     * Abstract builder with self-referencing generic for inheritance support.
     *
     * @param <T> the concrete builder type
     */
    public abstract static class Builder<T extends Builder<T>> {

        String id;
        String name;
        String basePath;

        protected Builder() {
        }

        public T id(String id) {
            this.id = id;
            return self();
        }

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T basePath(String basePath) {
            this.basePath = basePath;
            return self();
        }

        protected abstract T self();

        public abstract Resource build();
    }
}
