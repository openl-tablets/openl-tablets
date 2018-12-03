package org.openl.rules.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A set of util methods to work with strings.
 *
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author Yury Molchan
 */
public class Strings {

    /**
     * <p>
     * Checks if String contains a search String, handling <code>null</code>. This method uses
     * {@link String#indexOf(String)}.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> String will return <code>false</code>.
     * </p>
     * <p/>
     *
     * <pre>
     * contains(null, *)     = false
     * contains(*, null)     = false
     * contains("", "")      = true
     * contains("abc", "")   = true
     * contains("abc", "a")  = true
     * contains("abc", "z")  = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param searchStr the String to find, may be null
     * @return true if the String contains the search String, false if not or <code>null</code> string input
     */
    public static boolean contains(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.contains(searchStr);
    }

    /**
     * <p>
     * Checks if String contains a search character, handling <code>null</code>. This method uses
     * {@link String#indexOf(int)}.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> or empty ("") String will return <code>false</code>.
     * </p>
     * <p/>
     *
     * <pre>
     * contains(null, *)    = false
     * contains("", *)      = false
     * contains("abc", 'a') = true
     * contains("abc", 'z') = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param searchChar the character to find
     * @return true if the String contains the search character, false if not or <code>null</code> string input
     */
    public static boolean contains(String str, char searchChar) {
        if (isEmpty0(str)) {
            return false;
        }
        return str.indexOf(searchChar) != -1;
    }

    /**
     * <p>
     * Checks if the String contains any character in the given set of characters.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> String will return <code>false</code>. A <code>null</code> or zero length search array will
     * return <code>false</code>.
     * </p>
     * <p/>
     *
     * <pre>
     * containsAny(null, *)                = false
     * containsAny("", *)                  = false
     * containsAny(*, null)                = false
     * containsAny(*, [])                  = false
     * containsAny("zzabyycdxx",['z','a']) = true
     * containsAny("zzabyycdxx",['b','y']) = true
     * containsAny("aba", ['z'])           = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param chars the chars to search for, may be null
     * @return the <code>true</code> if any of the chars are found, <code>false</code> if no match or null input
     */
    public static boolean containsAny(String str, char... chars) {
        if (isEmpty0(str) || chars == null || chars.length == 0) {
            return false;
        }
        for (char ch : chars) {
            if (str.indexOf(ch) != -1)
                return true;
        }
        return false;
    }

    /**
     * <p>
     * Checks if the String contains any character in the given set of characters.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> String will return <code>false</code>. A <code>null</code> search string will return
     * <code>false</code>.
     * </p>
     * <p/>
     *
     * <pre>
     * containsAny(null, *)            = false
     * containsAny("", *)              = false
     * containsAny(*, null)            = false
     * containsAny(*, "")              = false
     * containsAny("zzabyycdxx", "za") = true
     * containsAny("zzabyycdxx", "by") = true
     * containsAny("aba","z")          = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param searchChars the chars to search for, may be null
     * @return the <code>true</code> if any of the chars are found, <code>false</code> if no match or null input
     */
    public static boolean containsAny(String str, String searchChars) {
        if (isEmpty0(str) || isEmpty0(searchChars)) {
            return false;
        }
        return containsAny(str, searchChars.toCharArray());
    }

    /**
     * Checks if a String is empty ("") or null or blank (" ").<br />
     * <br />
     * <code>
     * isEmpty(null)      = true <br />
     * isEmpty("")        = true <br />
     * isEmpty(" ")       = true <br />
     * isEmpty("bob")     = false <br />
     * isEmpty("  bob  ") = false <br />
     * </code>
     *
     * @param str the String to check, may be null
     * @return true if the String is empty or null or blank
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks if a String is NOT empty ("") or null or blank (" ").<br />
     * <br />
     * <code>
     * isEmpty(null)      = false <br />
     * isEmpty("")        = false <br />
     * isEmpty(" ")       = false <br />
     * isEmpty("bob")     = true <br />
     * isEmpty("  bob  ") = true <br />
     * </code>
     *
     * @param str the String to check, may be null
     * @return true if the String is NOT empty or null or blank
     * @see #isEmpty(String)
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Returns the length of this string.
     *
     * @return the length of the sequence of characters represented by this object, or 0 for null.
     */
    public static int length(String str) {
        return str == null ? 0 : str.length();
    }

