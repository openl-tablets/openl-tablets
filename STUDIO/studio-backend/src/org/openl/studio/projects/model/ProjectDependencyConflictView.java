package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Another dependency of a name, which asks for other Maven coordinates and is left out: one project of a name is
 * compiled, the one reached first.
 *
 * @param requestedBy   the project declaring the dependency left out
 * @param mavenArtifact the Maven coordinates it asks for, absent when it names none
 * @author Yury Molchan
 */
@Schema(description = "A dependency of the same name that asks for other Maven coordinates and is left out.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDependencyConflictView(
        @Parameter(description = "Project declaring the dependency left out") String requestedBy,
        @Parameter(description = "Maven coordinates it asks for, absent when it names none") String mavenArtifact) {
}
