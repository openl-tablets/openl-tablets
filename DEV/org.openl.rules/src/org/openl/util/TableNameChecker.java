package org.openl.util;

public class TableNameChecker {

    public static final String NAME_ERROR_MESSAGE = "%s '%s' name error. Name can only have letters, digits, _, $ and should not start with a digit.";

    private TableNameChecker() {
    }

    /**
     * Checks input string for Java Language Specification
     *
     * @param s - input string
     * @return true if string doesn't contain invalid characters
     */
    public static boolean isInvalidJavaIdentifier(String s) {
        return s == null || s.isEmpty() || indexOfInvalidChar(s, 0, s.length()) >= 0;
    }

    /**
     * Finds the first character of the identifier that Java does not accept.
     * <p>
     * The identifier is the text from {@code from} to {@code to}. Its first character must be one
     * {@link Character#isJavaIdentifierStart(int)} accepts, the others ones {@link Character#isJavaIdentifierPart(int)}
     * accepts. A character outside the Basic Multilingual Plane counts as one, though it takes two {@code char}s.
     *
     * @return the index of the first invalid character, or -1 when every character is valid
     */
    public static int indexOfInvalidChar(CharSequence s, int from, int to) {
        var i = from;
        while (i < to) {
            var c = Character.codePointAt(s, i);
            var valid = i == from ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c);
            if (!valid) {
                return i;
            }
            i += Character.charCount(c);
        }
        return -1;
    }

    public static boolean isValidJavaIdentifier(String s) {
        return !isInvalidJavaIdentifier(s);
    }

}
