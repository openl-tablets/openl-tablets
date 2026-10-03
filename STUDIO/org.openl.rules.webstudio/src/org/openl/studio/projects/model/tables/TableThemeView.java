package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A table theme OpenL Studio offers.
 *
 * @param id   the identifier the theme is asked for by: the name of its file without the extension
 * @param name the name the theme is shown by
 */
@Schema(description = "A table theme OpenL Studio offers")
public record TableThemeView(
        @Parameter(description = "Identifier the theme is asked for by: the name of its file without the extension")
        String id,

        @Parameter(description = "Name the theme is shown by")
        String name
) {
}
