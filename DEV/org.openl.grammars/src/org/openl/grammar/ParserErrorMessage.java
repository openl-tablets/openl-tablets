package org.openl.grammar;

/**
 *
 *
 * @author snshor
 *
 */

public class ParserErrorMessage {

    static public final String UNEXPECTED_SYMBOL = "Unexpected symbol '%s' after token <%s>",
            UNEXPECTED_BRACKET = "Unexpected bracket '%s'", UNMATCHED_BRACKET = "Need to close '%s'",
            MISMATCHED_BRACKET = "Mismatched: opened with '%s' and closed with '%s'",
            NEED_TO_CLOSE = "Need to close <%s> with <%s>";

    public static String printUnexpectedSymbolAfter(String token, char c) {
        return String.format(UNEXPECTED_SYMBOL, addEscapes(new String(new char[] { c })), addEscapes(token));
    }

    public static String printNeedToClose(String token, char c) {
        return String.format(NEED_TO_CLOSE, addEscapes(token), addEscapes(new String(new char[] { c })));
    }

    public static String printUnexpectedBracket(String image) {
        return String.format(UNEXPECTED_BRACKET, addEscapes(image));
    }

    public static String printUmatchedBracket(String image) {
        return String.format(UNMATCHED_BRACKET, addEscapes(image));
    }

    public static String printMismatchedBracket(String first, String second) {
        return String.format(MISMATCHED_BRACKET, addEscapes(first), addEscapes(second));
    }

    public static final String addEscapes(String str) {
        StringBuilder retval = new StringBuilder();
        char ch;
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case 0:
                    continue;
                case '\b':
                    retval.append("\\b");
                    continue;
                case '\t':
                    retval.append("\\t");
                    continue;
                case '\n':
                    retval.append("\\n");
                    continue;
                case '\f':
                    retval.append("\\f");
                    continue;
                case '\r':
                    retval.append("\\r");
                    continue;
                // case '\"':
                // retval.append("\\\"");
                // continue;
                // case '\'':
                // retval.append("\\\'");
                // continue;
                // case '\\':
                // retval.append("\\\\");
                // continue;
                default:
                    if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
                        String s = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u" + s.substring(s.length() - 4, s.length()));
                    } else {
                        retval.append(ch);
                    }
                    continue;
            }
        }
        return retval.toString();
    }

}
