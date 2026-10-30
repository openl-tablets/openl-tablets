package org.openl.studio.settings.model.repositories;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The list of the repositories of one kind, design or deployment.
 *
 * @param readOnly whether the list is set outside OpenL Studio, so no repository can be added or removed
 */
@Schema(description = "The list of the repositories of one kind, design or deployment.")
public record RepositoryConfigsModel(
        @Parameter(description = """
                Whether the list is set outside OpenL Studio, such as by an environment variable or \
                a system property. While it is, no repository can be added or removed.""")
        boolean readOnly) {
}
