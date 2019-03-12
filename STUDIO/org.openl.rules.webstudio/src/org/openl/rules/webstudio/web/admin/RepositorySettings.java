package org.openl.rules.webstudio.web.admin;

import org.openl.config.ConfigurationManager;
import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;

public abstract class RepositorySettings {
    public static final String VERSION_IN_DEPLOYMENT_NAME = "version-in-deployment-name";
    private final String COMMENT_VALIDATION_PATTERN;
    private final String INVALID_COMMENT_MESSAGE;

    private boolean includeVersionInDeploymentName;
    private String commentValidationPattern;
    private String invalidCommentMessage;

    public boolean isIncludeVersionInDeploymentName() {
        return includeVersionInDeploymentName;
    }

    public void setIncludeVersionInDeploymentName(boolean includeVersionInDeploymentName) {
        this.includeVersionInDeploymentName = includeVersionInDeploymentName;
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

    RepositorySettings(ConfigurationManager configManager, String configPrefix) {
        COMMENT_VALIDATION_PATTERN = configPrefix + "comment-validation-pattern";
        INVALID_COMMENT_MESSAGE = configPrefix + "invalid-comment-message";

        load(configManager);
    }

    private void load(ConfigurationManager configManager) {
        includeVersionInDeploymentName = Boolean.valueOf(configManager.getStringProperty(VERSION_IN_DEPLOYMENT_NAME));

        commentValidationPattern = configManager.getStringProperty(COMMENT_VALIDATION_PATTERN);
        invalidCommentMessage = configManager.getStringProperty(INVALID_COMMENT_MESSAGE);
    }

    protected void fixState() {
    }

    protected void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(VERSION_IN_DEPLOYMENT_NAME, includeVersionInDeploymentName);
        propertiesHolder.setProperty(COMMENT_VALIDATION_PATTERN, commentValidationPattern);
        propertiesHolder.setProperty(INVALID_COMMENT_MESSAGE, invalidCommentMessage);
    }

    protected void revert(ConfigurationManager configurationManager) {
        configurationManager.removeProperties(VERSION_IN_DEPLOYMENT_NAME, COMMENT_VALIDATION_PATTERN, INVALID_COMMENT_MESSAGE);
        load(configurationManager);
    }

    static String getTypePrefix(RepositoryMode repositoryMode) {
        String type;
        switch (repositoryMode) {
            case DESIGN:
                type = "design";
                break;
            case DEPLOY_CONFIG:
                type = "deploy-config";
                break;
            case PRODUCTION:
                type = "deployment";
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return type;
    }

    protected void onTypeChanged(RepositoryType newRepositoryType) {
    }

    public void copyContent(RepositorySettings other) {
        setIncludeVersionInDeploymentName(other.isIncludeVersionInDeploymentName());
        setCommentValidationPattern(other.getCommentValidationPattern());
        setInvalidCommentMessage(other.getInvalidCommentMessage());
    }

    public RepositorySettingsValidators getValidators() {
        return new RepositorySettingsValidators();
    }
}
