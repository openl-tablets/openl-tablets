package org.openl.rules.webstudio.web.repository;

import java.util.Map;
import java.util.regex.Pattern;

import org.openl.commons.web.jsf.FacesUtils;
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

    public static CommentValidator forDesignRepo(Map<String, Object> config) {
        return new CommentValidator(((String) config.get("design-repository.comment-validation-pattern")),
            ((String) config.get("design-repository.invalid-comment-message")));
    }

    static CommentValidator forDeployConfigRepo(Map<String, Object> config) {
        return new CommentValidator(((String) config.get("deploy-config-repository.comment-validation-pattern")),
            ((String) config.get("deploy-config-repository.invalid-comment-message")));
    }
}
