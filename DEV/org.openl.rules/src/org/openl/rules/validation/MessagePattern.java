package org.openl.rules.validation;

import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * The placeholders a message takes values into, and what breaks its pattern, each problem with its place in the
 * text.
 *
 * <p>{@link MessageFormat} itself judges a placeholder. It tells only that a pattern fails, not where, so the
 * pattern is cut into its placeholders, and each part of a placeholder it refuses is given to it alone: the number,
 * the type and the format. A full-width sign a translation writes for an ASCII one, such as {@code ，} for a comma
 * or {@code 。} for a dot, is told as such, and the parts are judged as the translator meant them.
 *
 * <p>{@code msg} formats a message only when values are passed: a message given none is returned as written. So a
 * single apostrophe, which quotes text, matters only in a message with a placeholder, and a pair of apostrophes,
 * which stands for one, is printed as two in a message without one. A quote of plain text is told beside a broken
 * placeholder as well, since an apostrophe of the language shifts the quotes after it and breaks a placeholder.
 *
 * @param indices  the numbers of the placeholders: {@code {0}} and {@code {1,number}} give 0 and 1
 * @param problems what breaks the pattern, in the order of the text
 */
record MessagePattern(Set<Integer> indices, List<Problem> problems) {

    /** What breaks a pattern. */
    enum Kind {
        /** A brace is left open: once values are passed, the rule fails. */
        BRACE,
        /**
         * A placeholder writes a full-width sign for an ASCII one, such as {@code ，} for a comma: once values are
         * passed, the rule fails.
         */
        FULL_WIDTH,
        /** A placeholder does not begin with its number: once values are passed, the rule fails. */
        PLACEHOLDER_NUMBER,
        /** A placeholder has a type {@code MessageFormat} does not know: once values are passed, the rule fails. */
        PLACEHOLDER_TYPE,
        /** A placeholder has a format its type does not take: once values are passed, the rule fails. */
        PLACEHOLDER_FORMAT,
        /**
         * Two single apostrophes in a message with a placeholder, which quote the text between them: once values are
         * passed, the apostrophes are dropped, and a quoted placeholder is printed as it is. A quote holding a brace
         * is meant to print the brace, and is fine.
         */
        APOSTROPHE,
        /**
         * Two single apostrophes of the language around a placeholder, as in {@code l'entité "{0}" et l'}: once values
         * are passed, the apostrophes are dropped, and the placeholder is printed as it is instead of its value.
         */
        QUOTED_PLACEHOLDER,
        /**
         * A single apostrophe left open in a message with a placeholder, which quotes the rest of the text: once
         * values are passed, the apostrophe is dropped, and a placeholder after it is printed as it is.
         */
        OPEN_APOSTROPHE,
        /**
         * A single apostrophe left open after the last placeholder: once values are passed, the apostrophe is dropped.
         * No placeholder follows it, so every one is replaced with its value.
         */
        TRAILING_APOSTROPHE,
        /** A pair of apostrophes in a message without a placeholder, which is printed as written: as two. */
        DOUBLED_APOSTROPHE
    }

    /**
     * One problem of a pattern and the piece of the text it is in.
     *
     * @param kind  what is wrong
     * @param start the position of the first character of the piece
     * @param end   the position after the last character of the piece
     */
    record Problem(Kind kind, int start, int end) {
    }

    /** The beginning of a placeholder, quoted or not, which tells a message written to take values. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d");

    /** The problems that hide no placeholder: the placeholders are read as they are written. */
    private static final Set<Kind> HIDING_NOTHING = EnumSet.of(Kind.APOSTROPHE,
            Kind.TRAILING_APOSTROPHE,
            Kind.DOUBLED_APOSTROPHE);

    /** Whether the placeholders of the pattern are all read: none is hidden by a broken one or a stray apostrophe. */
    boolean readsPlaceholders() {
        return problems.stream().allMatch(problem -> HIDING_NOTHING.contains(problem.kind()));
    }

    static MessagePattern of(String text) {
        return new Reader(text).read();
    }

    /**
     * The text with each full-width sign written as its ASCII one: the full-width forms of the ASCII signs, and the
     * ideographic comma, full stop and space. A sign keeps its place, so a position in one text is the same in the
     * other.
     */
    static String asciiOf(String text) {
        var ascii = new StringBuilder(text.length());
        text.chars().map(MessagePattern::asciiOf).forEach(ch -> ascii.append((char) ch));
        return ascii.toString();
    }

