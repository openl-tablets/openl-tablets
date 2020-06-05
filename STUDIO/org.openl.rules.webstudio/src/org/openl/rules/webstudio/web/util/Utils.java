package org.openl.rules.webstudio.web.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Controller;

@Controller
public class Utils {

    public String toJSText(String str) {
        return str == null ? null
                           : str.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'").replace("/", "\\/");
    }

    public String toUrl(String path) {
        return makeUrl(path);
    }

    public String toUrl(String path1, String path2) {
        return makeUrl(path1, path2);
    }

    public String makeUrl(String... path) {
        return "#" + Stream.of(path)
            .filter(StringUtils::isNotBlank)
            .map(StringTool::encodeURL)
            .collect(Collectors.joining("/"));
    }

    public String encode(String name) {
        return StringTool.encodeURL(name);
    }

}
