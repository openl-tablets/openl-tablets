package org.openl.util;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils to manipulate with strings.
 *
 * @author Yury Molchan
 */
public class StringUtils {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final String EMPTY = "";

    /**
     * Convert the specified CharSequence to an array of bytes, encoded using UTF-8 character encoding.
     *
     * @param input the CharSequence to convert
     * @return an array of encoded chars
     */
    public static byte[] toBytes(CharSequence input) {
        return input.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * <p>
     * Splits the provided string into an array of trimmed strings, separator specified. The separator is not included
     * in the returned String array. Adjacent separators are treated as one separator.
     * </p>
     * <p>
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
     * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
     * StringUtils.split(" a b:c ", '.')  = ["a b:c"]
     * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
     * StringUtils.split(" a, b, c", ',') = ["a", "b", "c"]
     * </pre>
     *
     * @param str the String to parse, may be null
     * @param separator the character used as the delimiter
     * @return an array of parsed Strings, {@code null} if null String input
     */
    public static String[] split(final String str, final char separator) {
        return splitWorker(str, new Predicate() {
            @Override
            public boolean evaluate(char ch) {
                return ch == separator;
            }
        }, true);
    }

    /**
     * <p>
     * Splits the provided text into an array, using whitespace as the separator. Whitespace is defined by
     * {@link Character#isWhitespace(char)}.
     * </p>
     * <p>
     * <p>
     * The separator is not included in the returned String array. Adjacent separators are treated as one separator. For
     * more control over the split use the StrTokenizer class.
     * </p>
     * <p>
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.split(null)         = null
     * StringUtils.split("")           = []
     * StringUtils.split("abc def")    = ["abc", "def"]
     * StringUtils.split("abc  def")   = ["abc", "def"]
     * StringUtils.split(" abc ")      = ["abc"]
     * StringUtils.split(" abc def ")  = ["abc", "def"]
     * </pre>
     *
     * @param str the String to parse, may be null
     * @return an array of parsed Strings, {@code null} if null String input
     */
    public static String[] split(final String str) {
        return splitWorker(str, Character::isWhitespace, false);
    }

    private static String[] splitWorker(final String str, final Predicate tester, boolean trim) {
        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        final List<String> list = new ArrayList<>();
        int i = 0, start = 0, end = 0;
        boolean match = false;
        while (i < len) {
            char ch = str.charAt(i++);
            if (tester.evaluate(ch)) {
                if (match) {
                    list.add(str.substring(start, end));
                    match = false;
                }
                start = i;
            } else if (trim && Character.isWhitespace(ch)) {
                if (!match) {
                    start = i;
                }
            } else {
                match = true;
                end = i;
            }
        }
        if (match) {
            list.add(str.substring(start, end));
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * <p>
     * Joins the elements of the provided array into a single String containing the provided list of elements.
     * </p>
     * <p>
     * <p>
     * No delimiter is added before or after the list. A {@code null} separator is the same as an empty String ("").
     * Null objects or empty strings within the array are represented by empty strings.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.join(null, *)                = null
     * StringUtils.join([], *)                  = ""
     * StringUtils.join([null], *)              = ""
     * StringUtils.join(["a", "b", "c"], "--")  = "a--b--c"
     * StringUtils.join(["a", "b", "c"], null)  = "abc"
     * StringUtils.join(["a", "b", "c"], "")    = "abc"
     * StringUtils.join([null, "", "a"], ',')   = ",,a"
     * </pre>
     *
     * @param values the array of values to join together, may be null
     * @param separator the separator character to use, null treated as ""
     * @return the joined String, {@code null} if null array input
     */
    public static String join(Object[] values, String separator) {
        if (values == null) {
            return null;
        }
        return join(Arrays.asList(values), separator);
    }

    /**
     * <p>
     * Joins the elements of the provided collection into a single String containing the provided list of elements.
     * </p>
     * <p>
     * <p>
     * No delimiter is added before or after the list. A {@code null} separator is the same as an empty String ("").
     * Null objects or empty strings within the array are represented by empty strings.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.join(null, *)                = null
     * StringUtils.join([], *)                  = ""
     * StringUtils.join([null], *)              = ""
     * StringUtils.join(["a", "b", "c"], "--")  = "a--b--c"
     * StringUtils.join(["a", "b", "c"], null)  = "abc"
     * StringUtils.join(["a", "b", "c"], "")    = "abc"
     * StringUtils.join([null, "", "a"], ',')   = ",,a"
     * </pre>
     *
     * @param values the collection of values to join together, may be null
     * @param separator the separator character to use, null treated as ""
     * @return the joined String, {@code null} if null array input
     */
    public static String join(Iterable<?> values, String separator) {
        if (values == null) {
            return null;
        }
        if (separator == null) {
            separator = EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        boolean prependComma = false;
        for (Object value : values) {
            if (prependComma) {
                builder.append(separator);
            }
            if (value != null) {
                builder.append(value);
            }
            prependComma = true;
        }
        return builder.toString();
    }

    /**
     * <p>
     * Checks if a CharSequence is empty ("") or null.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("boo")     = false
     * StringUtils.isEmpty("  boo  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * <p>
     * Checks if a CharSequence is not empty ("") and not null.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("boo")     = true
     * StringUtils.isNotEmpty("  boo  ") = true
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * <p>
     * Checks if a CharSequence is whitespace, empty ("") or null.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("boo")     = false
     * StringUtils.isBlank("  boo  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     */
    public static boolean isBlank(final CharSequence cs) {
        if (isEmpty(cs)) {
            return true;
        }
        int strLen = cs.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * Checks if a CharSequence is not empty (""), not null and not whitespace only.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("boo")     = true
     * StringUtils.isNotBlank("  boo  ") = true
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null and not whitespace
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * <p>
     * Compares two Strings, returning {@code true} if they represent equal sequences of characters.
     * </p>
     * <p>
     * <p>
     * {@code null}s are handled without exceptions. Two {@code null} references are considered to be equal. The
     * comparison is case sensitive.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.equals(null, null)   = true
     * StringUtils.equals(null, "boo")  = false
     * StringUtils.equals("boo", null)  = false
     * StringUtils.equals("boo", "boo") = true
     * StringUtils.equals("boo", "BOO") = false
     * </pre>
     *
     * @param str1 the first String, may be {@code null}
     * @param str2 the second String, may be {@code null}
     * @return {@code true} if the Strings are equal (case-sensitive), or both {@code null}
     * @see String#equals(Object)
     */
    public static boolean equals(final String str1, final String str2) {
        return Objects.equals(str1, str2);
    }

    /**
     * <p>
     * Compares two Strings, returning {@code false} if they represent equal sequences of characters.
     * </p>
     * This method is inverse to {@link #equals(String, String)}.
     * <p>
     *
     * <pre>
     * StringUtils.notEquals(null, null)   = false
     * StringUtils.notEquals(null, "boo")  = true
     * StringUtils.notEquals("boo", null)  = true
     * StringUtils.notEquals("boo", "boo") = false
     * StringUtils.notEquals("boo", "BOO") = true
     * </pre>
     *
     * @param str1 the first String, may be {@code null}
     * @param str2 the second String, may be {@code null}
     * @return {@code false} if the Strings are equal (case-sensitive), or both {@code null}
     * @see #equals(String, String)
     */
    public static boolean notEquals(final String str1, final String str2) {
        return !equals(str1, str2);
    }

    /**
     * <p>
     * Checks if String contains a search String irrespective of case, handling {@code null}. Case-insensitivity is
     * defined as by {@link String#equalsIgnoreCase(String)}.
     * <p>
     * <p>
     * A {@code null} String will return {@code false}.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.contains(null, *)     = false
     * StringUtils.contains(*, null)     = false
     * StringUtils.contains("", "")      = true
     * StringUtils.contains("abc", "")   = true
     * StringUtils.contains("abc", "a")  = true
     * StringUtils.contains("abc", "Bc") = true
     * StringUtils.contains("abc", "z")  = false
     * StringUtils.contains("abc", "Z")  = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param searchStr the String to find, may be null
     * @return true if the String contains the search String irrespective of case or false if not or {@code null} string
     *         input
     */
    public static boolean containsIgnoreCase(final String str, final String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        final int len = searchStr.length();
        final int end = str.length() - len;
        for (int i = 0; i <= end; i++) {
            if (str.regionMatches(true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Do the same as {@link java.lang.String#matches(String)}
     *
     * @param regex a Pattern to which this string is to be matched
     * @param input an input string to match regexp Pattern
     * @return {@code true} if, and only if, this string matches the given regular expression
     */
    public static boolean matches(Pattern regex, CharSequence input) {
        Matcher m = regex.matcher(input);
        return m.matches();
    }

    /**
     * <p>
     * Removes control characters (char &lt;= 32) from both ends of this String, handling {@code null} by returning
     * {@code null}.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.trim(null)          = null
     * StringUtils.trim("")            = ""
     * StringUtils.trim("     ")       = ""
     * StringUtils.trim("boo")         = "boo"
     * StringUtils.trim("    boo    ") = "boo"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(final String str) {
        return str == null ? null : str.trim();
    }

    public static String trimStart(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        int start = 0;
        while (start != strLen && Character.isWhitespace(str.charAt(start))) {
            start++;
        }
        return str.substring(start);
    }

    public static String trimEnd(final String str) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }
        while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
            end--;
        }
        return str.substring(0, end);
    }

    /**
     * <p>
     * Removes control characters (char &lt;= 32) from both ends of this String returning {@code null} if the String is
     * empty ("") after the trim or if it is {@code null}.
     * <p>
     *
     * <pre>
     * StringUtils.trimToNull(null)          = null
     * StringUtils.trimToNull("")            = null
     * StringUtils.trimToNull("     ")       = null
     * StringUtils.trimToNull("boo")         = "boo"
     * StringUtils.trimToNull("    boo    ") = "boo"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String, {@code null} if only chars &lt;= 32, empty or null String input
     */
    public static String trimToNull(final String str) {
        final String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    /**
     * <p>
     * Removes control characters (char &lt;= 32) from both ends of this String returning an empty String ("") if the
     * String is empty ("") after the trim or if it is {@code null}.
     * <p>
     *
     * <pre>
     * StringUtils.trimToEmpty(null)          = ""
     * StringUtils.trimToEmpty("")            = ""
     * StringUtils.trimToEmpty("     ")       = ""
     * StringUtils.trimToEmpty("boo")         = "boo"
     * StringUtils.trimToEmpty("    boo    ") = "boo"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String, or an empty String if {@code null} input
     */
    public static String trimToEmpty(final String str) {
        return str == null ? EMPTY : str.trim();
    }

    /**
     * <p>
     * Removes a substring only if it is at the beginning of a source string, otherwise returns the source string.
     * </p>
     * <p>
     * <p>
     * A {@code null} source string will return {@code null}. An empty ("") source string will return the empty string.
     * A {@code null} search string will return the source string.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.removeStart(null, *)                      = null
     * StringUtils.removeStart("", *)                        = ""
     * StringUtils.removeStart(*, null)                      = *
     * StringUtils.removeStart("boo", "")                    = "boo"
     * StringUtils.removeStart("www.example.com", "www.")    = "example.com"
     * StringUtils.removeStart("example.com", "www.")        = "example.com"
     * StringUtils.removeStart("www.example.com", "example") = "www.example.com"
     * </pre>
     *
     * @param str the source String to search, may be null
     * @param remove the String to search for and remove, may be null
     * @return the substring with the string removed if found, {@code null} if null String input
     */
    public static String removeStart(final String str, final String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.startsWith(remove)) {
            return str.substring(remove.length());
        }
        return str;
    }

    /**
     * <p>
     * Removes a substring only if it is at the end of a source string, otherwise returns the source string.
     * </p>
     * <p>
     * <p>
     * A {@code null} source string will return {@code null}. An empty ("") source string will return the empty string.
     * A {@code null} search string will return the source string.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.removeEnd(null, *)                      = null
     * StringUtils.removeEnd("", *)                        = ""
     * StringUtils.removeEnd(*, null)                      = *
     * StringUtils.removeEnd("boo", "")                    = "boo"
     * StringUtils.removeEnd("www.example.com", ".com")    = "www.example"
     * StringUtils.removeEnd("www.example", ".com")        = "www.example"
     * StringUtils.removeEnd("www.example.com", "example") = "www.example.com"
     * </pre>
     *
     * @param str the source String to search, may be null
     * @param remove the String to search for and remove, may be null
     * @return the substring with the string removed if found, {@code null} if null String input
     */
    public static String removeEnd(final String str, final String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

    /**
     * <p>
     * Capitalizes a String changing the first letter to title case as per {@link Character#toTitleCase(char)}. No other
     * letters are changed.
     * </p>
     * <p>
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.capitalize(null)  = null
     * StringUtils.capitalize("")    = ""
     * StringUtils.capitalize("foo") = "Foo"
     * StringUtils.capitalize("fOo") = "FOo"
     * </pre>
     *
     * @param str the String to capitalize, may be null
     * @return the capitalized String, {@code null} if null String input
     * @see #uncapitalize(String)
     */
    public static String capitalize(final String str) {
        if (isEmpty(str)) {
            return str;
        }

        char firstChar = str.charAt(0);
        if (Character.isTitleCase(firstChar)) {
            // already capitalized
            return str;
        }

        return new StringBuilder(str.length()).append(Character.toTitleCase(firstChar))
            .append(str, 1, str.length())
            .toString();
    }

    /**
     * <p>
     * Uncapitalizes a String changing the first letter to title case as per {@link Character#toLowerCase(char)}. No
     * other letters are changed.
     * </p>
     * <p>
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     * <p>
     *
     * <pre>
     * StringUtils.uncapitalize(null)  = null
     * StringUtils.uncapitalize("")    = ""
     * StringUtils.uncapitalize("Foo") = "foo"
     * StringUtils.uncapitalize("FOO") = "fOO"
     * </pre>
     *
     * @param str the String to uncapitalize, may be null
     * @return the uncapitalized String, {@code null} if null String input
     * @see #capitalize(String)
     */
    public static String uncapitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }

        char firstChar = str.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            // already uncapitalized
            return str;
        }

        return new StringBuilder(strLen).append(Character.toLowerCase(firstChar)).append(str.substring(1)).toString();
    }

    /**
     * Print an Object to a printer.
     *
     * @param ary array or Object
     * @param printer an appendable implementation
     * @return the same printer instance
     */
    public static Appendable print(Object ary, Appendable printer) {
        if (ary == null) {
            return printer;
        }
        try {
            if (ary.getClass().isArray()) {
                int len = Array.getLength(ary);

                printer.append('[');

                for (int i = 0; i < len; i++) {
                    printer.append(String.valueOf(Array.get(ary, i)));
                    if (i < len - 1) {
                        printer.append(", ");
                    }
                }
                printer.append(']');
            } else {
                printer.append(String.valueOf(ary));
            }
        } catch (IOException ex) {
            // Ignore
        }
        return printer;
    }

    /**
     * Defines a functor interface implemented by classes that perform a predicate test on a character.
     * <p>
     * A <code>Predicate</code> is the object equivalent of an <code>if</code> statement. It uses the input object to
     * return a true or false value, and is often used in validation or filtering.
     * <p>
     */
    private interface Predicate {

        /**
         * Use the specified parameter to perform a test that returns true or false.
         *
         * @param ch the character to evaluate
         * @return true or false
         */
        boolean evaluate(char ch);

    }
}
