package org.openl.studio.projects.service.resources;

import java.util.HashSet;
import java.util.Set;

/**
 * Query criteria for filtering project resources.
 *
 */
public record ResourceCriteriaQuery(

        String basePath,

        Set<String> extensions,

        String namePattern,

        boolean foldersOnly

) {

    private ResourceCriteriaQuery(Builder builder) {
        this(builder.basePath,
                Set.copyOf(builder.extensions),
                builder.namePattern,
                builder.foldersOnly);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String basePath;
        private final Set<String> extensions = new HashSet<>();
        private String namePattern;
        private boolean foldersOnly;

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

        public Builder foldersOnly(boolean foldersOnly) {
            this.foldersOnly = foldersOnly;
            return this;
        }

        public ResourceCriteriaQuery build() {
            return new ResourceCriteriaQuery(this);
        }
    }
}
