package org.openl.studio.dependencies.service;

import org.jspecify.annotations.Nullable;

/**
 * The Maven coordinates of the archive of an OpenL project: its {@code zip}, with no classifier.
 *
 * @param groupId    the group
 * @param artifactId the artifact
 * @param version    the version
 * @author Yury Molchan
 */
public record MavenCoordinates(String groupId, String artifactId, String version) {

    /** What starts the id of a project downloaded from the Maven repository. */
    private static final String PROJECT_ID_PREFIX = "maven:";

    /**
     * The coordinates of a project downloaded from the Maven repository, by its id:
     * {@code maven:groupId:artifactId:version}.
     *
     * @return the coordinates, or {@code null} when the id names no downloaded project
     */
    public static @Nullable MavenCoordinates ofProjectId(String projectId) {
        if (!projectId.startsWith(PROJECT_ID_PREFIX)) {
            return null;
        }
        var parts = projectId.substring(PROJECT_ID_PREFIX.length()).split(":", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            return null;
        }
        return new MavenCoordinates(parts[0], parts[1], parts[2]);
    }

    /** The id the project downloaded from these coordinates is known by in the session. */
    public String projectId() {
        return PROJECT_ID_PREFIX + this;
    }

    /** The coordinates as {@code rules.xml} writes them: {@code groupId:artifactId:version}. */
    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
