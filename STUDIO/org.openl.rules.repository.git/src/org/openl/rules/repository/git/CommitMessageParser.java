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

    private static final String COMMIT_TYPE_TOKEN = "{commit-type}";
    private static final String USER_MESSAGE_TOKEN = "{user-message}";
    private static final String USERNAME_TOKEN = "{username}";
    private static final String[] TOKENS = {COMMIT_TYPE_TOKEN, USER_MESSAGE_TOKEN, USERNAME_TOKEN};
    private static final String COMMIT_TYPES = Stream.of(CommitType.values())
            .map(Enum::name)
            .collect(Collectors.joining("|"));

    private final Pattern pattern;

    CommitMessageParser(String commentTemplate) {
        if (commentTemplate != null && !commentTemplate.trim().isEmpty()) {
            String patternStr = buildPattern(commentTemplate);
            this.pattern = Pattern.compile(patternStr, Pattern.DOTALL);
        } else {
            this.pattern = null;
        }
    }

    private String buildPattern(String template) {
        StringBuilder builder = new StringBuilder();
        final int len = template.length();
        final int[] tokenCounter = new int[TOKENS.length];
        int start = 0, pos = 0;
        while (pos < len) {
            int end = pos;
            for (int tokenIdx = 0; tokenIdx < TOKENS.length; tokenIdx++) {
                final String token = TOKENS[tokenIdx];
                final int tokenLen = token.length();
                int tokenPos = 0;
                boolean matched = true;
                //start token matching
                while (tokenPos < tokenLen && pos < len) {
                    if (token.charAt(tokenPos++) != template.charAt(pos++)) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    if (start < end) {
                        //quote plain text
                        builder.append("\\Q").append(template, start, end).append("\\E");
                    }
                    start = pos;
                    final boolean firstTokenOccurrence = tokenCounter[tokenIdx] == 0;
                    tokenCounter[tokenIdx]++;
                    //open group
                    builder.append("(?");
                    if (!firstTokenOccurrence) {
                        //make non-capturing group
                        builder.append(':');
                    }
                    switch (token) {
                        case COMMIT_TYPE_TOKEN:
                            if (firstTokenOccurrence) {
                                //set group name
                                builder.append("<commitType>");
                            }
                            builder.append(COMMIT_TYPES);
                            break;
                        case USER_MESSAGE_TOKEN:
                            if (firstTokenOccurrence) {
                                //set group name
                                builder.append("<message>");
                            }
                            builder.append(".*");
                            break;
                        case USERNAME_TOKEN:
                            if (firstTokenOccurrence) {
                                //set group name
                                builder.append("<author>");
                            }
                            builder.append(".+");
                            break;
                    }
                    //close group
                    builder.append(')');
                    break;
                } else {
                    if (tokenIdx < TOKENS.length - 1) {
                        //reset position if current token is not the last one
                        pos = end;
                    }
                }
            }
        }
        if (start < len) {
            builder.append("\\Q").append(template, start, len).append("\\E");
        }
        return builder.toString();
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