    /**
     * Returns a string whose value is this string, with any leading and trailing whitespace removed.
     *
     * @return A string whose value is this string, with any leading and trailing white space removed, or this string if
     *         it has no leading or trailing white space.
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * Check if a String starts with a specified prefix.<br />
     * <br />
     * Two null references are considered to be equal. The comparison is case sensitive.<br />
     * <br />
     * <code>
     * startsWith(null, null)      = true <br />
     * startsWith(null, "abc")     = false <br />
     * startsWith("abcdef", null)  = false <br />
     * startsWith("abcdef", "abc") = true <br />
     * startsWith("ABCDEF", "abc") = false <br />
     * </code>
     *
     * @param str the String to check, may be null
     * @param prefix the prefix to find, may be null
     * @return true if the String starts with the prefix, case sensitive, or both null
     */
    public static boolean startsWith(String str, String prefix) {
        if (str == null || prefix == null) {
            return str == null && prefix == null;
        }
        if (prefix.length() > str.length()) {
            return false;
        }
        return str.startsWith(prefix);
    }

    /**
     * Check if a String ends with a specified suffix.<br />
     * <br />
     * Two null references are considered to be equal. The comparison is case sensitive.<br />
     * <br />
     * <code>
     * endsWith(null, null)      = true <br />
     * endsWith(null, "def")     = false <br />
     * endsWith("abcdef", null)  = false <br />
     * endsWith("abcdef", "def") = true <br />
     * endsWith("ABCDEF", "def") = false <br />
     * endsWith("ABCDEF", "cde") = false <br />
     * </code>
     *
     * @param str the String to check, may be null
     * @param suffix the suffix to find, may be null
     * @return true if the String ends with the suffix, case sensitive, or both null
     */
    public static boolean endsWith(String str, String suffix) {
        if (str == null || suffix == null) {
            return str == null && suffix == null;
        }
        if (suffix.length() > str.length()) {
            return false;
        }
        return str.endsWith(suffix);
    }

    public static String substr(String str, int pos, int length) {
        return substring(str, pos, pos + length);
    }

    /**
     * Gets a substring from the specified String<br />
     * <br />
     * A negative start position can be used to start n characters from the end of the String.<br />
     * <br />
     * A null String will return null. An empty ("") String will return "".<br />
     * <br />
     * * substring(null, *) = null <br />
     * substring("", *) = "" <br />
     * substring("abc", 0) = "abc" <br />
     * substring("abc", 2) = "c" <br />
     * substring("abc", 4) = "" <br />
     * substring("abc", -2) = "bc" <br />
     * substring("abc", -4) = "abc" <br />
     *
     * @param str the String to get the substring from, may be null
     * @param beginIndex the position to start from, negative means count back from the end of the String by this many
     *            characters
     * @return substring from start position, null if null String input
     */
    public static String substring(String str, int beginIndex) {
        if (str == null) {
            return null;
        }

        // handle negatives, which means last n characters
        if (beginIndex < 0) {
            beginIndex = str.length() + beginIndex; // remember start is negative
        }

        if (beginIndex < 0) {
            beginIndex = 0;
        }
        if (beginIndex > str.length()) {
            return "";
        }

        return str.substring(beginIndex);
    }

