package org.openl.rules.maven.extension;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Maven coordinates (groupId, artifactId) synthesised for a pom-less OpenL project.
 * <p>
 * By default the groupId is derived from the project folder's path relative to the anchor pom
 * directory: {@code baseGroupId} followed by the dotted intermediate path. When
 * {@code flattenGroupId} is {@code true} the path is ignored and every pom-less project shares the
 * anchor's {@code baseGroupId}. The artifactId is always the project folder name.
 *
 * @author Yury Molchan
 */
record OpenLCoordinates(String groupId, String artifactId) {

    private static final Pattern VALID_FRAGMENT = Pattern.compile("[A-Za-z0-9_.-]+");

    static OpenLCoordinates of(Path anchorDir, Path projectDir, String baseGroupId, boolean flattenGroupId) {
        var anchor = anchorDir.toAbsolutePath().normalize();
        var project = projectDir.toAbsolutePath().normalize();
        if (!project.startsWith(anchor)) {
            throw new IllegalArgumentException(
                    "Project folder '" + projectDir + "' is not located under anchor '" + anchorDir + "'.");
        }
        var artifactId = project.getFileName().toString();
        validateFragment(artifactId, projectDir, "artifactId");
        if (flattenGroupId) {
            return new OpenLCoordinates(baseGroupId, artifactId);
        }
        var relative = anchor.relativize(project);
        var groupIdBuilder = new StringBuilder(baseGroupId);
        var depth = relative.getNameCount() - 1;
        for (var i = 0; i < depth; i++) {
            var segment = relative.getName(i).toString();
            validateFragment(segment, projectDir, "groupId segment");
            groupIdBuilder.append('.').append(segment);
        }
        return new OpenLCoordinates(groupIdBuilder.toString(), artifactId);
    }

    private static void validateFragment(String fragment, Path projectDir, String role) {
        if (!VALID_FRAGMENT.matcher(fragment).matches()) {
            throw new IllegalArgumentException(
                    "Folder '" + projectDir + "' produces an invalid Maven " + role + " '" + fragment
                            + "'. Allowed characters: A-Z, a-z, 0-9, '.', '_', '-'.");
        }
    }
}
