package org.openl.studio.projects.model.modules;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Base view model for a project module containing only identity fields.
 *
 * <p>Used for matched modules inside {@link WildcardModuleView} that do not
 * have their own method filter configuration.</p>
 *
 * @see ModuleView
 * @see WildcardModuleView
 */
@Schema(description = "Base module information")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseModuleView {

    @Schema(description = "Module name")
    private final String name;

    @Schema(description = "File path relative to project root")
    private final String path;

    protected BaseModuleView(ABuilder<?> builder) {
        this.name = builder.name;
        this.path = builder.path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    /**
     * Abstract builder base for {@link BaseModuleView} and its subclasses.
     *
     * @param <T> the concrete builder type
     */
    public abstract static class ABuilder<T extends ABuilder<T>> {

        private String name;
        private String path;

        protected ABuilder() {
            // Created via concrete builder subclass
        }

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T path(String path) {
            this.path = path;
            return self();
        }

        protected abstract T self();
    }

    /**
     * Concrete builder for {@link BaseModuleView}.
     */
    public static class Builder extends ABuilder<Builder> {

        public Builder() {
            // No fields to initialize; all set via fluent methods
        }

        @Override
        protected Builder self() {
            return this;
        }

        public BaseModuleView build() {
            return new BaseModuleView(this);
        }
    }
}
