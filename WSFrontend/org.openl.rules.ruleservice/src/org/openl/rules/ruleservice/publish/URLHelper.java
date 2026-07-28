package org.openl.rules.ruleservice.publish;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

public final class URLHelper {

    private URLHelper() {
    }

    public static String processURL(String url) {
        var ret = url;
        while (ret.charAt(0) == '/') {
            ret = ret.substring(1);
        }
        var parts = ret.split("/", -1);
        var sb = new StringBuilder();
        var f = false;
        for (String s : parts) {
            if (!f) {
                f = true;
            } else {
                sb.append("/");
            }
            sb.append(URLEncoder.encode(s, StandardCharsets.UTF_8).replaceAll("\\+", "%20"));
        }
        try {
            var uri = new URI(sb.toString());
            uri = uri.normalize();
            return uri.toString();
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(URLHelper.class).error("URL processing has been failed.", e);
            return url;
        }
    }
}
