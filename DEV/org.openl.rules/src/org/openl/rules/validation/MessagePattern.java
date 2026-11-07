package org.openl.rules.validation;

import java.util.Set;
import java.util.TreeSet;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * The placeholders a message takes values into, read as {@link java.text.MessageFormat} reads its pattern.
 *
 * <p>A pair of apostrophes stands for one; a single one quotes the text up to the next one, braces included. A
 * placeholder is a number in braces, followed by a type and a style that may hold braces of their own.
 *
 * @param indices the numbers of the placeholders: {@code {0}} and {@code {1,number}} give 0 and 1
 * @param problem what breaks the pattern, or {@code null} for a pattern that is fine
 */
record MessagePattern(Set<Integer> indices, @Nullable Problem problem) {

    /** What breaks a pattern. */
    enum Problem {
        /** An apostrophe is left open: once values are passed, the rest of the text is printed as it is. */
        APOSTROPHE,
        /** A brace is left open, or a placeholder is not a number: the message cannot take values. */
        BRACE
    }

    static MessagePattern of(String text) {
        return new Reader(text).read();
    }

    /** Reads a pattern char by char. */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Reader {

        private final String text;
        private final Set<Integer> indices = new TreeSet<>();
        private final StringBuilder argument = new StringBuilder();
        private boolean quoted;
        private int depth;
        private @Nullable Problem problem;

        private MessagePattern read() {
            for (var i = 0; i < text.length(); i++) {
                i += readAt(i);
            }
            if (depth > 0) {
                problem = Problem.BRACE;
            } else if (quoted && problem == null) {
                problem = Problem.APOSTROPHE;
            }
            return new MessagePattern(indices, problem);
        }

        /** Reads the character at the index, and answers how many characters after it it took as well. */
        private int readAt(int index) {
            var ch = text.charAt(index);
            if (ch == '\'' && index + 1 < text.length() && text.charAt(index + 1) == '\'') {
                appendInArgument('\'');
                return 1;
            }
            if (ch == '\'') {
                quoted = !quoted;
            } else if (quoted) {
                appendInArgument(ch);
            } else if (ch == '{') {
                depth++;
                if (depth > 1) {
                    argument.append(ch);
                }
            } else if (ch == '}' && depth > 0) {
                closeBrace();
            } else {
                appendInArgument(ch);
            }
            return 0;
        }

        private void appendInArgument(char ch) {
            if (depth > 0) {
                argument.append(ch);
            }
        }

        private void closeBrace() {
            depth--;
            if (depth > 0) {
                argument.append('}');
                return;
            }
            var index = argument.toString().split(",", -1)[0].trim();
            if (!index.isEmpty() && index.chars().allMatch(Character::isDigit)) {
                indices.add(Integer.parseInt(index));
            } else {
                problem = Problem.BRACE;
            }
            argument.setLength(0);
        }
    }
}
