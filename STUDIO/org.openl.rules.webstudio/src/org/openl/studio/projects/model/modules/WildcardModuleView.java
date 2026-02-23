package org.openl.studio.projects.model.modules;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View model representing a wildcard module definition with its matched concrete modules.
 *
 * <p>Wildcard modules use path patterns (e.g. {@code rules/*.xlsx}) to auto-discover
 * matching files. The {@link #matchedModules} list contains the concrete modules
 * resolved from the wildcard pattern.</p>
 *
 * @see BaseModuleView
 * @see ModuleView
 */
@Schema(description = "Wildcard module with matched concrete modules")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WildcardModuleView extends ModuleView {

    @Schema(description = "Concrete modules matching the wildcard path pattern")
    public final List<BaseModuleView> matchedModules;

    private WildcardModuleView(Builder builder) {
        super(builder);
        this.matchedModules = builder.matchedModules != null ? List.copyOf(builder.matchedModules) : List.of();
    }

    /**
     * Builder for {@link WildcardModuleView}.
     */
    public static class Builder extends ModuleView.ABuilder<Builder> {

        private List<BaseModuleView> matchedModules;

        public Builder() {
        }

        public Builder addMatchedModule(BaseModuleView module) {
            if (matchedModules == null) {
                matchedModules = new ArrayList<>();
            }
            matchedModules.add(module);
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public WildcardModuleView build() {
            return new WildcardModuleView(this);
        }
    }
}
