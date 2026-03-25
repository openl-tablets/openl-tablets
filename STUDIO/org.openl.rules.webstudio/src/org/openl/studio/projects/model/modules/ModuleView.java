package org.openl.studio.projects.model.modules;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * View model for a regular (non-wildcard) module definition with method filter configuration.
 *
 * <p>For wildcard modules, see {@link WildcardModuleView}.</p>
 *
 * @see BaseModuleView
 * @see WildcardModuleView
 */
@SuperBuilder
@Getter
@Schema(description = "Project module definition")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModuleView extends BaseModuleView {

    @Schema(description = "Method filter configuration")
    private final MethodFilterView methodFilter;
}
