package org.openl.rules.project.abstraction;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

public final class Comments {
    /**
     * Repository id for default repository
     */
    public static final String DESIGN_CONFIG_REPO_ID = "design";

    /**
     * Repository id for Deploy Configuration repository
     */
    public static final String DEPLOY_CONFIG_REPO_ID = "deploy-config";

    private static final String PROJECT_NAME = "{project-name}";
    private static final String REVISION = "{revision}";
    private static final String AUTHOR = "{author}";
    private static final String DATETIME = "{datetime}";
    private static final String CURRENT_DATE = "{current-date}";
    private static final String USER_NAME = "{username}";
    public static final String REPOSITORY_PREFIX = "repository.";

    private final String dateTimeFormat;

    private final String saveProjectTemplate;
    private final String createProjectTemplate;
    private final String archiveProjectTemplate;
    private final String restoreProjectTemplate;
    private final String eraseProjectTemplate;
    private final String copiedFromTemplate;
    private final String restoredFromTemplate;
    private final String newBranchNameTemplate;

    public Comments(PropertyResolver environment, String repoId) {
        dateTimeFormat = Objects.requireNonNull(environment.getProperty("data.format.datetime"));
        Objects.requireNonNull(repoId, "prefix cannot be null");
        saveProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.save");
        createProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.create");
        archiveProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.archive");
        restoreProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.restore");
        eraseProjectTemplate = environment
            .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.erase");
        copiedFromTemplate = environment
            .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.copied-from");
        restoredFromTemplate = environment
            .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.restored-from");
        newBranchNameTemplate = environment.getProperty(REPOSITORY_PREFIX + repoId + ".new-branch.pattern");
    }

    // protected for tests
    protected Comments(String dateTimeFormat,
            String saveProjectTemplate,
            String createProjectTemplate,
            String archiveProjectTemplate,
            String restoreProjectTemplate,
            String eraseProjectTemplate,
            String copiedFromTemplate,
            String restoredFromTemplate,
            String newBranchNameTemplate) {
        this.dateTimeFormat = Objects.requireNonNull(dateTimeFormat);
        this.saveProjectTemplate = saveProjectTemplate;
        this.createProjectTemplate = createProjectTemplate;
        this.archiveProjectTemplate = archiveProjectTemplate;
        this.restoreProjectTemplate = restoreProjectTemplate;
        this.eraseProjectTemplate = eraseProjectTemplate;
        this.copiedFromTemplate = copiedFromTemplate;
        this.restoredFromTemplate = restoredFromTemplate;
        this.newBranchNameTemplate = newBranchNameTemplate;
    }

    public String saveProject(String projectName) {
        return saveProjectTemplate.replace(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String createProject(String projectName) {
        return createProjectTemplate.replace(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    // Only for creation from Workspace!
    public String createProject(String template, String projectName) {
        if (StringUtils.isBlank(template)) {
            return createProject(projectName);
        }
        return template.replace(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String archiveProject(String projectName) {
        return archiveProjectTemplate.replace(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String restoreProject(String projectName) {
        return restoreProjectTemplate.replace(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String eraseProject(String projectName) {
        return eraseProjectTemplate.replace(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String copiedFrom(String sourceProjectName) {
        return copiedFromTemplate.replace(PROJECT_NAME,
            sourceProjectName == null ? StringUtils.EMPTY : sourceProjectName);
    }

    public String newBranch(String projectName, String userName, String date) {
        return newBranchNameTemplate.replace(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName)
            .replace(USER_NAME, userName == null ? StringUtils.EMPTY : userName)
            .replace(CURRENT_DATE, date == null ? StringUtils.EMPTY : date);
    }

    public List<String> getCommentParts(String comment) {
        if (StringUtils.isBlank(comment)) {
            return Collections.singletonList(comment);
        }
        String paramName = "{project-name}";
        int from = copiedFromTemplate.indexOf(paramName);
        if (from == -1) {
            return Collections.singletonList(comment);
        }
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
        return restoredFromTemplate.replace(REVISION, StringUtils.trimToEmpty(revisionNum))
            .replace(AUTHOR, StringUtils.trimToEmpty(userName))
            .replace(DATETIME, dateStr);
    }

    public String getCreateProjectTemplate() {
        return createProjectTemplate;
    }
}
