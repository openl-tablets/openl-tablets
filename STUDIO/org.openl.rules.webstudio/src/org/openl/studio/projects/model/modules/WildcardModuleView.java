package org.openl.studio.projects.model.modules;

import java.util.List;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder
@Getter
@Schema(description = "Wildcard module with matched concrete modules")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WildcardModuleView extends ModuleView {

    @NotNull
    @Singular
    @Schema(description = "Concrete modules matching the wildcard path pattern")
    private final List<BaseModuleView> matchedModules;
}
