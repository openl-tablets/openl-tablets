package org.openl.rules.webstudio.web.repository;

import java.util.regex.Pattern;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.spring.env.PropertyResolverProvider;
import org.openl.util.StringUtils;

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
        return new CommentValidator(
            PropertyResolverProvider.getProperty("repository.design.comment-validation-pattern"),
            PropertyResolverProvider.getProperty("repository.design.invalid-comment-message"));
    }

    static CommentValidator forDeployConfigRepo() {
        return new CommentValidator(
            PropertyResolverProvider.getProperty("repository.deploy-config.comment-validation-pattern"),
            PropertyResolverProvider.getProperty("repository.deploy-config.invalid-comment-message"));
    }
}
