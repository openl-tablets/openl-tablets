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
    private static final String DEFAULT_COMMENT_SAVE_SUFFIX = ".comment-template.user-message.default.save";
    private static final String DEFAULT_COMMENT_CREATE_SUFFIX = ".comment-template.user-message.default.create";
    private static final String DEFAULT_COMMENT_COPIED_FROM_SUFFIX = ".comment-template.user-message.default.copied-from";
    private static final String DEFAULT_COMMENT_RESTORED_FROM_SUFFIX = ".comment-template.user-message.default.restored-from";
    public static final String BASE_PATH_SUFFIX = ".base.path";
    private static final String DEPLOY_FROM_MAIN_BRANCH_SUFFIX = ".deploy-from-branch";

    public static final String MAIN_BRANCH = "MAIN_BRANCH";
    private final String useCustomCommentsKey;
    private final String commentValidationPatternKey;
    private final String invalidCommentMessageKey;
    private final String defaultCommentSaveKey;
    private final String defaultCommentCreateKey;
    private final String defaultCommentCopiedFromKey;
    private final String defaultCommentRestoredFromKey;
    private final String basePathKey;
    private final String deployFromMainBranchKey;

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

    @Parameter(description = "Default message for 'Save project'.")
    @SettingPropertyName(suffix = DEFAULT_COMMENT_SAVE_SUFFIX)
    @JsonView(Views.Design.class)
    private String defaultCommentSave;

    @Parameter(description = "Default message for 'Create project'.")
    @SettingPropertyName(suffix = DEFAULT_COMMENT_CREATE_SUFFIX)
    @JsonView(Views.Design.class)
    private String defaultCommentCreate;

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
        useCustomCommentsKey = configPrefix + USE_CUSTOM_COMMENTS_SUFFIX;
        commentValidationPatternKey = configPrefix + COMMENT_VALIDATION_PATTERN_SUFFIX;
        invalidCommentMessageKey = configPrefix + INVALID_COMMENT_MESSAGE_SUFFIX;
        defaultCommentSaveKey = configPrefix + DEFAULT_COMMENT_SAVE_SUFFIX;
        defaultCommentCreateKey = configPrefix + DEFAULT_COMMENT_CREATE_SUFFIX;
        defaultCommentCopiedFromKey = configPrefix + DEFAULT_COMMENT_COPIED_FROM_SUFFIX;
        defaultCommentRestoredFromKey = configPrefix + DEFAULT_COMMENT_RESTORED_FROM_SUFFIX;
        basePathKey = configPrefix + BASE_PATH_SUFFIX;
        deployFromMainBranchKey = configPrefix + DEPLOY_FROM_MAIN_BRANCH_SUFFIX;

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
    }

    private void load(PropertiesHolder properties) {
        useCustomComments = Boolean.parseBoolean(properties.getProperty(useCustomCommentsKey));
        commentValidationPattern = properties.getProperty(commentValidationPatternKey);
        invalidCommentMessage = properties.getProperty(invalidCommentMessageKey);
        defaultCommentSave = properties.getProperty(defaultCommentSaveKey);
        defaultCommentCreate = properties.getProperty(defaultCommentCreateKey);
        defaultCommentCopiedFrom = properties.getProperty(defaultCommentCopiedFromKey);
        defaultCommentRestoredFrom = properties.getProperty(defaultCommentRestoredFromKey);

        mainBranchOnly = MAIN_BRANCH.equals(properties.getProperty(deployFromMainBranchKey));

        basePath = properties.getProperty(basePathKey);
        if (StringUtils.isBlank(basePath) && repositoryMode != null) {
            // Try to get default base path for repository mode.
            var defaultBasePathName = RepositoryConfiguration.REPOSITORY_DEFAULT_PREFIX + repositoryMode.name().toLowerCase() + BASE_PATH_SUFFIX;
            basePath = properties.getProperty(defaultBasePathName);
        }
    }

    protected void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(basePathKey, basePath);
        propertiesHolder.setProperty(useCustomCommentsKey, useCustomComments);
        propertiesHolder.setProperty(commentValidationPatternKey, commentValidationPattern);
        propertiesHolder.setProperty(invalidCommentMessageKey, invalidCommentMessage);

        propertiesHolder.setProperty(defaultCommentSaveKey, defaultCommentSave);
        propertiesHolder.setProperty(defaultCommentCreateKey, defaultCommentCreate);
        propertiesHolder.setProperty(defaultCommentCopiedFromKey, defaultCommentCopiedFrom);
        propertiesHolder.setProperty(defaultCommentRestoredFromKey, defaultCommentRestoredFrom);

        propertiesHolder.setProperty(deployFromMainBranchKey, mainBranchOnly ? MAIN_BRANCH : null);
    }

    protected void revert(PropertiesHolder properties) {
        properties.revertProperties(useCustomCommentsKey,
                commentValidationPatternKey,
                invalidCommentMessageKey,
                defaultCommentSaveKey,
                defaultCommentCreateKey,
                defaultCommentCopiedFromKey,
                defaultCommentRestoredFromKey,
                basePathKey,
                deployFromMainBranchKey);
        load(properties);
    }

    @Override
    public String getConfigPropertyKey(String configSuffix) {
        return configPrefix + configSuffix;
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
