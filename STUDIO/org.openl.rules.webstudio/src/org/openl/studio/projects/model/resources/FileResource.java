package org.openl.studio.projects.model.resources;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a file resource in the project.
 *
 */
@JsonPropertyOrder({"id", "name", "type", "path", "basePath", "extension", "size", "lastModified"})
@JsonDeserialize(builder = FileResource.Builder.class)
@Schema(description = "A file resource in the project")
public class FileResource extends Resource {

    @Schema(description = "File size in bytes")
    public final Long size;

    @Schema(description = "File extension without the dot (e.g., 'xlsx', 'xml')")
    public final String extension;

    @Schema(description = "Last modification timestamp")
    public final ZonedDateTime lastModified;

    private FileResource(Builder builder) {
        super(builder);
        this.size = builder.size;
        this.extension = builder.extension;
        this.lastModified = builder.lastModified;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends Resource.Builder<Builder> {

        private Long size;
        private String extension;
        private ZonedDateTime lastModified;

        private Builder() {
        }

        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        public Builder extension(String extension) {
            this.extension = extension;
            return this;
        }

        public Builder lastModified(ZonedDateTime lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public FileResource build() {
            return new FileResource(this);
        }
    }
}
