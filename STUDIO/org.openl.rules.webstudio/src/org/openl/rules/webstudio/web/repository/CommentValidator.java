package org.openl.rules.webstudio.web.repository;

import java.util.regex.Pattern;

import org.openl.rules.project.abstraction.Comments;
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

    public static CommentValidator forRepo(String repoId) {
        boolean customComments = Boolean.parseBoolean(Props.text(Comments.REPOSITORY_PREFIX + repoId + ".comment-template.use-custom-comments"));
        if (customComments) {
            return new CommentValidator(Props.text(Comments.REPOSITORY_PREFIX + repoId + ".comment-template.comment-validation-pattern"),
                Props.text(Comments.REPOSITORY_PREFIX + repoId + ".comment-template.invalid-comment-message"));
        } else {
            return new CommentValidator(null, null);
        }
    }

}
