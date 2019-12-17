package org.openl.rules.webstudio.web.repository;

import java.util.regex.Pattern;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.spring.env.ApplicationContextProvider;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

public class CommentValidator {
    private static final int MAX_COMMENT_LENGTH = 255;
    private final Pattern pattern;
    private final String invalidMessage;

    private CommentValidator(String regex, String invalidMessage) {
        this.pattern = StringUtils.isBlank(regex) ? null : Pattern.compile(regex);
        this.invalidMessage = invalidMessage;
    }

    public void validate(String comment) {
        if (comment == null) {
            comment = "";
        }
        if (pattern != null) {
            FacesUtils.validate(pattern.matcher(comment).matches(), invalidMessage);
        }
        FacesUtils.validate(comment.length() <= MAX_COMMENT_LENGTH,
            "Length is greater than allowable maximum of '" + MAX_COMMENT_LENGTH + "'");
    }

    public static CommentValidator forDesignRepo() {
        PropertyResolver propertyResolver = ApplicationContextProvider.getApplicationContext().getEnvironment();
        return new CommentValidator(propertyResolver.getProperty("repository.design.comment-validation-pattern"),
            propertyResolver.getProperty("repository.design.invalid-comment-message"));
    }

    static CommentValidator forDeployConfigRepo() {
        PropertyResolver propertyResolver = ApplicationContextProvider.getApplicationContext().getEnvironment();
        return new CommentValidator(propertyResolver.getProperty("repository.deploy-config.comment-validation-pattern"),
            propertyResolver.getProperty("repository.deploy-config.invalid-comment-message"));
    }
}
