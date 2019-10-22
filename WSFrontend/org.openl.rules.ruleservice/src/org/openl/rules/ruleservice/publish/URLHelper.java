package org.openl.rules.ruleservice.publish;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.slf4j.LoggerFactory;

public final class URLHelper {

    private URLHelper() {
    }

    public static String processURL(String url) {
        String ret = url;
        while (ret.charAt(0) == '/') {
            ret = ret.substring(1);
        }
        String[] parts = ret.split("/");
        StringBuilder sb = new StringBuilder();
        boolean f = false;
        for (String s : parts) {
            try {
                if (!f) {
                    f = true;
                } else {
                    sb.append("/");
                }
                sb.append(URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20"));
            } catch (UnsupportedEncodingException e) {
                sb.append(s);
            }
        }
        try {
            URI uri = new URI(sb.toString());
            uri = uri.normalize();
            return uri.toString();
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(URLHelper.class).error("URL processing has been failed.", e);
            return url;
        }
    }
}
