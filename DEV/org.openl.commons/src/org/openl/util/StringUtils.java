package org.openl.util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils to manipulate with strings.
 * 
 * @author Yury Molchan
 */
public class StringUtils {

    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Convert the specified CharSequence to an array of bytes, encoded using
     * UTF-8 character encoding.
     *
     * @param input the CharSequence to convert
     * @return an array of encoded chars
     */
    public static byte[] toBytes(CharSequence input) {
        return input.toString().getBytes(UTF_8);
    }

    /**
     * <p>
     * Splits the provided string into an array of strings, separator specified.
     * The separator is not included in the returned String array. Adjacent
     * separators are treated as one separator.
     * </p>
     *
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
     * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
     * StringUtils.split("a:b:c", '.')    = ["a:b:c"]
     * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
     * </pre>
     *
     * @param str the String to parse, may be null
     * @param separator the character used as the delimiter
     * @return an array of parsed Strings, {@code null} if null String input
     */
    public static String[] split(final String str, final char separator) {
        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        final List<String> list = new ArrayList<String>();
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        while (i < len) {
            if (str.charAt(i) == separator) {
                if (match) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }
                start = ++i;
                continue;
            }
            lastMatch = false;
            match = true;
            i++;
        }
        if (match && lastMatch) {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[list.size()]);
    }

}
