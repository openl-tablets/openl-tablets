package org.openl.rules.webstudio.web.repository;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.util.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

public class CommentValidator {
    private final Pattern pattern;
    private final String invalidMessage;

    private CommentValidator(String regex, String invalidMessage) {
        this.pattern = StringUtils.isBlank(regex) ? null : Pattern.compile(regex);
        this.invalidMessage = invalidMessage;
    }

    public void validate(String comment) {
        if (pattern != null) {
            if (comment == null) {
                comment = "";
            }
            FacesUtils.validate(pattern.matcher(comment).matches(), invalidMessage);
        }
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
