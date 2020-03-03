package org.openl.rules.project.abstraction;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

public final class Comments {

    private static final String PROJECT_NAME = "\\{project-name}";
    private static final String REVISION = "\\{revision}";
    private static final String USER_NAME = "\\{author}";
    private static final String DATETIME = "\\{datetime}";
    private static final String REPOSITORY_PREFIX = "repository.";

    private final String dateTimeFormat;
    private final String saveProjectTemplate;
    private final String createProjectTemplate;
    private final String archiveProjectTemplate;
    private final String restoreProjectTemplate;
    private final String eraseProjectTemplate;
    private final String copiedFromTemplate;
    private final String restoredFromTemplate;

    public Comments(PropertyResolver environment, String prefix) {
        dateTimeFormat = Objects.requireNonNull(environment.getProperty("data.format.date"));
        Objects.requireNonNull(prefix, "prefix cannot be null");
        saveProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + prefix + "comment-template.user-message.default.save");
        createProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + prefix + "comment-template.user-message.default.create");
        archiveProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + prefix + "comment-template.user-message.default.archive");
        restoreProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + prefix + "comment-template.user-message.default.restore");
        eraseProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + prefix + "comment-template.user-message.default.erase");
        copiedFromTemplate = environment
            .getProperty(REPOSITORY_PREFIX + prefix + "comment-template.user-message.default.copied-from");
        restoredFromTemplate = environment
            .getProperty(REPOSITORY_PREFIX + prefix + "comment-template.user-message.default.restored-from");
    }

    // protected for tests
    protected Comments(String dateTimeFormat,
            String saveProjectTemplate,
            String createProjectTemplate,
            String archiveProjectTemplate,
            String restoreProjectTemplate,
            String eraseProjectTemplate,
            String copiedFromTemplate,
            String restoredFromTemplate) {
        this.dateTimeFormat = Objects.requireNonNull(dateTimeFormat);
        this.saveProjectTemplate = saveProjectTemplate;
        this.createProjectTemplate = createProjectTemplate;
        this.archiveProjectTemplate = archiveProjectTemplate;
        this.restoreProjectTemplate = restoreProjectTemplate;
        this.eraseProjectTemplate = eraseProjectTemplate;
        this.copiedFromTemplate = copiedFromTemplate;
        this.restoredFromTemplate = restoredFromTemplate;
    }

    public String saveProject(String projectName) {
        return saveProjectTemplate.replaceAll(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : Matcher.quoteReplacement(projectName));
    }

    public String createProject(String projectName) {
        return createProjectTemplate.replaceAll(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : Matcher.quoteReplacement(projectName));
    }

    // Only for creation from Workspace!
    public String createProject(String template, String projectName) {
        if (StringUtils.isBlank(template)) {
            return createProject(projectName);
        }
        return template.replaceAll(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : Matcher.quoteReplacement(projectName));
    }

    public String archiveProject(String projectName) {
        return archiveProjectTemplate.replaceAll(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : Matcher.quoteReplacement(projectName));
    }

    public String restoreProject(String projectName) {
        return restoreProjectTemplate.replaceAll(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : Matcher.quoteReplacement(projectName));
    }

    public String eraseProject(String projectName) {
        return eraseProjectTemplate.replaceAll(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : Matcher.quoteReplacement(projectName));
    }

    public String copiedFrom(String sourceProjectName) {
        return copiedFromTemplate.replaceAll(PROJECT_NAME,
            sourceProjectName == null ? StringUtils.EMPTY : Matcher.quoteReplacement(sourceProjectName));
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

    public String restoredFrom(String revisionNum, String userName, Date modifiedAt) {
        String dateStr = modifiedAt == null ? "" : new SimpleDateFormat(dateTimeFormat).format(modifiedAt);
        return restoredFromTemplate.replaceAll(REVISION, Matcher.quoteReplacement(StringUtils.trimToEmpty(revisionNum)))
            .replaceAll(USER_NAME, Matcher.quoteReplacement(StringUtils.trimToEmpty(userName)))
            .replaceAll(DATETIME, Matcher.quoteReplacement(dateStr));
    }

    public String getCreateProjectTemplate() {
        return createProjectTemplate;
    }
}
