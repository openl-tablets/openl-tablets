package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What writing the table theme into a project did.
 *
 * @param themed  the identifiers of the tables the theme was written into
 * @param skipped the identifiers of the Datatype tables left as they are, because each is written as several
 *                partial tables
 */
@Schema(description = "The tables the table theme was written into, and the ones it left as they are")
public record TableThemeResultView(
        @Parameter(description = "Identifiers of the tables the theme was written into")
        List<String> themed,

        @Parameter(description = "Identifiers of the Datatype tables left as they are, because each is written as several partial tables")
        List<String> skipped
) {
}
