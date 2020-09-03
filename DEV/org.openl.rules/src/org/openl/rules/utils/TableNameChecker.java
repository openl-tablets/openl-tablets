package org.openl.rules.utils;

public class TableNameChecker {

    public static final String NAME_ERROR_MESSAGE = " name error. Name can only have letters, digits, _, $ and should not start with a digit.";

    private TableNameChecker() {
    }

    /**
     * Checks input string for Java Language Specification
     *
     * @param s - input string
     * @return true if string doesn't contain invalid characters
     */
    public static boolean isInvalidJavaIdentifier(String s) {
        if (s == null || s.length() == 0) {
            return true;
        }
        char[] c = s.toCharArray();

        if (!Character.isJavaIdentifierStart(c[0])) {
            return true;
        }

        for (int i = 1; i < c.length; i++) {
            if (!Character.isJavaIdentifierPart(c[i])) {
                return true;
            }
        }

        return false;
    }

    public static boolean isValidJavaIdentifier(String s) {
        return !isInvalidJavaIdentifier(s);
    }

}