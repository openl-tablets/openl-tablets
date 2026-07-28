package org.openl.studio.repositories.model;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;

import org.openl.rules.rest.validation.PathConstraint;
import org.openl.rules.rest.validation.ProjectNameConstraint;
import org.openl.util.StringUtils;

public class CreateUpdateProjectModel {

    @Getter
    @NotBlank
    private final String repoName;
    @Getter
    @NotBlank
    private final String author;

    @Getter
    @NotBlank(message = "{openl.constraints.not-blank.project-name.message}")
    @ProjectNameConstraint
    private final String projectName;

    @Getter
    @PathConstraint
    private final String path;
    @Getter
    private final String comment;
    @Getter
    private final boolean overwrite;

    public CreateUpdateProjectModel(String repoName,
                                    String author,
                                    String projectName,
                                    String path,
                                    String comment,
                                    boolean overwrite) {
        this.repoName = repoName;
        this.author = author;
        this.projectName = projectName;
        this.path = normalizePath(path);
        this.comment = comment;
        this.overwrite = overwrite;
    }

    private static String normalizePath(String path) {
        if (StringUtils.isEmpty(path)) {
            return path;
        }
        return path.replace('\\', '/');
    }

    /**
     * Where the project lands inside a non-flat repository. An uploaded archive carries the full internal
     * path, so the project folder may be named differently from the project itself.
     */
    public String getFullPath() {
        return StringUtils.isEmpty(path) ? projectName : path;
    }
}
