package org.openl.rules.webstudio.web.repository;

import java.util.regex.Pattern;

import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
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
            WebStudioUtils.validate(pattern.matcher(comment).matches(), invalidMessage);
        }
        WebStudioUtils.validate(comment.length() <= MAX_COMMENT_LENGTH,
            "Length is greater than allowable maximum of '" + MAX_COMMENT_LENGTH + "'");
    }

    public static CommentValidator forDesignRepo() {
        return new CommentValidator(Props.text("repository.design.comment-validation-pattern"),
            Props.text("repository.design.invalid-comment-message"));
    }

    static CommentValidator forDeployConfigRepo() {
        return new CommentValidator(Props.text("repository.deploy-config.comment-validation-pattern"),
            Props.text("repository.deploy-config.invalid-comment-message"));
    }
}
