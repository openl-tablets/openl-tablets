package org.openl.studio.projects.model.localization;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The changes of the localization files to make as one write.
 *
 * <p>The changes are made in the order they are given, each seeing the files as the previous one left them. A change
 * that is refused refuses them all, and no file is written.
 *
 * @param actions the changes, in order
 */
@Schema(description = "Changes of the localization files, made in order as one write.")
public record LocalizationActions(
        @Parameter(description = """
                Changes to make, in order. Each sees the files as the previous one left them; one refused refuses \
                them all.""", required = true)
        @NotEmpty
        @Valid
        List<LocalizationAction> actions
) {
}
