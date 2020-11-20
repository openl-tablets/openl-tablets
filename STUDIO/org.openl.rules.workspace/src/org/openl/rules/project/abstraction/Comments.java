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
    private static final String REPOSITORY_PREFIX = "repository.";

    private final PropertyResolver environment;
    private final String repoId;
    private final String dateTimeFormat;

    private String saveProjectTemplate;
    private String createProjectTemplate;
    private String archiveProjectTemplate;
    private String restoreProjectTemplate;
    private String eraseProjectTemplate;
    private String copiedFromTemplate;
    private String restoredFromTemplate;
    private String newBranchNameTemplate;

    public Comments(PropertyResolver environment, String repoId) {
        dateTimeFormat = Objects.requireNonNull(environment.getProperty("data.format.datetime"));
        Objects.requireNonNull(repoId, "prefix cannot be null");
        this.environment = environment;
        this.repoId = repoId;
    }

    public String getSaveProjectTemplate() {
        if(saveProjectTemplate==null){
            saveProjectTemplate = environment
                    .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.save");
        }
        return saveProjectTemplate;
    }

    public String getCreateProjectTemplate() {
        if (createProjectTemplate == null) {
            createProjectTemplate = environment
                    .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.create");
        }
        return createProjectTemplate;
    }

    public String getArchiveProjectTemplate() {
        if (archiveProjectTemplate == null) {
            archiveProjectTemplate = environment
                    .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.archive");
        }
        return archiveProjectTemplate;
    }

    public String getRestoreProjectTemplate() {
        if (restoreProjectTemplate == null) {
            restoreProjectTemplate = environment
                    .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.restore");
        }
        return restoreProjectTemplate;
    }

    public String getEraseProjectTemplate() {
        if (eraseProjectTemplate == null) {
            eraseProjectTemplate = environment
                    .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.erase");
        }
        return eraseProjectTemplate;
    }

    public String getCopiedFromTemplate() {
        if (copiedFromTemplate == null) {
            copiedFromTemplate = environment
                    .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.copied-from");
        }
        return copiedFromTemplate;
    }

    public String getRestoredFromTemplate() {
        if (restoreProjectTemplate == null) {
            restoredFromTemplate = environment
                    .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.restored-from");
        }
        return restoredFromTemplate;
    }

    public String getNewBranchNameTemplate() {
        if (newBranchNameTemplate == null) {
            newBranchNameTemplate = environment
                    .getProperty(REPOSITORY_PREFIX + repoId + ".new-branch-pattern");
        }
        return newBranchNameTemplate;
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
        this.environment = null;
        this.repoId = null;
    }

    public String saveProject(String projectName) {
        return getSaveProjectTemplate().replace(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String createProject(String projectName) {
        return getCreateProjectTemplate().replace(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : projectName);
    }

    // Only for creation from Workspace!
    public String createProject(String template, String projectName) {
        if (StringUtils.isBlank(template)) {
            return createProject(projectName);
        }
        return template.replace(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String archiveProject(String projectName) {
        return getArchiveProjectTemplate().replace(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String restoreProject(String projectName) {
        return getRestoreProjectTemplate().replace(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String eraseProject(String projectName) {
        return getEraseProjectTemplate().replace(PROJECT_NAME,
            projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String copiedFrom(String sourceProjectName) {
        return getCopiedFromTemplate().replace(PROJECT_NAME,
            sourceProjectName == null ? StringUtils.EMPTY : sourceProjectName);
    }

    public String newBranch(String projectName, String userName, String date) {
        return getNewBranchNameTemplate().replace(PROJECT_NAME,
                 projectName == null ? StringUtils.EMPTY : projectName)
                .replace(USER_NAME, userName == null ? StringUtils.EMPTY : userName)
                .replace(CURRENT_DATE, date == null ? StringUtils.EMPTY : date);
    }

    public List<String> getCommentParts(String comment) {
        if (StringUtils.isBlank(comment)) {
            return Collections.singletonList(comment);
        }
        String paramName = "{project-name}";
        String copiedFromTemplate = getCopiedFromTemplate();
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
        return getRestoredFromTemplate().replace(REVISION, StringUtils.trimToEmpty(revisionNum))
            .replace(AUTHOR, StringUtils.trimToEmpty(userName))
            .replace(DATETIME, dateStr);
    }
}
