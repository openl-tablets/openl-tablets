package org.openl.rules.rest.model;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.rules.rest.validation.PathConstraint;
import org.openl.rules.rest.validation.ProjectNameConstraint;
import org.openl.util.StringUtils;

public class CreateUpdateProjectModel {

    @NotBlank
    private final String repoName;
    @NotBlank
    private final String author;

    @NotBlank(message = "Project name must not be empty.")
    @ProjectNameConstraint
    private final String projectName;

    @PathConstraint
    private final String path;
    private final String comment;
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
        path = path.replace('\\', '/');
        if (path.charAt(path.length() - 1) != '/') {
            return path + "/";
        }
        return path;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getAuthor() {
        return author;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getPath() {
        return path;
    }

    public String getComment() {
        return comment;
    }

    public boolean isOverwrite() {
        return overwrite;
    }
}
