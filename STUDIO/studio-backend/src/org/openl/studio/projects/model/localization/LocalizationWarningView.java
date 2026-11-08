package org.openl.studio.projects.model.localization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.project.status.TextSpot;

/**
 * What the check of the localization files warns about the value of a key in a language.
 *
 * @param language the language the value is written in
 * @param summary  what is wrong
 * @param spots    the pieces of the value the warning is about; absent for the value as a whole
 */
@Schema(description = "A warning of the check of the localization files about the value of a key in a language.")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record LocalizationWarningView(
        @Parameter(description = "Language of the value, empty for the default file.")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        String language,

        @Parameter(description = "What is wrong with the value.")
        String summary,

        @Parameter(description = """
                Pieces of the value the warning is about, counted from its first character. \
                Absent when the warning is about the value as a whole.""")
        @Nullable List<TextSpot> spots
) {
}