    /**
     * Gets a substring from the specified String <br />
     * <br />
     * A negative start position can be used to start/end n characters from the end of the String. <br />
     * <br />
     * The returned substring starts with the character in the start position and ends before the end position. All
     * position counting is zero-based -- i.e., to start at the beginning of the string use start = 0. Negative start
     * and end positions can be used to specify offsets relative to the end of the String. <br />
     * <br />
     * If start is not strictly to the left of end, "" is returned.<br />
     * <br />
     * <code>
     * substring(null, *, *)    = null <br />
     * substring("", * ,  *)    = ""; <br />
     * substring("abc", 0, 2)   = "ab" <br />
     * substring("abc", 2, 0)   = "" <br />
     * substring("abc", 2, 4)   = "c" <br />
     * substring("abc", 4, 6)   = "" <br />
     * substring("abc", 2, 2)   = "" <br />
     * substring("abc", -2, -1) = "b" <br />
     * substring("abc", -4, 2)  = "ab" <br />
     * </code>
     *
     * @param str the String to get the substring from, may be null
     * @param beginIndex the position to start from, negative means count back from the end of the String by this many
     *            characters
     * @param endIndex the position to end at (exclusive), negative means count back from the end of the String by this
     *            many characters
     * @return substring from start position to end positon, null if null String input
     */
    public static String substring(String str, int beginIndex, int endIndex) {
        if (str == null) {
            return null;
        }

        // handle negatives
        if (endIndex < 0) {
            endIndex = str.length() + endIndex; // remember end is negative
        }
        if (beginIndex < 0) {
            beginIndex = str.length() + beginIndex; // remember start is negative
        }

        // check length next
        if (endIndex > str.length()) {
            endIndex = str.length();
        }

        // if start is greater than end, return ""
        if (beginIndex > endIndex) {
            return "";
        }

        if (beginIndex < 0) {
            beginIndex = 0;
        }
        if (endIndex < 0) {
            endIndex = 0;
        }

        return str.substring(beginIndex, endIndex);
    }

    /**
     * Removes a substring only if it is at the begining of a source string, otherwise returns the source string. <br />
     * <br />
     * <p/>
     * A null source string will return null. An empty ("") source string will return the empty string. A null search
     * string will return the source string. <br />
     * <br />
     * <code>
     * removeStart(null, *)      = null <br />
     * removeStart("", *)        = "" <br />
     * removeStart(*, null)      = * <br />
     * removeStart("www.domain.com", "www.")   = "domain.com" <br />
     * removeStart("domain.com", "www.")       = "domain.com" <br />
     * removeStart("www.domain.com", "domain") = "www.domain.com" <br />
     * removeStart("abc", "")    = "abc" <br />
     * </code>
     *
     * @param str the source String to search, may be null
     * @param remove the String to search for and remove, may be null
     * @return the substring with the string removed if found, null if null String input
     */
    public static String removeStart(String str, String remove) {
        if (isEmpty0(str) || isEmpty0(remove)) {
            return str;
        }
        if (str.startsWith(remove)) {
            return str.substring(remove.length());
        }
        return str;
    }

    /**
     * Removes a substring only if it is at the end of a source string, otherwise returns the source string. <br />
     * <br />
     * <p/>
     * A null source string will return null. An empty ("") source string will return the empty string. A null search
     * string will return the source string. <br />
     * <br />
     * <code>
     * removeEnd(null, *)      = null <br />
     * removeEnd("", *)        = "" <br />
     * removeEnd(*, null)      = * <br />
     * removeEnd("www.domain.com", ".com.")  = "www.domain.com" <br />
     * removeEnd("www.domain.com", ".com")   = "www.domain" <br />
     * removeEnd("www.domain.com", "domain") = "www.domain.com" <br />
     * removeEnd("abc", "")    = "abc" <br />
     * </code>
     *
     * @param str the source String to search, may be null
     * @param remove the String to search for and remove, may be null
     * @return the String to search for and remove, may be null
     */
    public static String removeEnd(String str, String remove) {
        if (isEmpty0(str) || isEmpty0(remove)) {
            return str;
        }
        if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

    /**
     * Converts a String to lower case <br />
     * <br />
     * A null input String returns null. <br />
     * <br />
     * <p/>
     * <code>
     * lowerCase(null)  = null <br />
     * lowerCase("")    = "" <br />
     * lowerCase("aBc") = "abc" <br />
     * </code>
     *
     * @param str the String to lower case, may be null
     * @return the lower cased String, null if null String input
     */
    public static String lowerCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toLowerCase();
    }

