package org.openl.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
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
        if (StringUtils.isBlank(url)) {
            return url;
        }
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return null;
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
        String[] tokens = src.split("(?=" + splitSymbol + ")");
        boolean f = tokens[0].startsWith(splitSymbol);
        tokens = Arrays.stream(tokens)
            .map(e -> e.startsWith(splitSymbol) ? (e.length() > 1 ? e.substring(1) : null) : e)
            .toArray(String[]::new);
        List<String> resultList = new ArrayList<>();
        if (f) {
            resultList.add(null);
        }
        StringBuilder buf = new StringBuilder();
        if (escapeSymbol != null) {
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i] != null && tokens[i].endsWith(escapeSymbol)) {
                    String noEscapeToken = tokens[i].substring(0, tokens[i].length() - 1);
                    if (buf.length() == 0) {
                        buf.append(trimStart(noEscapeToken));
                    } else {
                        buf.append(noEscapeToken);
                    }
                    buf.append(splitSymbol);
                } else {
                    if (buf.length() == 0) {
                        tokens[i] = tokens[i] != null ? tokens[i].trim() : null;
                        resultList.add(tokens[i]);
                    } else {
                        buf.append(trimEnd(tokens[i] != null ? tokens[i] : StringUtils.EMPTY));
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
