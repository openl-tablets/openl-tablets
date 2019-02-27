package org.openl.rules.webstudio.web.admin;

import org.openl.config.ConfigurationManager;

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

    public RepositorySettings(ConfigurationManager configManager, String configPrefix) {
        COMMENT_VALIDATION_PATTERN = configPrefix + "comment-validation-pattern";
        INVALID_COMMENT_MESSAGE = configPrefix + "invalid-comment-message";

        includeVersionInDeploymentName = Boolean.valueOf(configManager.getStringProperty(VERSION_IN_DEPLOYMENT_NAME));

        commentValidationPattern = configManager.getStringProperty(COMMENT_VALIDATION_PATTERN);
        invalidCommentMessage = configManager.getStringProperty(INVALID_COMMENT_MESSAGE);
    }

    protected void fixState() {
    }

    protected void store(ConfigurationManager configurationManager) {
        configurationManager.setProperty(VERSION_IN_DEPLOYMENT_NAME, includeVersionInDeploymentName);
        configurationManager.setProperty(COMMENT_VALIDATION_PATTERN, commentValidationPattern);
        configurationManager.setProperty(INVALID_COMMENT_MESSAGE, invalidCommentMessage);
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
