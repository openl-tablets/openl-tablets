package org.openl.studio.projects.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

/**
 * The parts of a project's {@code rules.xml} the UI cannot work out from the file itself.
 *
 * <p>Everything else in the descriptor is plain declared text the UI reads from the file directly. This
 * carries only what needs the engine and the workspace: the modules a wildcard resolves to, and the
 * source path entries — each with whether it is the engine's default because the file declares none, so
 * a default is shown as such and is never written back into the file. Present only when requested.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DescriptorViewModel(
        @Parameter(description = "Rules modules, one per rules.xml declaration, with each wildcard resolved to its files")
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ModuleViewModel> modules,

        @Parameter(description = "Whether the modules are the engine's defaults because rules.xml declares none. Such modules are not in the file.")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean modulesDefault,

        @Parameter(description = "Source path entries (classpath); the engine's defaults when rules.xml declares none")
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> sources,

        @Parameter(description = "Whether the sources are the engine's defaults because rules.xml declares none. Such sources are not in the file.")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean sourcesDefault) {
}
