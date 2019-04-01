package org.openl.rules.utils;

import org.openl.util.StringUtils;

/**
 * Common util methods for parsing and binding.
 *
 * @author Yury Molchan
 */
public class ParserUtils {

    /**
     * Checks if the input string is commented
     *
     * @param str the string to check
     * @return true if string is commented
     */
    public static boolean isCommented(String str) {
        return str.trim().startsWith("//");
    }

    /**
     * Checks if the input string is commented or is blank
     *
     * @param str the string to check
     * @return true if string is commented or is blank
     */
    public static boolean isBlankOrCommented(String str) {
        return StringUtils.isBlank(str) || isCommented(str);
    }
}
