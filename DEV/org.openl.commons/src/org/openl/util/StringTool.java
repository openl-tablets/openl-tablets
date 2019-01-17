package org.openl.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringTool {

    public static final String NEW_LINE = "\n";

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
            result = resultList.toArray(new String[0]);
        } else {
            result = tokens;
        }

        return result;
    }
}
