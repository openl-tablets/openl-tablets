package org.openl.rules.project.abstraction;

import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.PropertyResolver;

import org.openl.util.StringUtils;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class Comments {
    private static final String PROJECT_NAME = "{project-name}";
    private static final String REVISION = "{revision}";
    private static final String AUTHOR = "{author}";
    /** The letters of a {@code {datetime:<pattern>}}, each with the longest run of it the pattern may hold. */
    private static final Map<Character, Integer> PATTERN_LETTERS = Map.of('y', 4, 'M', 4, 'd', 2, 'H', 2, 'h', 2,
            'm', 2, 's', 2, 'S', 3, 'a', 1, 'E', 4);
    /** {@code {datetime}}, or {@code {datetime:<pattern>}} naming the pattern the moment is written in. */
    private static final Pattern DATETIME = Pattern.compile("\\{datetime(?::([^}]*))?}");
    private static final String CURRENT_DATE = "{current-date}";
    private static final String USER_NAME = "{username}";
    public static final String REPOSITORY_PREFIX = "repository.";

    private final String saveProjectTemplate;
    @Getter
    private final String createProjectTemplate;
    private final String copiedFromTemplate;
    private final String restoredFromTemplate;
    private final String newBranchNameTemplate;

    public Comments(PropertyResolver environment, String repoId) {
        Objects.requireNonNull(repoId, "prefix cannot be null");
        saveProjectTemplate = environment
                .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.save");
        createProjectTemplate = environment
                .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.create");
        copiedFromTemplate = environment
                .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.copied-from");
        restoredFromTemplate = environment
                .getProperty(REPOSITORY_PREFIX + repoId + ".comment-template.user-message.default.restored-from");
        newBranchNameTemplate = environment.getProperty(REPOSITORY_PREFIX + repoId + ".new-branch.pattern");
    }

    public String saveProject(String projectName) {
        return saveProjectTemplate.replace(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String createProject(String projectName) {
        return createProjectTemplate.replace(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String copiedFrom(String sourceProjectName) {
        return copiedFromTemplate.replace(PROJECT_NAME,
                sourceProjectName == null ? StringUtils.EMPTY : sourceProjectName);
    }

    public String newBranch(@Nullable String projectName, String userName, String date) {
        var simplifiedProjName = Optional.ofNullable(projectName)
                .map(s -> s.replaceAll("[^\\p{LD}\\-$]", "")) // Remove restricted symbols
                .orElse(StringUtils.EMPTY);
        return newBranchNameTemplate.replace(PROJECT_NAME, simplifiedProjName)
                .replace(USER_NAME, userName == null ? StringUtils.EMPTY : userName)
                .replace(CURRENT_DATE, date == null ? StringUtils.EMPTY : date);
    }

    public List<String> getCommentParts(String comment) {
        if (StringUtils.isBlank(comment)) {
            return Collections.singletonList(comment);
        }
        var from = copiedFromTemplate.indexOf(PROJECT_NAME);
        if (from == -1) {
            return Collections.singletonList(comment);
        }
        var prefix = copiedFromTemplate.substring(0, from);
        var suffix = copiedFromTemplate.substring(from + PROJECT_NAME.length());
        if (comment.startsWith(prefix) && comment.endsWith(suffix)) {
            return Arrays.asList(prefix, comment.substring(from, comment.lastIndexOf(suffix)), suffix);
        }
        return Collections.singletonList(comment);
    }

    /**
     * The comment of a project restored from an old revision.
     *
     * <p>The {@code {datetime}} of the template is the moment the revision was made, in ISO 8601 and UTC to the second,
     * such as {@code 2020-06-22T21:02:42Z}. {@code {datetime:<pattern>}} writes it in a pattern of
     * {@link DateTimeFormatter} instead, such as {@code {datetime:yyyy-MM-dd HH:mm}}, in UTC as well.
     *
     * <p>A comment is text stored in the repository and read by anyone, so the moment depends on no reader and no time
     * zone. A pattern that is not one is ignored, and the moment is written in ISO 8601.
     */
    public String restoredFrom(String revisionNum, String userName, @Nullable Date modifiedAt) {
        var restored = restoredFromTemplate.replace(REVISION, StringUtils.trimToEmpty(revisionNum))
                .replace(AUTHOR, StringUtils.trimToEmpty(userName));
        return DATETIME.matcher(restored)
                .replaceAll(match -> Matcher.quoteReplacement(momentOf(modifiedAt, match.group(1))));
    }

    /**
     * The moment in the given pattern, in UTC, or in ISO 8601 to the second for no pattern or one this method does not
     * write.
     */
    private static String momentOf(@Nullable Date modifiedAt, @Nullable String pattern) {
        if (modifiedAt == null) {
            return "";
        }
        var moment = modifiedAt.toInstant();
        var iso = moment.truncatedTo(ChronoUnit.SECONDS).toString();
        if (StringUtils.isBlank(pattern) || !isWritten(pattern)) {
            return iso;
        }
        try {
            return DateTimeFormatter.ofPattern(pattern, Locale.ROOT).withZone(ZoneOffset.UTC).format(moment);
        } catch (IllegalArgumentException | DateTimeException e) {
            return iso;
        }
    }

    /**
     * Whether a pattern takes only the letters a comment writes a moment with, which the screen suggesting the comment
     * writes too: the year, the month, the day, the hours, the minutes, the seconds and their fraction, the day period
     * and the day of the week, each in a count {@link DateTimeFormatter} reads, and any text in single quotes.
     */
    static boolean isWritten(String pattern) {
        var at = 0;
        while (at < pattern.length()) {
            var letter = pattern.charAt(at);
            if (letter == '\'') {
                at = afterQuote(pattern, at);
                if (at < 0) {
                    return false;
                }
            } else {
                var end = at;
                while (end < pattern.length() && pattern.charAt(end) == letter) {
                    end++;
                }
                if (Character.isLetter(letter) && end - at > PATTERN_LETTERS.getOrDefault(letter, 0)) {
                    return false;
                }
                at = end;
            }
        }
        return true;
    }

    /**
     * Where the text in quotes starting at the given quote ends: past two quotes standing for one, or past the quote
     * closing the text, in which two quotes stand for one as well. -1 for a text no quote closes.
     */
    private static int afterQuote(String pattern, int quote) {
        if (isQuoteAt(pattern, quote + 1)) {
            return quote + 2;
        }
        var at = quote + 1;
        while (at < pattern.length()) {
            if (isQuoteAt(pattern, at)) {
                if (!isQuoteAt(pattern, at + 1)) {
                    return at + 1;
                }
                at++;
            }
            at++;
        }
        return -1;
    }

    private static boolean isQuoteAt(String pattern, int at) {
        return at < pattern.length() && pattern.charAt(at) == '\'';
    }
}
