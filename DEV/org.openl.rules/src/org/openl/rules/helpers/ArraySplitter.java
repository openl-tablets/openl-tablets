package org.openl.rules.helpers;

import org.openl.util.StringUtils;

/**
 * A utility class for parsing a string into array, elements of which are delimited by a comma {@code ','}.
 * Comma symbol can be escaped by the backslash symbol {@code '\'}.
 *
 * @author Yury Molchan
 */
public class ArraySplitter {

    private static final char ARRAY_SEPARATOR = ',';
    private static final char ARRAY_ESCAPE = '\\';

    /**
     * Splits the given string into the array elements.
     */
    public static String[] split(String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY_STRING_ARRAY;
        }
        var result = new String[count(text) + 1];

        int count = 0;
        int start = 0;
        int pos = -1;
        while ((pos = text.indexOf(ARRAY_SEPARATOR, pos + 1)) >= 0) {
            if (pos == 0 || text.charAt(pos - 1) != ARRAY_ESCAPE) {
                result[count] = stripToNull(text, start, pos);
                start = pos + 1;
                count++;
            }
        }

        result[count] = stripToNull(text, start, text.length());

        return result;
    }

    private static String stripToNull(String text, int start, int end) {
        int beginIndex = StringUtils.firstNonSpace(text, start, end);
        if (beginIndex < 0) {
            return null;
        }
        return text.substring(beginIndex, StringUtils.lastNonSpace(text, start, end) + 1).replace("\\,", ",");
    }

    private static int count(String text) {
        int count = 0;
        int pos = -1;
        while ((pos = text.indexOf(ARRAY_SEPARATOR, pos + 1)) >= 0) {
            if (pos == 0 || text.charAt(pos - 1) != ARRAY_ESCAPE) {
                count++;
            }
        }
        return count;
    }
}
