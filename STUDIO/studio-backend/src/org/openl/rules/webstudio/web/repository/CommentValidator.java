package org.openl.rules.webstudio.web.repository;

import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.rules.workspace.RepositorySettingNames;
import org.openl.util.StringUtils;

public final class CommentValidator {

    /** The most characters of a comment a repository keeps: a database repository stores it in a column of 255. */
    public static final int MAX_COMMENT_LENGTH = 255;
    private final @Nullable Pattern pattern;
    private final @Nullable String invalidMessage;

    /**
     * @param regex          the pattern a comment must match; the spaces around it are ignored, as the screens of
     *                       OpenL Studio ignore them, and a blank one accepts any comment
     * @param invalidMessage the message telling why a comment does not match
     */
    CommentValidator(@Nullable String regex, @Nullable String invalidMessage) {
        var trimmed = StringUtils.trimToNull(regex);
        this.pattern = trimmed == null ? null : Pattern.compile(trimmed);
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

    /**
     * The validator of the comments of a repository. A repository without a validation pattern accepts any comment
     * up to the maximum length.
     */
    public static CommentValidator forRepo(String repoId) {
        var prefix = RepositorySettingNames.PREFIX + repoId;
        return new CommentValidator(Props.text(prefix + RepositorySettings.COMMENT_VALIDATION_PATTERN_SUFFIX),
                Props.text(prefix + RepositorySettings.INVALID_COMMENT_MESSAGE_SUFFIX));
    }

}
