package org.openl.rules.webstudio.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.Default;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.rest.validation.PathConstraint;
import org.openl.studio.settings.converter.SettingPropertyName;
import org.openl.studio.settings.model.constraint.CommentMessageTemplateConstraint;
import org.openl.studio.settings.model.constraint.RegexpConstraint;
import org.openl.util.StringUtils;

@JsonSubTypes({
        @JsonSubTypes.Type(value = AWSS3RepositorySettings.class, name = "repo-aws-s3"),
        @JsonSubTypes.Type(value = AzureBlobRepositorySettings.class, name = "repo-azure-blob"),
        @JsonSubTypes.Type(value = CommonRepositorySettings.class, names = {"repo-jdbc", "repo-jndi"}),
        @JsonSubTypes.Type(value = GitRepositorySettings.class, name = "repo-git"),
        @JsonSubTypes.Type(value = LocalRepositorySettings.class, name = "repo-file")
})
@Schema(description = "Repository settings", oneOf = {
        AWSS3RepositorySettings.class,
        AzureBlobRepositorySettings.class,
        CommonRepositorySettings.class,
        GitRepositorySettings.class,
        LocalRepositorySettings.class
})
public abstract class RepositorySettings implements ConfigPrefixSettingsHolder {

    private static final String USE_CUSTOM_COMMENTS_SUFFIX = ".comment-template.use-custom-comments";
    private static final String COMMENT_VALIDATION_PATTERN_SUFFIX = ".comment-template.comment-validation-pattern";
    private static final String INVALID_COMMENT_MESSAGE_SUFFIX = ".comment-template.invalid-comment-message";
    private static final String COMMENT_TEMPLATE_SUFFIX = ".comment-template";
    private static final String COMMENT_TEMPLATE_OLD_SUFFIX = ".comment-template-old";
    private static final String DEFAULT_COMMENT_SAVE_SUFFIX = ".comment-template.user-message.default.save";
    private static final String DEFAULT_COMMENT_CREATE_SUFFIX = ".comment-template.user-message.default.create";
    private static final String DEFAULT_COMMENT_ARCHIVE_SUFFIX = ".comment-template.user-message.default.archive";
    private static final String DEFAULT_COMMENT_RESTORE_SUFFIX = ".comment-template.user-message.default.restore";
    private static final String DEFAULT_COMMENT_ERASE_SUFFIX = ".comment-template.user-message.default.erase";
    private static final String DEFAULT_COMMENT_COPIED_FROM_SUFFIX = ".comment-template.user-message.default.copied-from";
    private static final String DEFAULT_COMMENT_RESTORED_FROM_SUFFIX = ".comment-template.user-message.default.restored-from";
    public static final String BASE_PATH_SUFFIX = ".base.path";
    private static final String DEPLOY_FROM_MAIN_BRANCH_SUFFIX = ".deploy-from-branch";

    public static final String MAIN_BRANCH = "MAIN_BRANCH";
    private final String USE_CUSTOM_COMMENTS;
    private final String COMMENT_VALIDATION_PATTERN;
    private final String INVALID_COMMENT_MESSAGE;
    private final String COMMENT_TEMPLATE;
    private final String COMMENT_TEMPLATE_OLD;
    private final String DEFAULT_COMMENT_SAVE;
    private final String DEFAULT_COMMENT_CREATE;
    private final String DEFAULT_COMMENT_ARCHIVE;
    private final String DEFAULT_COMMENT_RESTORE;
    private final String DEFAULT_COMMENT_ERASE;
    private final String DEFAULT_COMMENT_COPIED_FROM;
    private final String DEFAULT_COMMENT_RESTORED_FROM;
    private final String BASE_PATH;
    private final String DEPLOY_FROM_MAIN_BRANCH;

    @Parameter(description = "Customize comments")
    @SettingPropertyName(suffix = USE_CUSTOM_COMMENTS_SUFFIX)
    @JsonView(Views.Design.class)
    private boolean useCustomComments;

    @Parameter(description = "A regular expression that is used to validate user message.")
    @SettingPropertyName(suffix = COMMENT_VALIDATION_PATTERN_SUFFIX)
    @RegexpConstraint(groups = Validation.Design.class)
    @JsonView(Views.Design.class)
    private String commentValidationPattern;

