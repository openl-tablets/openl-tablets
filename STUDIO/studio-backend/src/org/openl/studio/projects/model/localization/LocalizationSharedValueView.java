package org.openl.studio.projects.model.localization;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * The value a project the project depends on gives a key in a language.
 *
 * @param value     the value
 * @param project   the name of the project it comes from
 * @param projectId the id of that project, {@code null} when the workspace of the user does not hold it
 */
@Schema(description = "The value a project the project depends on gives a key in a language.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocalizationSharedValueView(
        @Parameter(description = "Value of the key.")
        String value,

        @Parameter(description = "Name of the project the value comes from: the nearest dependency that has one.")
        String project,

        @Parameter(description = "Id of the project the value comes from. Absent when the workspace does not hold it.")
        @Nullable String projectId
) {
}
