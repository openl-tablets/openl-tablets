package org.openl.rules.range;

/**
 * List of separators for parsing ranges.
 *
 * @author Yury Molchan
 */
enum Separator {

    DASH(Range.Type.CLOSED, "-"),
    SEMICOLON(Range.Type.CLOSED, ";"),
    ELLIPSIS(Range.Type.OPEN, "…"),
    TRIPLE_DOT(Range.Type.OPEN, "..."),
    DOUBLE_DOT(Range.Type.CLOSED, "..");

    private final Range.Type type;
    private final char[] chars;

    Separator(Range.Type type, String sep) {
        this.type = type;
        this.chars = sep.toCharArray();
    }

    int length() {
        return chars.length;
    }

    public Range.Type getType() {
        return type;
    }

    static Separator recognize(CharSequence text, int index) {
        for (Separator sep : values()) {
            if (sep.matches(text, index)) {
                return sep;
            }
        }
        throw new IllegalStateException("Unknown separator");
    }

    private boolean matches(CharSequence text, int index) {
        if (index + chars.length > text.length()) {
            return false;
        }
        for (int i = 0; i < chars.length; i++) {
            if (text.charAt(index + i) != chars[i]) {
                return false;
            }
        }
        return true;
    }
}
