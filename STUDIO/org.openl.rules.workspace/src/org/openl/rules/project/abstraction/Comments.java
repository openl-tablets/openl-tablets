package org.openl.rules.project.abstraction;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.util.StringUtils;

/**
 * The comments OpenL Studio writes for an action that commits a project when the user gives none.
 *
 * <p>The screens of OpenL Studio suggest the same comments, so a commit reads alike whether its comment was typed or
 * written by OpenL Studio. A missing project name or author is written as an empty text.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Comments {

    public static final String REPOSITORY_PREFIX = "repository.";

    private static final String COPIED_FROM_PREFIX = "Copied from: ";
    private static final String COPIED_FROM_SUFFIX = ".";
    /**
     * The moment of a restored revision. A comment is text stored in the repository and read by anyone, so the moment
     * is written in UTC and in English, whatever the reader or the server.
     */
    private static final DateTimeFormatter RESTORED_MOMENT = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a",
            Locale.ROOT).withZone(ZoneOffset.UTC);

    /** The comment of a saved project, such as {@code Project Bank Rating is saved.} */
    public static String saveProject(@Nullable String projectName) {
        return "Project " + Objects.requireNonNullElse(projectName, "") + " is saved.";
    }

    /** The comment of a created project, such as {@code Project Bank Rating is created.} */
    public static String createProject(@Nullable String projectName) {
        return "Project " + Objects.requireNonNullElse(projectName, "") + " is created.";
    }

    /** The comment of a copied project, naming the project it is copied from: {@code Copied from: Bank Rating.} */
    public static String copiedFrom(@Nullable String sourceProjectName) {
        return COPIED_FROM_PREFIX + Objects.requireNonNullElse(sourceProjectName, "") + COPIED_FROM_SUFFIX;
    }

    /**
     * The comment of a project restored from an old revision, naming the author and the moment of that revision:
     * {@code Restored from revision of John on 06/22/2020 09:02:42 PM.}
     *
     * <p>The moment is written in UTC. An unknown moment is written as an empty text.
     */
    public static String restoredFrom(@Nullable String author, @Nullable Date modifiedAt) {
        var moment = modifiedAt == null ? "" : RESTORED_MOMENT.format(modifiedAt.toInstant());
        return "Restored from revision of " + StringUtils.trimToEmpty(author) + " on " + moment + ".";
    }

    /**
     * The parts of the comment of a copied project: the text before the name of the source project, the name, and the
     * text after it.
     *
     * <p>Any other comment, an empty one included, is a single part, the comment itself.
     */
    public static List<@Nullable String> getCommentParts(@Nullable String comment) {
        if (comment == null || !comment.startsWith(COPIED_FROM_PREFIX) || !comment.endsWith(COPIED_FROM_SUFFIX)) {
            return Collections.singletonList(comment);
        }
        var name = comment.substring(COPIED_FROM_PREFIX.length(), comment.length() - COPIED_FROM_SUFFIX.length());
        return List.of(COPIED_FROM_PREFIX, name, COPIED_FROM_SUFFIX);
    }
}