    @Parameter(description = "This message is shown to the user if the user's message does not match the regular expression used in the validation pattern.")
    @NotBlank(message = "Invalid user message hint cannot be empty.", groups = Validation.Design.class)
    @SettingPropertyName(suffix = INVALID_COMMENT_MESSAGE_SUFFIX)
    @JsonView(Views.Design.class)
    private String invalidCommentMessage;

    @Parameter(description = "Comment message template for commits.")
    @SettingPropertyName(suffix = COMMENT_TEMPLATE_SUFFIX)
    @NotBlank(message = "Comment message template cannot be empty.", groups = Validation.Design.class)
    @CommentMessageTemplateConstraint(groups = Validation.Design.class)
    @JsonView(Views.Design.class)
    private String commentTemplate;

    @Parameter(description = "Comment message template for commits from old version.")
    @SettingPropertyName(suffix = COMMENT_TEMPLATE_OLD_SUFFIX)
    @JsonView(Views.Design.class)
    private String commentTemplateOld;

    @Parameter(description = "Default message for 'Save project'.")
    @SettingPropertyName(suffix = DEFAULT_COMMENT_SAVE_SUFFIX)
    @JsonView(Views.Design.class)
    private String defaultCommentSave;

    @Parameter(description = "Default message for 'Create project'.")
    @SettingPropertyName(suffix = DEFAULT_COMMENT_CREATE_SUFFIX)
    @JsonView(Views.Design.class)
    private String defaultCommentCreate;

    @Parameter(description = "Default message for 'Archive project'.")
    @SettingPropertyName(suffix = DEFAULT_COMMENT_ARCHIVE_SUFFIX)
    @JsonView(Views.Design.class)
    private String defaultCommentArchive;

    @Parameter(description = "Default message for 'Restore project'.")
    @SettingPropertyName(suffix = DEFAULT_COMMENT_RESTORE_SUFFIX)
    @JsonView(Views.Design.class)
    private String defaultCommentRestore;

    @Parameter(description = "Default message for 'Erase project'.")
    @SettingPropertyName(suffix = DEFAULT_COMMENT_ERASE_SUFFIX)
    @JsonView(Views.Design.class)
    private String defaultCommentErase;

    @Parameter(description = "Default message for 'Copy project'.")
    @SettingPropertyName(suffix = DEFAULT_COMMENT_COPIED_FROM_SUFFIX)
    @JsonView(Views.Design.class)
    private String defaultCommentCopiedFrom;

    @Parameter(description = "Default message when restore from old version.")
    @SettingPropertyName(suffix = DEFAULT_COMMENT_RESTORED_FROM_SUFFIX)
    @JsonView(Views.Design.class)
    private String defaultCommentRestoredFrom;

