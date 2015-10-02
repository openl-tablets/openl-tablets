package org.openl.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class StringTool {

    public static final String NEW_LINE = "\n";
    private static final String COMMA = ",";

    public interface Convertor {
        void convert(char c, int idx, StringBuilder out);
    }

    public interface MacroKeyHandler {
        void handleKey(String key, MacroSubst ms, StringBuilder out);
    }

    public static class MacroSubst extends TextTransformer {

        public char _macroDelim;
        public Map<String, String> _macros;
        MacroKeyHandler _mkh;

        public MacroSubst(Map<String, String> macros, char macroDelim, MacroKeyHandler mkh) {
            _macros = macros;
            _macroDelim = macroDelim;
            _mkh = mkh;
        }

        /**
         * cur == delim delim != delim !=delim status == 1 0 0 1
         * ====================================================== flush out tmp
         * status=0 status=1
         */

        @Override
        public void process() {
            if (cur == _macroDelim) {
                if (status == 0) {
                    status = 1;
                } else {
                    status = 0;
                    String key = flushTmp();
                    String res = (String) _macros.get(key);
                    if (res == null) {
                        _mkh.handleKey(key, this, out);
                    } else {
                        out.append(res);
                    }
                }
            } else if (status == 0) {
                out.append(cur);
            } else {
                tmp().append(cur);
            }
        }

        // protected void handleUnknownKey(String key)
        // {
        // switch(_mode)
        // {
        // case COPY:
        // out.append(_macroDelim).append(key).append(_macroDelim);
        // break;
        // case ERROR:
        // throw new RuntimeException("Macro key: " + key + " is not found");
        // case EMPTY:
        // }
        // do nothing
        // throw new RuntimeException("Key " + key + " is not found");
        // }
    }

    public interface Selector {
        boolean select(char c, int idx);
    }

    static public class TextTransformer {
        static final public char EOF = (char) -1;
        protected char prev = EOF, cur = EOF, next = EOF;
        protected StringBuilder out = null;
        private StringBuilder tmp = null;
        protected int len = -1;
        protected int status = 0;
        protected int idx = 0;

        public String flushTmp() {
            String s = tmp().toString();
            tmp.setLength(0);
            return s;
        }

        public void process() {
            out.append(cur);
        }

        public final StringBuilder tmp() {
            if (tmp == null) {
                tmp = new StringBuilder();
            }
            return tmp;
        }

        public String transform(String src) {
            return transform(src, new StringBuilder()).toString();
        }

        public StringBuilder transform(String src, StringBuilder buf) {
            out = buf;
            len = src.length();
            if (len > 0) {
                next = src.charAt(0);
            }

            for (idx = 0; idx < len; idx++) {
                prev = cur;
                cur = next;
                next = idx + 1 < len ? src.charAt(idx + 1) : EOF;

                process();
            }

            veryEnd();

            return out;

        }

        public void veryEnd() {
        }
    }

    static public Convertor IGNORE = new Convertor() {
        public void convert(char c, int idx, StringBuilder out) {
        }
    };

    static public Convertor UPPER = new Convertor() {
        public void convert(char c, int idx, StringBuilder out) {
            out.append(Character.toUpperCase(c));
        }
    };

    static public Convertor LOWER = new Convertor() {
        public void convert(char c, int idx, StringBuilder out) {
            out.append(Character.toLowerCase(c));
        }
    };

    static public final MacroKeyHandler MKH_DONOTHING = new MacroKeyHandler() {
        public void handleKey(String key, MacroSubst ms, StringBuilder out) {
        }
    };

    static public final MacroKeyHandler MKH_LEAVE = new MacroKeyHandler() {
        public void handleKey(String key, MacroSubst ms, StringBuilder out) {
            out.append(ms._macroDelim).append(key).append(ms._macroDelim);
        }
    };

    static public final MacroKeyHandler MKH_ERROR = new MacroKeyHandler() {
        public void handleKey(String key, MacroSubst ms, StringBuilder out) {
            throw new RuntimeException("Key " + key + " not found");
        }
    };

    public static StringBuilder append(StringBuilder buf, char c, int n) {
        for (int i = 0; i < n; i++) {
            buf.append(c);
        }
        return buf;
    }

    /**
     * Create hexadecimal string representation of a specified number of bytes
     * from array (padded with 0s)
     * 
     * @param src source byte array
     * @param off offset
     * @param len length
     * @return hex string
     */
    public static String byteArrayToHexString(byte[] src, int off, int len) {
        StringBuilder out = new StringBuilder();
        for (int i = off; i < off + len; i++) {
            String s = Integer.toHexString(src[i] & 0xFF);
            if (s.length() % 2 != 0) {
                out.append("0");
            }
            out.append(s);
        }
        return out.toString();
    }

    /**
     * See examples below: 1) Assert.assertEquals("url",
     * StringTool.decapitalizeName("URL", "_")); 2)
     * Assert.assertEquals("driver", StringTool.decapitalizeName("Driver",
     * "_")); 3) Assert.assertEquals("test_url",
     * StringTool.decapitalizeName("TestURL", "_")); 4)
     * Assert.assertEquals("testurl", StringTool.decapitalizeName("testURL",
     * null)); 5) Assert.assertEquals("test_url_code",
     * StringTool.decapitalizeName("TestURLCode", "_")); 6)
     * Assert.assertEquals("url_code", StringTool.decapitalizeName("URLCode",
     * "_"));
     */

    public static String decapitalizeName(String capitalized, String separator) {
        return decapitalizeName(capitalized, separator, new StringBuilder()).toString();
    }

    public static StringBuilder decapitalizeName(String capitalized, String separator, StringBuilder buf) {
        if (capitalized == null) {
            return buf;
        }
        if (separator == null) {
            separator = "";
        }

        // StringBuilder buf = new StringBuilder();
        int start = 0;
        boolean prevUP = false;

        char[] src = capitalized.toCharArray();

        for (int i = 0; i < src.length; i++) {
            char c = src[i];

            if (Character.isUpperCase(c)) {
                if (!prevUP) {
                    prevUP = true;
                    if (i > start) {
                        buf.append(src, start, i - start);
                        start = i;
                        buf.append(separator);
                    }
                }
                src[i] = Character.toLowerCase(c);
            } else // lower case
            {
                if (prevUP) {
                    prevUP = false;
                    int len = i - start;

                    if (len > 1) {
                        buf.append(src, start, len - 1);
                        start = i - 1;
                        buf.append(separator);
                    }
                }
            } // else
        } // for

        buf.append(src, start, src.length - start);
        return buf;
    }

    // TODO Move to URLUtils class
    public static String encodeURL(String url) {
        String encodedUrl = null;
        if (StringUtils.isBlank(url)) {
            return url;
        }
        try {
            encodedUrl = URLEncoder.encode(url, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodedUrl;
    }

    // TODO Move to URLUtils class
    public static String decodeURL(String url) {
        String decodedUrl = null;
        if (StringUtils.isBlank(url)) {
            return url;
        }
        try {
            decodedUrl = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decodedUrl;
    }

    /**
     * Transforms String using the following rule: if c is not selected,
     * convertor is called to transform it, otherwise c is put into output
     */
    public static String filter(String src, Selector sel, Convertor conv) {
        return filter(src, sel, conv, new StringBuilder()).toString();
    }

    /**
     * Transforms String using the following rule: if c is not selected,
     * convertor is called to transform it, otherwise c is put into output
     */
    public static StringBuilder filter(String src, Selector sel, Convertor conv, StringBuilder buf) {
        int len = src.length();

        for (int i = 0; i < len; i++) {
            char c = src.charAt(i);
            if (!sel.select(c, i)) {
                conv.convert(c, i, buf);
            } else {
                buf.append(c);
            }
        }

        return buf;
    }

    public static String firstToken(String src, String delim) {
        String[] tokens = tokenize(src, delim);
        return tokens.length > 0 ? tokens[0] : "";
    }

    public static int indexOfClosingBracket(String src, char openingBracket, char closingBracket, int fromIndex) {
        int len = src.length();
        int cnt = 1;
        for (int i = fromIndex; i < len; i++) {
            char c = src.charAt(i);

            if (c == closingBracket) {
                if (--cnt == 0) {
                    return i;
                }
            } else if (c == openingBracket) {
                ++cnt;
            }
        }
        return -1;
    }

    static public boolean isSpace(char c) {
        return c <= ' ' || Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    public static String keepChars(String src, String toKeep) {
        StringBuilder buf = new StringBuilder(src.length());
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (toKeep.indexOf(c) >= 0) {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    public static String lastToken(String src, String delim) {
        String[] tokens = tokenize(src, delim);
        return tokens.length > 0 ? tokens[tokens.length - 1] : "";
    }

    public static String macroSubst(String src, Map<String, String> macros, char macroDelim, MacroKeyHandler mkh) {
        return macroSubst(src, macros, macroDelim, mkh, new StringBuilder()).toString();
    }

    public static StringBuilder macroSubst(String src, Map<String, String> macros, char macroDelim, MacroKeyHandler mkh,
            StringBuilder buf) {
        MacroSubst ms = new MacroSubst(macros, macroDelim, mkh);
        return ms.transform(src, buf);
    }

    public static String makeJavaIdentifier(String src) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (i == 0) {
                buf.append(Character.isJavaIdentifierStart(c) ? c : '_');
            } else {
                buf.append(Character.isJavaIdentifierPart(c) ? c : '_');
            }
        }

        return buf.toString();
    }

    public static String[] openBrackets(String src, char openingBracket, char closingBracket, String ignore) {
        int len = src.length();
        List<String> v = new ArrayList<String>();

        for (int i = 0; i < len; i++) {
            char c = src.charAt(i);

            if (c == openingBracket) {
                int closed = indexOfClosingBracket(src, openingBracket, closingBracket, i + 1);
                if (closed == -1) {
                    throw new RuntimeException("Expected: " + closingBracket);
                }

                v.add(src.substring(i + 1, closed));
                i = closed;
                continue;
            }

            if (ignore.indexOf(c) == -1) {
                throw new RuntimeException("UnExpected: " + c);
            }

        }

        return (String[]) v.toArray(new String[v.size()]);
    }

    public static String replace(String src, String toFind, String toReplace) {
        return replace(src, toFind, toReplace, true, false, new StringBuilder()).toString();
    }

    public static StringBuilder replace(String src, String toFind, String toReplace, boolean all, boolean ignoreCase,
            StringBuilder out) {
        int find_len = toFind.length();
        int src_len = src.length();
        int replace_len = toReplace.length();
        int start = 0;

        String test_src = src;
        String test_tofind = toFind;
        if (ignoreCase) {
            test_src = src.toLowerCase();
            test_tofind = toFind.toLowerCase();
        }

        while (start + find_len <= src_len) {
            int idx = test_src.indexOf(test_tofind, start);
            if (idx < 0) {
                break;
            }

            if (start != idx) {
                out.append(src.substring(start, idx));
            }
            if (replace_len > 0) {
                out.append(toReplace);
            }
            start = idx + find_len;
            if (!all) {
                break;
            }
        }

        if (start < src_len) {
            out.append(src.substring(start));
        }
        return out;
    }

    public static String[] splitLines(Reader reader) {
        BufferedReader br = new BufferedReader(reader);
        List<String> v = new ArrayList<String>();
        String s;

        try {
            try {
                while ((s = br.readLine()) != null) {
                    v.add(s);
                }
                return (String[]) v.toArray(new String[v.size()]);
            } finally {
                br.close();
            }
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

    }

    public static String[] splitLines(String src) {
        return splitLines(new StringReader(src));
    }

    public static String[] tokenize(String src, String delim) {
        StringTokenizer st = new StringTokenizer(src, delim);
        int cnt = st.countTokens();
        String[] res = new String[cnt];
        for (int i = 0; i < res.length; i++) {
            res[i] = st.nextToken();
        }
        return res;
    }

    static public String untab(String src, int tabSize) {
        StringBuilder buf = new StringBuilder(src.length() + 10);

        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c != '\t') {
                buf.append(c);
            } else {
                buf.append(' ');

                int extra = buf.length() % tabSize;
                if (extra != 0) {
                    append(buf, ' ', tabSize - extra);
                }
            }
        }
        return buf.toString();
    }

    static public String getFileNameOfJavaClass(Class<?> c) {
        return c.getName().replace('.', '/') + ".java";
    }

    /**
     * Split the string by the splitSymbol. To avoid splitting escapeSymbol
     * is used. Trims the splitted result. For examples see tests.
     * 
     * @param src source to process. Can`t be <code>null</code>.
     * @param splitSymbol the delimiting symbol. Can`t be <code>null</code>.
     * @param escapeSymbol the escaper, that is used to break splitting by
     *            splitSymbol. If <code>null</code>, the splitSymbol array
     *            will be returned.
     * @return the array of strings computed by splitting this string around
     *         matches of the given splitSymbol and escaped by escaper.
     */
    public static String[] splitAndEscape(String src, String splitSymbol, String escapeSymbol) {
        String[] result;
        String[] tokens = src.split(splitSymbol);
        List<String> resultList = new ArrayList<String>();
        StringBuilder buf = new StringBuilder();
        if (escapeSymbol != null) {
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].endsWith(escapeSymbol)) {
                    String noEscapeToken = tokens[i].substring(0, tokens[i].length() - 1);
                    buf.append(noEscapeToken).append(splitSymbol);
                } else {
                    if (buf.length() == 0) {
                        tokens[i] = tokens[i].trim();
                        resultList.add(tokens[i]);
                    } else {
                        buf.append(tokens[i]);
                        resultList.add(buf.toString());
                        buf.delete(0, buf.length());
                    }
                }
            }
            result = (String[]) resultList.toArray(new String[0]);
        } else {
            result = tokens;
        }

        return result;
    }

    public static String insertStringToString(String baseStr, String strToInsertBefore, String insertion) {
        String src = baseStr;
        String[] tokens = src.split(strToInsertBefore);
        StringBuilder strBuf = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            strBuf.append(token);
            if (!(i == tokens.length - 1)) {
                strBuf.append(insertion);
                strBuf.append(strToInsertBefore);
            }
        }
        return strBuf.toString();
    }

    public static String arrayToStringThroughSymbol(Object[] values, String symbol) {
        if (ArrayUtils.isNotEmpty(values)) {
            List<String> objectStrings = new ArrayList<String>();
            for (Object value : values) {
                objectStrings.add(value.toString());
            }
            return listToStringThroughSymbol(objectStrings, symbol);
        }
        return null;
    }

    public static String listToStringThroughSymbol(List<String> values, String symbol) {
        String result = StringUtils.EMPTY;
        if (StringUtils.isBlank(symbol)) {
            symbol = COMMA;
        }
        if (values != null && !values.isEmpty()) {
            StringBuilder strBuf = new StringBuilder();
            int paramNum = values.size();
            for (String value : values) {
                paramNum--;
                strBuf.append(value);
                if (paramNum > 0) {
                    strBuf.append(symbol).append(' ');
                }
            }
            result = strBuf.toString();
        }
        return result;
    }

    public static String listObjectToStringThroughSymbol(List<?> values, String symbol) {
        if (values == null || values.isEmpty()) {
            return StringUtils.EMPTY;
        }

        List<String> strList = new ArrayList<String>(values.size());
        for (Object obj : values) {
            if (obj != null) {
                strList.add(obj.toString());
            }
        }
        return listToStringThroughSymbol(strList, symbol);
    }


    /**
     * Returns the setter name, by adding set, to the field name, and upper case
     * the first field name symbol.
     * 
     * @param fieldName
     * @return setFieldName
     */
    public static String getSetterName(String fieldName) {
        final StringBuilder builder = new StringBuilder(64);
        return builder.append("set").append(Character.toUpperCase(fieldName.charAt(0))).append(fieldName.substring(1)).toString();
    }

    public static String getGetterName(String fieldName) {
        final StringBuilder builder = new StringBuilder(64);
        return builder.append("get").append(Character.toUpperCase(fieldName.charAt(0))).append(fieldName.substring(1)).toString();
    }

    /**
     * Builds the type name with namespace.
     * 
     * @param namespace for typeName
     * @param typeName
     * @return namespace::typeName
     */
    public static final String buildTypeName(String namespace, String typeName) {
        final StringBuilder builder = new StringBuilder(64);
        return builder.append(namespace).append("::").append(typeName).toString();
    }

}
