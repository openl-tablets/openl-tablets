package org.openl.rules.repository.git;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses {@code {commit-type}}, {@code {user-message}} and {@code {username}} from git commit messages by given template
 *
 * @author Vladyslav Pikus
 */
class CommitMessageParser {

    private final Pattern pattern;

    CommitMessageParser(String commentTemplate) {
        if (commentTemplate != null && !commentTemplate.trim().isEmpty()) {
            String commitTypeCombinations = Stream.of(CommitType.values())
                    .map(Enum::name)
                    .collect(Collectors.joining("|"));

            String patternStr = commentTemplate.replaceAll("\\{commit-type}", "\\\\E(?<commitType>(?:" + commitTypeCombinations + "))\\\\Q")
                    .replaceAll("\\{user-message}", "\\\\E(?<message>.*)\\\\Q")
                    .replaceAll("\\{username}", "\\\\E(?<author>.+)\\\\Q");
            this.pattern = Pattern.compile("\\Q" + patternStr + "\\E");
        } else {
            this.pattern = null;
        }
    }

    CommitMessage parse(String message) {
        if (pattern == null || message == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(message);
        if (!matcher.matches()) {
            return null;
        }
        return new CommitMessage(matcher);
    }

    static class CommitMessage {

        private final Matcher matcher;

        private CommitMessage(Matcher matcher) {
            this.matcher = Objects.requireNonNull(matcher);
        }

        CommitType getCommitType() {
            String value = getValue("commitType");
            return value == null ? null : CommitType.valueOf(value);
        }

        String getAuthor() {
            return getValue("author");
        }

        String getMessage() {
            return getValue("message");
        }

        private String getValue(String groupName) {
            try {
                return matcher.group(groupName);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
            }
            return null;
        }
    }

}
