package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What formatting the Excel files of a project did.
 *
 * @param formatted the identifiers of the tables formatted
 * @param skipped   the identifiers of the tables left as they are, because each is written as several partial tables
 */
@Schema(description = "The tables formatted with the OpenL table style, and the ones left as they are")
public record TableFormatResultView(
        @Parameter(description = "Identifiers of the tables formatted")
        List<String> formatted,

        @Parameter(description = "Identifiers of the tables left as they are, each written as several partial tables")
        List<String> skipped
) {
}
