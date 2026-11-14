package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Picks the section of an Excel number format that shows a value.
 * <p>
 * A format holds up to four sections separated by semicolons: for positive numbers, negative numbers, zero and text. One
 * section shows every number, with two sections the second shows the negative numbers, and with three the third shows
 * zero. The first two sections may have a condition in brackets, such as {@code [<1]} or {@code [>=.5]}, which takes
 * the place of their sign: a section shows a value its condition holds for, and the third section shows what the
 * first two do not. A value no section shows, such as {@code 5} for {@code [<1]0.00}, has no section, as Apache POI
 * shows it with another format.
 * <p>
 * A semicolon or a bracket in quotes or after a backslash is text, and so is a semicolon in brackets.
 *
 * @author Yury Molchan
 */
final class FormatSections {

    private static final Pattern CONDITION = Pattern
            .compile("\\s*([<>=]=?|!=|<>)\\s*(-?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*");

    private FormatSections() {
    }

    /**
     * Returns the section of a format that shows a value, without its condition.
     *
     * @return the section, or {@code null} when no section shows the value
     */
    static @Nullable String sectionFor(String format, double value) {
        var section = select(split(format), value);
        return section == null ? null : section.text();
    }

    private static @Nullable Section select(List<Section> sections, double value) {
        var positive = sections.getFirst();
        if (sections.size() == 1) {
            return positive.takes(value, true) ? positive : null;
        }
        var negative = sections.get(1);
        if (sections.size() == 2) {
            if (positive.takes(value, value >= 0)) {
                return positive;
            }
            return negative.takes(value, true) ? negative : null;
        } else if (positive.takes(value, value > 0)) {
            return positive;
        }
        return negative.takes(value, value < 0) ? negative : sections.get(2);
    }

    private static List<Section> split(String format) {
        var splitter = new Splitter();
        for (var c : format.toCharArray()) {
            splitter.accept(c);
        }
        return splitter.end();
    }

    /**
     * Splits a format into its sections, a character at a time.
     */
    private static final class Splitter {

        private final List<Section> sections = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder bracket = new StringBuilder();
        private @Nullable Condition condition;
        private boolean quoted;
        private boolean bracketed;
        private boolean escaped;

        void accept(char c) {
            if (bracketed) {
                acceptInBracket(c);
            } else if (!escaped && !quoted && c == ';') {
                endSection();
            } else if (!escaped && !quoted && c == '[') {
                bracketed = true;
                bracket.setLength(0);
            } else {
                text.append(c);
                quoted = quoted != (c == '"' && !escaped);
                escaped = !escaped && !quoted && c == '\\';
            }
        }

        List<Section> end() {
            endSection();
            return sections;
        }

        // A bracket holds a condition, such as [<1], or a text of the section, such as a color
        private void acceptInBracket(char c) {
            bracketed = c != ']';
            if (bracketed) {
                bracket.append(c);
                return;
            }
            var matcher = CONDITION.matcher(bracket);
            if (matcher.matches()) {
                condition = new Condition(matcher.group(1), Double.parseDouble(matcher.group(2)));
            } else {
                text.append('[').append(bracket).append(']');
            }
        }

        private void endSection() {
            sections.add(new Section(text.toString(), condition));
            text.setLength(0);
            condition = null;
        }
    }

    /**
     * A section of a format: its text and its condition.
     */
    private record Section(String text, @Nullable Condition condition) {

        /**
         * Tells whether the section shows a value: by its condition, or else as its place in the format says.
         */
        boolean takes(double value, boolean byPlace) {
            return condition == null ? byPlace : condition.holds(value);
        }
    }

    /**
     * A condition of a section, such as {@code <1}.
     */
    private record Condition(String operator, double bound) {

        boolean holds(double value) {
            return switch (operator) {
                case "<" -> value < bound;
                case "<=" -> value <= bound;
                case ">" -> value > bound;
                case ">=" -> value >= bound;
                case "<>", "!=" -> value != bound;
                default -> value == bound;
            };
        }
    }
}
