package org.openl.rules.workspace.dtr.impl;

import java.util.function.Function;

import org.openl.rules.repository.api.AdditionalData;
import org.openl.util.StringUtils;

public class FileMappingData implements AdditionalData<FileMappingData> {
    private String externalPath;
    private final String internalPath;

    public FileMappingData(String externalPath, String internalPath) {
        this.externalPath = externalPath;
        this.internalPath = StringUtils.trimToEmpty(internalPath);
    }

    /**
     * Maps a project to its place inside a non-flat repository.
     *
     * @param externalPath path the project is addressed by outside the repository
     * @param folder       folder inside the repository, with or without a trailing slash; empty for the root
     * @param projectName  name of the project, which becomes the last segment of the path
     */
    public static FileMappingData forProject(String externalPath, String folder, String projectName) {
        return new FileMappingData(externalPath, internalPath(folder, projectName));
    }

    /**
     * The path a project takes inside a non-flat repository — its folder followed by its name.
     *
     * <p>The folder is normalized first: back slashes become forward ones, leading slashes are dropped and a
     * separator is added when the folder does not end with one. A blank folder puts the project in the root.
     */
    public static String internalPath(String folder, String projectName) {
        var normalized = StringUtils.trimToEmpty(folder).replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.isEmpty() && !normalized.endsWith("/")) {
            normalized += "/";
        }
        return normalized + StringUtils.trimToEmpty(projectName);
    }

    @Override
    public FileMappingData convertPaths(Function<String, String> converter) {
        // We don't need to convert internalPath so return this.
        return this;
    }

    public String getInternalPath() {
        return internalPath;
    }

    public String getExternalPath() {
        return externalPath;
    }

    public void setExternalPath(String externalPath) {
        this.externalPath = externalPath;
    }
}
