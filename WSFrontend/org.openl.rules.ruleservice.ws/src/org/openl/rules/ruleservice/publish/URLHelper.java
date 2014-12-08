package org.openl.rules.ruleservice.publish;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class URLHelper {
    private URLHelper() {
    }

    public static String processURL(String url) {
        String[] parts = url.split("/");
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : parts) {
            if (first) {
                first = false;
            } else {
                sb.append("/");
            }
            try {
                sb.append(URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20"));
            } catch (UnsupportedEncodingException e) {
                sb.append(s);
            }
        }

        String ret = sb.toString();
        while (ret.charAt(0) == '/') {
            ret = ret.substring(1);
        }

        return ret;
    }
}