    @Parameter(description = "Path")
    @PathConstraint(allowTrailingSlash = true)
    @SettingPropertyName(suffix = BASE_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    private String basePath;

    @Parameter(description = "Deployment Branch")
    @SettingPropertyName(suffix = DEPLOY_FROM_MAIN_BRANCH_SUFFIX)
    @JsonView(Views.Production.class)
    private boolean mainBranchOnly;

    @JsonIgnore
    private final String configPrefix;

    @JsonIgnore
    private final RepositoryMode repositoryMode;

    RepositorySettings(PropertiesHolder propertyResolver, String configPrefix, RepositoryMode repositoryMode) {
        this.configPrefix = configPrefix;
        this.repositoryMode = repositoryMode;
        USE_CUSTOM_COMMENTS = configPrefix + USE_CUSTOM_COMMENTS_SUFFIX;
        COMMENT_VALIDATION_PATTERN = configPrefix + COMMENT_VALIDATION_PATTERN_SUFFIX;
        INVALID_COMMENT_MESSAGE = configPrefix + INVALID_COMMENT_MESSAGE_SUFFIX;
        COMMENT_TEMPLATE = configPrefix + COMMENT_TEMPLATE_SUFFIX;
        COMMENT_TEMPLATE_OLD = configPrefix + COMMENT_TEMPLATE_OLD_SUFFIX;
        DEFAULT_COMMENT_SAVE = configPrefix + DEFAULT_COMMENT_SAVE_SUFFIX;
        DEFAULT_COMMENT_CREATE = configPrefix + DEFAULT_COMMENT_CREATE_SUFFIX;
        DEFAULT_COMMENT_ARCHIVE = configPrefix + DEFAULT_COMMENT_ARCHIVE_SUFFIX;
        DEFAULT_COMMENT_RESTORE = configPrefix + DEFAULT_COMMENT_RESTORE_SUFFIX;
        DEFAULT_COMMENT_ERASE = configPrefix + DEFAULT_COMMENT_ERASE_SUFFIX;
        DEFAULT_COMMENT_COPIED_FROM = configPrefix + DEFAULT_COMMENT_COPIED_FROM_SUFFIX;
        DEFAULT_COMMENT_RESTORED_FROM = configPrefix + DEFAULT_COMMENT_RESTORED_FROM_SUFFIX;
        BASE_PATH = configPrefix + BASE_PATH_SUFFIX;
        DEPLOY_FROM_MAIN_BRANCH = configPrefix + DEPLOY_FROM_MAIN_BRANCH_SUFFIX;

        load(propertyResolver);
    }

    public String getCommentValidationPattern() {
        return commentValidationPattern;
    }

    public void setCommentValidationPattern(String commentValidationPattern) {
        this.commentValidationPattern = commentValidationPattern;
    }

    public String getInvalidCommentMessage() {
        return invalidCommentMessage;
    }

    public void setInvalidCommentMessage(String invalidCommentMessage) {
        this.invalidCommentMessage = invalidCommentMessage;
    }

    public String getCommentTemplate() {
        return commentTemplate;
    }

    public void setCommentTemplate(String commentTemplate) {
        this.commentTemplate = commentTemplate;
    }

    public String getCommentTemplateOld() {
        return commentTemplateOld;
    }

    public RepositorySettings setCommentTemplateOld(String commentTemplateOld) {
        this.commentTemplateOld = commentTemplateOld;
        return this;
    }

    public String getDefaultCommentSave() {
        return defaultCommentSave;
    }

    public void setDefaultCommentSave(String defaultCommentSave) {
        this.defaultCommentSave = defaultCommentSave;
    }

    public boolean isUseCustomComments() {
        return useCustomComments;
    }

    public void setUseCustomComments(boolean useCustomComments) {
        this.useCustomComments = useCustomComments;
    }

    public String getDefaultCommentCreate() {
        return defaultCommentCreate;
    }

    public void setDefaultCommentCreate(String defaultCommentCreate) {
        this.defaultCommentCreate = defaultCommentCreate;
    }

    public String getDefaultCommentArchive() {
        return defaultCommentArchive;
    }

    public void setDefaultCommentArchive(String defaultCommentArchive) {
        this.defaultCommentArchive = defaultCommentArchive;
    }

    public String getDefaultCommentRestore() {
        return defaultCommentRestore;
    }

    public void setDefaultCommentRestore(String defaultCommentRestore) {
        this.defaultCommentRestore = defaultCommentRestore;
    }

    public String getDefaultCommentErase() {
        return defaultCommentErase;
    }

    public void setDefaultCommentErase(String defaultCommentErase) {
        this.defaultCommentErase = defaultCommentErase;
    }

    public String getDefaultCommentCopiedFrom() {
        return defaultCommentCopiedFrom;
    }

    public void setDefaultCommentCopiedFrom(String defaultCommentCopiedFrom) {
        this.defaultCommentCopiedFrom = defaultCommentCopiedFrom;
    }

    public String getDefaultCommentRestoredFrom() {
        return defaultCommentRestoredFrom;
    }

    public void setDefaultCommentRestoredFrom(String defaultCommentRestoredFrom) {
        this.defaultCommentRestoredFrom = defaultCommentRestoredFrom;
    }

    public boolean isMainBranchOnly() {
        return mainBranchOnly;
    }

    public void setMainBranchOnly(boolean mainBranchOnly) {
        this.mainBranchOnly = mainBranchOnly;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath.isEmpty() || basePath.endsWith("/") ? basePath : (basePath + "/");
        ;
    }

    private void load(PropertiesHolder properties) {
        useCustomComments = Boolean.parseBoolean(properties.getProperty(USE_CUSTOM_COMMENTS));
        commentValidationPattern = properties.getProperty(COMMENT_VALIDATION_PATTERN);
        invalidCommentMessage = properties.getProperty(INVALID_COMMENT_MESSAGE);
        commentTemplate = properties.getProperty(COMMENT_TEMPLATE);
        commentTemplateOld = properties.getProperty(COMMENT_TEMPLATE_OLD);
        defaultCommentSave = properties.getProperty(DEFAULT_COMMENT_SAVE);
        defaultCommentCreate = properties.getProperty(DEFAULT_COMMENT_CREATE);
        defaultCommentArchive = properties.getProperty(DEFAULT_COMMENT_ARCHIVE);
        defaultCommentRestore = properties.getProperty(DEFAULT_COMMENT_RESTORE);
        defaultCommentErase = properties.getProperty(DEFAULT_COMMENT_ERASE);
        defaultCommentCopiedFrom = properties.getProperty(DEFAULT_COMMENT_COPIED_FROM);
        defaultCommentRestoredFrom = properties.getProperty(DEFAULT_COMMENT_RESTORED_FROM);

        mainBranchOnly = MAIN_BRANCH.equals(properties.getProperty(DEPLOY_FROM_MAIN_BRANCH));

        basePath = properties.getProperty(BASE_PATH);
        if (StringUtils.isBlank(basePath) && repositoryMode != null) {
            // Try to get default base path for repository mode.
            var defaultBasePathName = RepositoryConfiguration.REPOSITORY_DEFAULT_PREFIX + repositoryMode.name().toLowerCase() + BASE_PATH_SUFFIX;
            basePath = properties.getProperty(defaultBasePathName);
        }
    }

    protected void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(BASE_PATH, basePath);
        propertiesHolder.setProperty(USE_CUSTOM_COMMENTS, useCustomComments);
        propertiesHolder.setProperty(COMMENT_VALIDATION_PATTERN, commentValidationPattern);
        propertiesHolder.setProperty(INVALID_COMMENT_MESSAGE, invalidCommentMessage);

        propertiesHolder.setProperty(COMMENT_TEMPLATE, commentTemplate);
        propertiesHolder.setProperty(COMMENT_TEMPLATE_OLD, commentTemplateOld);
        propertiesHolder.setProperty(DEFAULT_COMMENT_SAVE, defaultCommentSave);
        propertiesHolder.setProperty(DEFAULT_COMMENT_CREATE, defaultCommentCreate);
        propertiesHolder.setProperty(DEFAULT_COMMENT_ARCHIVE, defaultCommentArchive);
        propertiesHolder.setProperty(DEFAULT_COMMENT_RESTORE, defaultCommentRestore);
        propertiesHolder.setProperty(DEFAULT_COMMENT_ERASE, defaultCommentErase);
        propertiesHolder.setProperty(DEFAULT_COMMENT_COPIED_FROM, defaultCommentCopiedFrom);
        propertiesHolder.setProperty(DEFAULT_COMMENT_RESTORED_FROM, defaultCommentRestoredFrom);

        propertiesHolder.setProperty(DEPLOY_FROM_MAIN_BRANCH, mainBranchOnly ? MAIN_BRANCH : null);
    }

    protected void revert(PropertiesHolder properties) {
        properties.revertProperties(USE_CUSTOM_COMMENTS,
                COMMENT_VALIDATION_PATTERN,
                INVALID_COMMENT_MESSAGE,
                COMMENT_TEMPLATE,
                COMMENT_TEMPLATE_OLD,
                DEFAULT_COMMENT_SAVE,
                DEFAULT_COMMENT_CREATE,
                DEFAULT_COMMENT_ARCHIVE,
                DEFAULT_COMMENT_RESTORE,
                DEFAULT_COMMENT_ERASE,
                DEFAULT_COMMENT_COPIED_FROM,
                DEFAULT_COMMENT_RESTORED_FROM,
                BASE_PATH,
                DEPLOY_FROM_MAIN_BRANCH);
        load(properties);
    }

    @Override
    public String getConfigPropertyKey(String configSuffix) {
        return configPrefix + configSuffix;
    }

    public String getConfigPrefix() {
        return configPrefix;
    }

    public static class Views {
        public interface Base {
        }

        public interface Design extends Base {
        }

        public interface Production extends Base {
        }
    }

    public static class Validation {
        public interface Design extends Default {
        }
    }
}