    /**
     * Converts a String to upper case <br />
     * <br />
     * A null input String returns null.<br />
     * <br />
     * <p/>
     * <code>
     * upperCase(null)  = null <br />
     * upperCase("")    = "" <br />
     * upperCase("aBc") = "ABC" <br />
     * </code>
     *
     * @param str the String to upper case, may be null
     * @return the upper cased String, null if null String input
     */
    public static String upperCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase();
    }

    /**
     * Replaces all occurrences of a String within another String <br />
     * <br />
     * <p/>
     * A null reference passed to this method is a no-op. <br />
     * <br />
     * <p/>
     * <code>
     * replace(null, *, *)        = null <br />
     * replace("", *, *)          = "" <br />
     * replace("any", null, *)    = "any" <br />
     * replace("any", *, null)    = "any" <br />
     * replace("any", "", *)      = "any" <br />
     * replace("aba", "a", null)  = "aba" <br />
     * replace("aba", "a", "")    = "b" <br />
     * replace("aba", "a", "z")   = "zbz" <br />
     * </code>
     *
     * @param str text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement the String to replace it with, may be null
     * @return the text with any replacements processed, null if null String input
     */
    public static String replace(String str, String searchString, String replacement) {
        return replace(str, searchString, replacement, -1);
    }

    /**
     * Replaces a String with another String inside a larger String, for the first max values of the search String.<br>
     * <br />
     * A null reference passed to this method is a no-op. <br />
     * <br />
     * <p/>
     * <code>
     * replace(null, *, *, *)         = null <br />
     * replace("", *, *, *)           = "" <br />
     * replace("any", null, *, *)     = "any" <br />
     * replace("any", *, null, *)     = "any"    <br />
     * replace("any", "", *, *)       = "any"    <br />
     * replace("any", *, *, 0)        = "any"    <br />
     * replace("abaa", "a", null, -1) = "abaa" <br />
     * replace("abaa", "a", "", -1)   = "b" <br />
     * replace("abaa", "a", "z", 0)   = "abaa" <br />
     * replace("abaa", "a", "z", 1)   = "zbaa" <br />
     * replace("abaa", "a", "z", 2)   = "zbza" <br />
     * replace("abaa", "a", "z", -1)  = "zbzz" <br />
     * </code>
     *
     * @param str text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement the String to replace it with, may be null
     * @param max maximum number of values to replace, or -1 if no maximum
     * @return the text with any replacements processed, null if null String input
     */
    public static String replace(String str, String searchString, String replacement, int max) {
        if (isEmpty0(str) || isEmpty0(searchString) || replacement == null || max == 0) {
            return str;
        }
        int start = 0;
        int end = str.indexOf(searchString, start);
        if (end == -1) {
            return str;
        }
        final int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : max > 64 ? 64 : max;
        final StringBuilder buf = new StringBuilder(str.length() + increase);
        while (end != -1) {
            buf.append(str.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = str.indexOf(searchString, start);
        }
        buf.append(str.substring(start));
        return buf.toString();
    }

    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Double || obj instanceof Float || obj instanceof BigDecimal) {
            return obj.toString().replaceAll("(\\.0+$)|(?<=\\.\\d{0,20})0+$", ""); // remove zeros
        }
        return obj.toString();
    }

    public static Integer toInteger(String str) {
        try {
            return isEmpty(str) ? null : Integer.valueOf(trim(str));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double toDouble(String str) {
        try {
            return isEmpty(str) ? null : Double.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Number toNumber(String str) {
        if (isEmpty(str)) {
            return null;
        }
        ParsePosition parsePosition = new ParsePosition(0);
        Number parsed = NumberFormat.getInstance(Locale.US).parse(str, parsePosition);
        if (parsePosition.getIndex() != str.length()) {
            return null;
        }
        return parsed;
    }

    public static String concatenate(Object... objects) {
        if (objects == null || objects.length == 0) {
            return null;
        }
        StringBuilder builder = null;
        for (Object obj : objects) {
            if (obj != null) {
                if (builder == null) {
                    builder = new StringBuilder(16 * objects.length);
                }
                builder.append(obj);
            }
        }
        return builder == null ? null : builder.toString();
    }

    private static boolean isEmpty0(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Checks if the String is matched using given Pattern.<br/>
     * <br/>
     * Syntax:<br/>
     * <br/>
     *
     * <pre>
     * # - matches any single digit
     * ? - matches any single character
     * * - matches any character 0 or more times
     * &#64; - matches any single alphabetic character
     * [charlist] - matches any single character in {@code charlist}
     * [!charlist]- matches any single character not in {@code charlist}
     * X+ - matches @{code X} one or more times
     * </pre>
     *
     * <br/>
     * Examples:<br/>
     * <br/>
     * <code>
     *     like(null, "")      = true<br/>
     *     like("", "#")       = false<br/>
     *     like("9", "#")      = true<br/>
     *     like("a", "?")      = true<br/>
     *     like("a", "@")      = true<br/>
     *     like("1a23", "*")   = true<br/>
     *     like("foo@bar.com", "?+\@?+\.?+")                = true<br/>
     *     like("+38(099) 123-12-12", "+##(###) ###-##-##") = true<br/>
     * </code>
     *
     * @param str any String
     * @param pattern pattern
     * @return {@code true} if the String matches given pattern
     */
    public static boolean like(String str, String pattern) {
        if (isEmpty0(str)) {
            return isEmpty0(pattern);
        }
        if (isEmpty0(pattern)) {
            return false;
        }

        String likePattern = parseLikePattern(pattern);
        Pattern regex = Pattern.compile(likePattern);
        return regex.matcher(str).matches();
    }

    private static String parseLikePattern(String pattern) {
        final int size = pattern.length();
        StringBuilder regex = new StringBuilder(size * 2);

        int i = 0;
        char prevCh = 0;
        char nextCh = pattern.charAt(i);
        boolean inSet = false;
        while (i < size) {
            final char ch = nextCh;
            i++;
            nextCh = i < size ? pattern.charAt(i) : 0;
            switch (ch) {
                case '?':
                    if (!inSet) {
                        regex.append('.');
                    } else {
                        regex.append(ch);
                    }
                    break;
                case '*':
                    if (!inSet) {
                        regex.append(".*");
                    } else {
                        regex.append(ch);
                    }
                    break;
                case '#':
                    if (!inSet) {
                        regex.append("\\d");
                    } else {
                        regex.append(ch);
                    }
                    break;
                case '@':
                    if (!inSet) {
                        regex.append("\\p{Alpha}");
                    } else {
                        regex.append(ch);
                    }
                    break;
                case '!':
                    if (inSet && prevCh == '[' && nextCh != ']') {
                        regex.append('^');
                    } else {
                        regex.append(ch);
                    }
                    break;
                case '[':
                    if (inSet) {
                        regex.append('\\');
                    }
                    regex.append(ch);
                    inSet = true;
                    break;
                case ']':
                    regex.append(ch);
                    inSet = false;
                    break;
                case ' ':
                    if (prevCh != ' ') {
                        regex.append("\\s+");
                    }
                    break;
                case '+':
                    if (prevCh != '?' && prevCh != '@' && prevCh != '#' && prevCh != ']') {
                        regex.append('\\');
                    }
                    regex.append(ch);
                    break;
                case '\\':
                    regex.append('\\');
                    regex.append(ch);
                    break;
                case '{':
                case '}':
                case '(':
                case ')':
                case '.':
                case '$':
                case '|':
                    if (!inSet) {
                        regex.append('\\');
                    }
                    regex.append(ch);
                    break;
                default:
                    regex.append(ch);
                    break;
            }
            prevCh = ch;
        }

        return regex.toString();
    }
}
