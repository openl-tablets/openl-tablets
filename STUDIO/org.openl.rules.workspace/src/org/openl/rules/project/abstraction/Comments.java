package org.openl.rules.project.abstraction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.util.StringUtils;

public final class Comments {

    private static final String PROJECT_NAME = "\\{project-name}";
    private static final String REVISION = "\\{revision}";

    private final String saveProjectTemplate;
    private final String createProjectTemplate;
    private final String archiveProjectTemplate;
    private final String restoreProjectTemplate;
    private final String eraseProjectTemplate;
    private final String copiedFromTemplate;
    private final String restoredFromTemplate;

    public Comments(Map<String, Object> properties, String prefix) {
        Objects.requireNonNull(prefix, "prefix cannot be null");

        saveProjectTemplate = properties.get(prefix + "comment-template.user-message.default.save").toString();
        createProjectTemplate = properties.get(prefix + "comment-template.user-message.default.create").toString();
        archiveProjectTemplate = properties.get(prefix + "comment-template.user-message.default.archive").toString();
        restoreProjectTemplate = properties.get(prefix + "comment-template.user-message.default.restore").toString();
        eraseProjectTemplate = properties.get(prefix + "comment-template.user-message.default.erase").toString();
        copiedFromTemplate = properties.get(prefix + "comment-template.user-message.default.copied-from").toString();
        restoredFromTemplate = properties.get(prefix + "comment-template.user-message.default.restored-from")
            .toString();
    }

    public String saveProject(String projectName) {
        return saveProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String createProject(String projectName) {
        return createProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    // Only for creation from Workspace!
    public String createProject(String template, String projectName) {
        if (StringUtils.isBlank(template)) {
            return createProject(projectName);
        }
        return template.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String archiveProject(String projectName) {
        return archiveProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String restoreProject(String projectName) {
        return restoreProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String eraseProject(String projectName) {
        return eraseProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String copiedFrom(String sourceProjectName) {
        return copiedFromTemplate.replaceAll(PROJECT_NAME,
            sourceProjectName == null ? StringUtils.EMPTY : sourceProjectName);
    }

    public List<String> getCommentParts(String comment) {
        if (StringUtils.isBlank(comment)) {
            return Collections.singletonList(comment);
        }
        String paramName = "{project-name}";
        int from = copiedFromTemplate.indexOf(paramName);
        String prefix = copiedFromTemplate.substring(0, from);
        String suffix = copiedFromTemplate.substring(from + paramName.length());
        if (comment.startsWith(prefix) && comment.endsWith(suffix)) {
            return Arrays.asList(prefix, comment.substring(from, comment.lastIndexOf(suffix)), suffix);
        } else {
            return Collections.singletonList(comment);
        }
    }

    public String restoredFrom(String revisionNum) {
        return restoredFromTemplate.replaceAll(REVISION, revisionNum == null ? StringUtils.EMPTY : revisionNum);
    }

    public String getCreateProjectTemplate() {
        return createProjectTemplate;
    }
}
