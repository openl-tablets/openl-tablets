package org.openl.studio.projects.model.modules;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View model for a module's method filter configuration.
 */
@Schema(description = "Method filter configuration for a module")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MethodFilterView {

    @Schema(description = "Include patterns for method filtering")
    public final List<String> includes;

    @Schema(description = "Exclude patterns for method filtering")
    public final List<String> excludes;

    private MethodFilterView(Builder builder) {
        this.includes = builder.includes != null ? List.copyOf(builder.includes) : null;
        this.excludes = builder.excludes != null ? List.copyOf(builder.excludes) : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Collection<String> includes;
        private Collection<String> excludes;

        private Builder() {
        }

        public Builder includes(Collection<String> includes) {
            this.includes = includes;
            return this;
        }

        public Builder excludes(Collection<String> excludes) {
            this.excludes = excludes;
            return this;
        }

        public MethodFilterView build() {
            return new MethodFilterView(this);
        }
    }
}
