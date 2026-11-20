package org.openl.studio.dependencies.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A project of the Maven repository the workspace depends on, as the list of the projects names it.
 *
 * @param groupId    the group of its coordinates
 * @param artifactId the artifact of its coordinates
 * @param version    the version of its coordinates
 * @param name       the name its {@code rules.xml} gives
 * @author Yury Molchan
 */
@Schema(description = "A project of the Maven repository the workspace depends on.")
public record MavenProjectSummary(
        @Parameter(description = "Group of the Maven coordinates") String groupId,
        @Parameter(description = "Artifact of the Maven coordinates") String artifactId,
        @Parameter(description = "Version of the Maven coordinates") String version,
        @Parameter(description = "Name of the project, as its rules.xml gives it") String name) {
}
