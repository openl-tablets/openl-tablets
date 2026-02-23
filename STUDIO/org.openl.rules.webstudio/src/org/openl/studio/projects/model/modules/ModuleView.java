package org.openl.studio.projects.model.modules;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View model for a regular (non-wildcard) module definition with method filter configuration.
 *
 * <p>For wildcard modules, see {@link WildcardModuleView}.</p>
 *
 * @see BaseModuleView
 * @see WildcardModuleView
 */
@Schema(description = "Project module definition")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModuleView extends BaseModuleView {

    @Schema(description = "Method filter configuration")
    public final MethodFilterView methodFilter;

    protected ModuleView(ABuilder<?> builder) {
        super(builder);
        this.methodFilter = builder.methodFilter;
    }

    /**
     * Abstract builder base for {@link ModuleView} and its subclasses.
     *
     * @param <T> the concrete builder type
     */
    public abstract static class ABuilder<T extends ABuilder<T>> extends BaseModuleView.ABuilder<T> {

        private MethodFilterView methodFilter;

        protected ABuilder() {
        }

        public T methodFilter(MethodFilterView methodFilter) {
            this.methodFilter = methodFilter;
            return self();
        }
    }

    /**
     * Concrete builder for {@link ModuleView}.
     */
    public static class Builder extends ABuilder<Builder> {

        public Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        public ModuleView build() {
            return new ModuleView(this);
        }
    }
}
