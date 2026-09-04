package org.openl.studio.repositories.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

/**
 * A group of project templates the user can create a new project from.
 *
 * <p>Predefined templates are bundled with the application (categories {@code templates}, {@code
 * examples}, {@code tutorials}); custom templates are configured per installation. A template is
 * addressed by its {@code type}, {@code category} and name.
 */
@Builder
public record ProjectTemplateGroup(
        @Parameter(description = "Template source: predefined (bundled) or custom (per installation)") String type,
        @Parameter(description = "Template category") String category,
        @Parameter(description = "Template names in the category") List<String> templates) {
}
