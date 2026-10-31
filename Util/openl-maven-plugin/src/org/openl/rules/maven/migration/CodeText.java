package org.openl.rules.maven.migration;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Reads the structure of a piece of OpenL code that a migration rewrites: the arguments of a call and whether an
 * expression needs parentheses where it is put.
 * <p>
 * Text and character literals, quoted names, comments and brackets are read as a whole, so a comma or an operator
 * inside them does not count.
 *
 * @author Yury Molchan
 */
final class CodeText {

    private static final String OPENING = "([{";
    private static final String CLOSING = ")]}";
    private static final String QUOTES = "\"'`";
    private static final String BEFORE_OPERAND = "([{,;";
    private static final String AFTER_OPERAND = ")]},;";
    private static final String BEFORE_COMPARISON = "=!<>";
    private static final String RETURN = "return";

    private CodeText() {
    }

    /**
     * Returns the texts of the arguments of the call of the function, such as {@code a} and {@code b(c, d)} for
     * {@code name(a, b(c, d))}. Returns {@code null} when the text is not a whole call of the function.
     */
    static @Nullable List<String> arguments(String call, String function) {
        var open = call.indexOf('(');
        if (!call.startsWith(function) || open < 0 || !call.substring(function.length(), open).isBlank()
                || matching(call, open) != call.length() - 1) {
            return null;
        }
        return arguments(call, open);
    }

    /**
     * Returns the texts of the arguments in the parentheses that open at the position, or {@code null} when they are
     * not closed.
     */
    static @Nullable List<String> arguments(String code, int open) {
        var close = matching(code, open);
        if (close < 0) {
            return null;
        }
        var arguments = new ArrayList<String>();
        var from = open + 1;
        var i = from;
        while (i < close) {
            if (code.charAt(i) == ',') {
                arguments.add(code.substring(from, i).strip());
                from = i + 1;
            }
            i = next(code, i);
        }
        var last = code.substring(from, i).strip();
        if (!last.isEmpty() || !arguments.isEmpty()) {
            arguments.add(last);
        }
        return arguments;
    }

    /**
     * Tells whether the expression is a single operand: a name, a literal, a call or an access to a field or an
     * element. Such an expression needs no parentheses wherever it is put.
     */
    static boolean isAtomic(String expression) {
        var text = expression.strip();
        var i = 0;
        while (i < text.length()) {
            var c = text.charAt(i);
            var end = next(text, i);
            if (end == i + 1 && !Character.isJavaIdentifierPart(c) && c != '.') {
                return false;
            }
            i = end;
        }
        return !text.isEmpty();
    }

    /**
     * Tells whether the expression calls something, so that writing it twice runs the call twice. A part in
     * parentheses counts as a call.
     */
    static boolean hasCall(String expression) {
        var i = 0;
        while (i < expression.length()) {
            var skipped = skip(expression, i);
            if (skipped == i && expression.charAt(i) == '(') {
                return true;
            }
            i = Math.max(skipped, i + 1);
        }
        return false;
    }

    /**
     * Returns the expression to put in place of the part of the code between the positions: as it is, or in
     * parentheses when it is not a single operand and the place is next to an operator.
     */
    static String operand(String expression, String code, int start, int end) {
        return isAtomic(expression) || isDelimited(code, start, end) ? expression : "(" + expression + ")";
    }

    /**
     * Tells whether the part of the code between the positions stands as a whole expression: an argument, an
     * element, an assigned or returned value, or the whole code. Such a part keeps its meaning without parentheses
     * whatever expression it holds.
     */
    static boolean isDelimited(String code, int start, int end) {
        var before = code.substring(0, start).stripTrailing();
        var after = code.substring(end).stripLeading();
        var delimitedBefore = before.isEmpty()
                || BEFORE_OPERAND.indexOf(before.charAt(before.length() - 1)) >= 0
                || isAssignment(before)
                || endsWithWord(before, RETURN);
        return delimitedBefore && (after.isEmpty() || AFTER_OPERAND.indexOf(after.charAt(0)) >= 0);
    }

    private static boolean isAssignment(String before) {
        return before.endsWith("=")
                && (before.length() == 1 || BEFORE_COMPARISON.indexOf(before.charAt(before.length() - 2)) < 0);
    }

    private static boolean endsWithWord(String text, String word) {
        var start = text.length() - word.length();
        return text.endsWith(word) && (start == 0 || !Character.isJavaIdentifierPart(text.charAt(start - 1)));
    }

    /**
     * Returns the position of the bracket that closes the one at the position, or {@code -1} when it is not closed.
     */
    private static int matching(String code, int open) {
        var i = open + 1;
        while (i < code.length() && CLOSING.indexOf(code.charAt(i)) < 0) {
            i = next(code, i);
        }
        return i < code.length() ? i : -1;
    }

    /**
     * Returns the position after the literal, the comment or the bracketed part that starts at the position, or the
     * next position.
     */
    private static int next(String code, int i) {
        var skipped = skip(code, i);
        if (skipped > i) {
            return skipped;
        }
        var closing = OPENING.indexOf(code.charAt(i)) >= 0 ? matching(code, i) : -1;
        return closing > 0 ? closing + 1 : i + 1;
    }

    /**
     * Returns the position after the text literal, the character literal, the quoted name or the comment that starts
     * at the position, or the position itself when none starts there.
     */
    private static int skip(String code, int i) {
        var c = code.charAt(i);
        if (QUOTES.indexOf(c) >= 0) {
            var j = i + 1;
            while (j < code.length() && code.charAt(j) != c) {
                j += code.charAt(j) == '\\' ? 2 : 1;
            }
            return Math.min(j + 1, code.length());
        } else if (code.startsWith("//", i)) {
            var eol = code.indexOf('\n', i);
            return eol < 0 ? code.length() : eol;
        } else if (code.startsWith("/*", i)) {
            var end = code.indexOf("*/", i + 2);
            return end < 0 ? code.length() : end + 2;
        }
        return i;
    }
}
