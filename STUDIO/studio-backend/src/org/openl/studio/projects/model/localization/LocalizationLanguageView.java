package org.openl.studio.projects.model.localization;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A language of the messages: a localization file of the project.
 *
 * @param tag    the language, country and variant of the file name joined by underscores, empty for the default file
 * @param legacy {@code true} for a file written in a legacy encoding, which a write turns into UTF-8; absent otherwise
 */
@Schema(description = "A language of the messages: a localization file of the project.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocalizationLanguageView(
        @Parameter(description = """
                Language, country and variant of the file name joined by underscores, as `de_LU`. \
                Empty for the default file.""")
        String tag,

        @Parameter(description = """
                `true` for a file in a legacy encoding rather than UTF-8: the first write of it writes it whole in \
                UTF-8. Absent for a file in UTF-8.""")
        @Nullable Boolean legacy
) {
}
