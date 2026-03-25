package org.openl.studio.projects.model.modules;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Base view model for a project module containing only identity fields.
 *
 * <p>Used for matched modules inside {@link WildcardModuleView} that do not
 * have their own method filter configuration.</p>
 *
 * @see ModuleView
 * @see WildcardModuleView
 */
@SuperBuilder
@Getter
@Schema(description = "Base module information")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseModuleView {

    @Schema(description = "Module name")
    private final String name;

    @Schema(description = "File path relative to project root")
    private final String path;
}