    private static int asciiOf(int ch) {
        return switch (ch) {
            case 0x3000 -> ' ';
            case 0x3001 -> ',';
            case 0x3002 -> '.';
            default -> ch >= 0xFF01 && ch <= 0xFF5E ? ch - 0xFEE0 : ch;
        };
    }

    /** Reads a pattern char by char, as {@code MessageFormat} reads it. */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Reader {

        private final String text;
        private final Set<Integer> indices = new TreeSet<>();
        private final List<Problem> broken = new ArrayList<>();
        /** The closed quotes of plain text, apostrophes included. */
        private final List<Problem> textQuotes = new ArrayList<>();
        private final List<Problem> pairs = new ArrayList<>();
        private boolean quoted;
        /** Where the open quote begins. */
        private int quoteStart;
        /** Whether the open quote is of plain text: outside a placeholder, and without a brace so far. */
        private boolean quoteOfText;
        /** Whether the open quote begins outside a placeholder. */
        private boolean quoteOutside;
        /** Whether the open quote holds a letter so far: the apostrophe of a word rather than a quote of a brace. */
        private boolean quoteOfWords;
        private int depth;
        /** Where the open placeholder begins. */
        private int braceStart;

        private MessagePattern read() {
            var at = 0;
            while (at < text.length()) {
                at += 1 + readAt(at);
            }
            if (depth > 0) {
                broken.add(new Problem(Kind.BRACE, braceStart, text.length()));
            }
            // A quote of plain text is told beside a broken placeholder too: an apostrophe of the language, as in
            // "l'étape {0}", shifts every quote after it, and a placeholder is broken by it.
            var problems = new ArrayList<>(broken);
            if (PLACEHOLDER.matcher(text).find()) {
                problems.addAll(textQuotes);
                if (quoted && PLACEHOLDER.matcher(text).region(quoteStart, text.length()).find()) {
                    problems.add(new Problem(Kind.OPEN_APOSTROPHE, quoteStart, text.length()));
                } else if (quoted && quoteOfWords) {
                    // Only the apostrophe is lost when no placeholder comes after it. A quote left open before a sign
                    // alone, the last of a pair of quotes an apostrophe before them shifted, tells nothing more.
                    problems.add(new Problem(Kind.TRAILING_APOSTROPHE, quoteStart, text.length()));
                }
            } else if (broken.isEmpty()) {
                problems.addAll(pairs);
            }
            problems.sort(Comparator.comparingInt(Problem::start));
            return new MessagePattern(indices, List.copyOf(problems));
        }

        /** Reads the character at the index, and answers how many characters after it it took as well. */
        private int readAt(int index) {
            var ch = text.charAt(index);
            if (ch == '\'') {
                return readQuote(index);
            }
            if (quoted) {
                quoteOfText = quoteOfText && ch != '{' && ch != '}';
                quoteOfWords = quoteOfWords || Character.isLetter(ch);
            } else if (ch == '{') {
                depth++;
                braceStart = depth == 1 ? index : braceStart;
            } else if (ch == '}' && depth > 0) {
                depth--;
                if (depth == 0) {
                    readPlaceholder(braceStart + 1, index);
                }
            }
            return 0;
        }

        /** Reads an apostrophe at the index: a doubled one stands for itself, a single one opens or closes a quote. */
        private int readQuote(int index) {
            if (index + 1 < text.length() && text.charAt(index + 1) == '\'') {
                pairs.add(new Problem(Kind.DOUBLED_APOSTROPHE, index, index + 2));
                return 1;
            }
            if (quoted) {
                closeQuote(index);
            }
            quoted = !quoted;
            quoteStart = quoted ? index : quoteStart;
            quoteOfText = quoted && depth == 0;
            quoteOutside = quoteOfText;
            quoteOfWords = false;
            return 0;
        }

        /**
         * Tells the quote that closes at the index when it is of the language: a quote of plain text, or one holding
         * words and a placeholder. A quote of a brace alone, as {@code '{'}, prints the brace, as it is meant to.
         */
        private void closeQuote(int index) {
            if (quoteOfText) {
                textQuotes.add(new Problem(Kind.APOSTROPHE, quoteStart, index + 1));
            } else if (quoteOutside && quoteOfWords && PLACEHOLDER.matcher(text).region(quoteStart, index).find()) {
                textQuotes.add(new Problem(Kind.QUOTED_PLACEHOLDER, quoteStart, index + 1));
            }
        }

        /**
         * Reads the placeholder between the positions and keeps its number. One {@code MessageFormat} refuses is
         * read again with its full-width signs written in ASCII, and its number, its type and its format are each
         * judged alone.
         */
        private void readPlaceholder(int start, int end) {
            var written = text.substring(start, end);
            var argument = asciiOf(written);
            var first = separatorAt(argument, 0);
            var second = first < 0 ? -1 : separatorAt(argument, first + 1);
            var number = argument.substring(0, first < 0 ? argument.length() : first);
            // Its type and format are tried on the first value, the one formatted.
            var typed = first < 0 ? "" : written.substring(first);
            if (takes("{" + written + "}") && formats("{0" + typed + "}")) {
                indices.add(Integer.parseInt(number));
                indices.addAll(chosenIndices("{" + written + "}"));
                return;
            }
            fullWidthSigns(written, argument, start);
            if (takes("{" + number + "}")) {
                indices.add(Integer.parseInt(number));
            } else {
                broken.add(new Problem(Kind.PLACEHOLDER_NUMBER, start, start + number.length()));
            }
            if (first < 0) {
                return;
            }
            var typeEnd = second < 0 ? argument.length() : second;
            var type = argument.substring(first + 1, typeEnd);
            var format = second < 0 ? "" : "," + argument.substring(second + 1);
            if (!takes("{0," + type + "}")) {
                broken.add(new Problem(Kind.PLACEHOLDER_TYPE, start + first + 1, start + typeEnd));
            } else if (!formats("{0," + type + format + "}")) {
                // A type that cannot go without a format, as a choice, is marked where the format is missing.
                var formatStart = second < 0 ? first + 1 : second + 1;
                broken.add(new Problem(Kind.PLACEHOLDER_FORMAT, start + formatStart, end));
            }
        }

        /** Tells each full-width sign the placeholder writes outside a quote, the placeholder beginning at start. */
        private void fullWidthSigns(String written, String argument, int start) {
            var inQuote = false;
            for (var i = 0; i < written.length(); i++) {
                inQuote = written.charAt(i) == '\'' ? !inQuote : inQuote;
                if (!inQuote && written.charAt(i) != argument.charAt(i)) {
                    broken.add(new Problem(Kind.FULL_WIDTH, start + i, start + i + 1));
                }
            }
        }

        /**
         * The placeholders of the texts a choice chooses from, as {@code {1}} in {@code {0,choice,0#none|1<{1} items}}:
         * the text chosen is formatted with the same values, so they are placeholders of the message too.
         */
        private static Set<Integer> chosenIndices(String placeholder) {
            var nested = new TreeSet<Integer>();
            Arrays.stream(new MessageFormat(placeholder, Locale.ROOT).getFormats())
                    .filter(ChoiceFormat.class::isInstance)
                    .map(ChoiceFormat.class::cast)
                    .flatMap(choice -> Arrays.stream(choice.getFormats()))
                    .map(String::valueOf)
                    .filter(chosen -> chosen.indexOf('{') >= 0)
                    .forEach(chosen -> nested.addAll(MessagePattern.of(chosen).indices()));
            return nested;
        }

        /** Whether {@code MessageFormat} takes the pattern. */
        private static boolean takes(String pattern) {
            try {
                new MessageFormat(pattern, Locale.ROOT);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        /**
         * Whether {@code MessageFormat} takes the pattern and formats a number as its first value. A choice without a
         * single choice, such as {@code {0,choice,abc}}, is taken and fails only once a value is formatted.
         */
        private static boolean formats(String pattern) {
            try {
                new MessageFormat(pattern, Locale.ROOT).format(new Object[] {1});
                return true;
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                return false;
            }
        }

        /** Where the next part of a placeholder begins: a comma outside a quote, or -1 for none. */
        private static int separatorAt(String argument, int from) {
            var inQuote = false;
            var at = -1;
            for (var i = from; i < argument.length() && at < 0; i++) {
                var ch = argument.charAt(i);
                inQuote = ch == '\'' ? !inQuote : inQuote;
                at = !inQuote && ch == ',' ? i : -1;
            }
            return at;
        }
    }
}
