package org.openl.studio.projects.service.resources;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Query criteria for filtering project resources.
 *
 */
@JsonDeserialize(builder = ResourceCriteriaQuery.Builder.class)
public record ResourceCriteriaQuery(

        @Schema(description = "Base path to start listing resources from. If not specified, starts from project root.")
        String basePath,

        @Schema(description = "Filter by file extensions (without dot). Example: xlsx, xml")
        Set<String> extensions,

        @Schema(description = "Filter by name pattern (case-insensitive contains match)")
        String namePattern

) {

    private ResourceCriteriaQuery(Builder builder) {
        this(builder.basePath,
                builder.extensions.isEmpty() ? null : Set.copyOf(builder.extensions),
                builder.namePattern);
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private String basePath;
        private final Set<String> extensions = new HashSet<>();
        private String namePattern;

        private Builder() {
        }

        public Builder basePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public Builder extensions(Set<String> extensions) {
            if (extensions != null) {
                this.extensions.addAll(extensions);
            }
            return this;
        }

        public Builder extension(String extension) {
            if (extension != null) {
                this.extensions.add(extension);
            }
            return this;
        }

        public Builder namePattern(String namePattern) {
            this.namePattern = namePattern;
            return this;
        }

        public ResourceCriteriaQuery build() {
            return new ResourceCriteriaQuery(this);
        }
    }
}
