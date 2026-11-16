package org.openl.rules.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Converts a pattern of {@link Strings#like(String, String)} to a regular expression.
 *
 * <p>A pattern that converts to no valid regular expression, such as {@code [a-A]} with a reversed range, cannot match
 * any text. Rules that write such a pattern as text are reported when they compile.
 *
 * @author Yury Molchan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LikePattern {

    /**
     * Compiles a non-empty pattern of {@code like}.
     *
     * @throws PatternSyntaxException when the pattern is not valid
     */
    public static Pattern compile(String pattern) {
        return Pattern.compile(toRegex(pattern));
    }

    private static String toRegex(String pattern) {
        final var size = pattern.length();
        var regex = new StringBuilder(size * 2);

        var i = 0;
        var prevCh = 0;
        var nextCh = pattern.charAt(i);
        var inSet = false;
        while (i < size) {
            final var ch = nextCh;
            i++;
            nextCh = i < size ? pattern.charAt(i) : 0;
            switch (ch) {
                case '?', '*', '#', '@' -> regex.append(inSet ? String.valueOf(ch) : wildcardToRegex(ch));
                case '!' -> regex.append(inSet && prevCh == '[' && nextCh != ']' ? '^' : ch);
                case '[' -> {
                    if (inSet) {
                        regex.append('\\');
                    }
                    regex.append(ch);
                    inSet = true;
                }
                case ']' -> {
                    regex.append(ch);
                    inSet = false;
                }
                default -> appendLiteral(regex, ch, prevCh, inSet);
            }
            prevCh = ch;
        }

        return regex.toString();
    }

    private static String wildcardToRegex(char ch) {
        return switch (ch) {
            case '?' -> ".";
            case '*' -> ".*";
            case '#' -> "\\d";
            default -> "\\p{L}"; // '@'
        };
    }

    // A when guard is allowed only on a pattern label, not on the constant labels this switch dispatches on.
    @SuppressWarnings("java:S6916")
    private static void appendLiteral(StringBuilder regex, char ch, int prevCh, boolean inSet) {
        switch (ch) {
            case ' ' -> {
                if (prevCh != ' ') {
                    regex.append("\\s+");
                }
            }
            case '+' -> {
                if (prevCh != '?' && prevCh != '@' && prevCh != '#' && prevCh != ']') {
                    regex.append('\\');
                }
                regex.append(ch);
            }
            case '\\' -> regex.append('\\').append(ch);
            case '{', '}', '(', ')', '.', '$', '|' -> {
                if (!inSet) {
                    regex.append('\\');
                }
                regex.append(ch);
            }
            default -> regex.append(ch);
        }
    }
}
