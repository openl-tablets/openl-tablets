package org.openl.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class StringTool {

    public static final String NEW_LINE = "\n";
    private static final Pattern PLUS = Pattern.compile("\\+");

    // TODO Move to URLUtils class
    public static String encodeURL(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
        encodedUrl = PLUS.matcher(encodedUrl).replaceAll("%20");
        return encodedUrl;
    }

    // TODO Move to URLUtils class
    public static String decodeURL(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

}
