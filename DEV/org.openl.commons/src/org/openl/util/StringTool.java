package org.openl.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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

}
