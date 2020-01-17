package org.openl.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class StringTool {

    public static final String NEW_LINE = "\n";
    private static final Pattern PLUS = Pattern.compile("\\+");

    // TODO Move to URLUtils class
    public static String encodeURL(String url) {
        String encodedUrl;
        if (StringUtils.isBlank(url)) {
            return url;
        }
        try {
            encodedUrl = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        encodedUrl = PLUS.matcher(encodedUrl).replaceAll("%20");
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

    public static String[] tokenize(String src, String delim) {
        StringTokenizer st = new StringTokenizer(src, delim);
        int cnt = st.countTokens();
        String[] res = new String[cnt];
        for (int i = 0; i < res.length; i++) {
            res[i] = st.nextToken();
        }
        return res;
    }

    /**
     * Split the string by the splitSymbol. To avoid splitting escapeSymbol is used. Trims the splitted result. For
     * examples see tests.
     *
     * @param src source to process. Can`t be <code>null</code>.
     * @param splitSymbol the delimiting symbol. Can`t be <code>null</code>.
     * @param escapeSymbol the escaper, that is used to break splitting by splitSymbol. If <code>null</code>, the
     *            splitSymbol array will be returned.
     * @return the array of strings computed by splitting this string around matches of the given splitSymbol and
     *         escaped by escaper.
     */
    public static String[] splitAndEscape(String src, String splitSymbol, String escapeSymbol) {
        String[] result;
        String[] tokens = src.split(splitSymbol);
        List<String> resultList = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        if (escapeSymbol != null) {
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].endsWith(escapeSymbol)) {
                    String noEscapeToken = tokens[i].substring(0, tokens[i].length() - 1);
                    if (buf.length() == 0) {
                        buf.append(trimStart(noEscapeToken));
                    } else {
                        buf.append(noEscapeToken);
                    }
                    buf.append(splitSymbol);
                } else {
                    if (buf.length() == 0) {
                        tokens[i] = tokens[i].trim();
                        resultList.add(tokens[i]);
                    } else {
                        buf.append(trimEnd(tokens[i]));
                        resultList.add(buf.toString());
                        buf.delete(0, buf.length());
                    }
                }
            }
            result = resultList.toArray(EMPTY_STRING_ARRAY);
        } else {
            result = tokens;
            for (int i = 0; i < tokens.length; i++) {
                result[i] = result[i] != null ? result[i].trim() : null;
            }
        }

        return result;
    }

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static String trimStart(final String str) {
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

    private static String trimEnd(final String str) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }
        while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
            end--;
        }
        return str.substring(0, end);
    }
}
