package org.openl.rules.webstudio.web.repository;

import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.webstudio.web.Props;
import org.openl.util.StringUtils;

public final class CommentValidator {

    /** The most characters of a comment a repository keeps: a database repository stores it in a column of 255. */
    public static final int MAX_COMMENT_LENGTH = 255;
    private final Pattern pattern;
    private final String invalidMessage;

    private CommentValidator(String regex, String invalidMessage) {
        this.pattern = StringUtils.isBlank(regex) ? null : Pattern.compile(regex);
        this.invalidMessage = invalidMessage;
    }

    /**
     * Checks the comment against what the repository accepts.
     *
     * @param comment the comment to check; a missing one is read as empty
     * @throws IllegalArgumentException when the comment does not match the repository's pattern, or is longer than
     *                                  the maximum
     */
    public void validate(@Nullable String comment) {
        if (comment == null) {
            comment = "";
        }
        if (pattern != null && !pattern.matcher(comment).matches()) {
            throw new IllegalArgumentException(invalidMessage);
        }
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("it has %d characters, more than the %d a repository keeps"
                    .formatted(comment.length(), MAX_COMMENT_LENGTH));
        }
    }

    public static CommentValidator forRepo(String repoId) {
        var customComments = Boolean.parseBoolean(Props.text(Comments.REPOSITORY_PREFIX + repoId + ".comment-template.use-custom-comments"));
        if (customComments) {
            return new CommentValidator(Props.text(Comments.REPOSITORY_PREFIX + repoId + ".comment-template.comment-validation-pattern"),
                    Props.text(Comments.REPOSITORY_PREFIX + repoId + ".comment-template.invalid-comment-message"));
        }
        return new CommentValidator(null, null);
    }

}
