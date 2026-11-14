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

    private static final String COMMENT_VALIDATION_PATTERN_SUFFIX = ".comment-template.comment-validation-pattern";
    private static final String INVALID_COMMENT_MESSAGE_SUFFIX = ".comment-template.invalid-comment-message";
    public static final String BASE_PATH_SUFFIX = ".base.path";
    private static final String DEPLOY_FROM_MAIN_BRANCH_SUFFIX = ".deploy-from-branch";
    private static final String PATH_SEPARATOR = "/";

    public static final String MAIN_BRANCH = "MAIN_BRANCH";
    private final String commentValidationPatternKey;
    private final String invalidCommentMessageKey;
    private final String basePathKey;
    private final String deployFromMainBranchKey;

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
        commentValidationPatternKey = configPrefix + COMMENT_VALIDATION_PATTERN_SUFFIX;
        invalidCommentMessageKey = configPrefix + INVALID_COMMENT_MESSAGE_SUFFIX;
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
        this.basePath = basePath.isEmpty() || basePath.endsWith(PATH_SEPARATOR) ? basePath
                : basePath + PATH_SEPARATOR;
    }

    private void load(PropertiesHolder properties) {
        commentValidationPattern = properties.getProperty(commentValidationPatternKey);
        invalidCommentMessage = properties.getProperty(invalidCommentMessageKey);

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
        propertiesHolder.setProperty(commentValidationPatternKey, commentValidationPattern);
        propertiesHolder.setProperty(invalidCommentMessageKey, invalidCommentMessage);

        propertiesHolder.setProperty(deployFromMainBranchKey, mainBranchOnly ? MAIN_BRANCH : null);
    }

    protected void revert(PropertiesHolder properties) {
        properties.revertProperties(commentValidationPatternKey,
                invalidCommentMessageKey,
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
