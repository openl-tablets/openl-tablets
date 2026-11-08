package org.openl.studio.projects.model.localization;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A key of the messages with its values in every language of the project.
 *
 * @param key      the key
 * @param values   the values of the key by the language that writes it; {@code null} for a key written without a
 *                 value
 * @param shared   the values the projects the project depends on give the key, by the language; empty when none does
 * @param warnings what the check of the files warns about the values; empty when it warns about nothing
 */
@Schema(description = "A key of the messages with its values in every language of the project.")
public record LocalizationMessageView(
        @Parameter(description = "Key of the messages.")
        String key,

        @Parameter(description = """
                Values of the key by the language that writes it, the default language as an empty name. A language \
                missing here does not write the key. `null` stands for a key written on a line without a separator: \
                the language has no value for it, and a rule gets the key itself.""")
        @JsonInclude(content = JsonInclude.Include.ALWAYS)
        Map<String, @Nullable String> values,

        @Parameter(description = """
                Values the projects the project depends on give the key, by the language: a language of the project \
                without a value of its own takes it from the nearest of them before its parent language. \
                Absent when none of them has the key.""")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, LocalizationSharedValueView> shared,

        @Parameter(description = """
                Warnings of the check of the localization files about the values of the key. \
                Absent when there are none.""")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<LocalizationWarningView> warnings
) {
}
