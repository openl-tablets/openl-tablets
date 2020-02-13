package org.openl.rules.webstudio.web;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.openl.util.StringTool;
import org.openl.util.StringUtils;

@ManagedBean
@ApplicationScoped
public class Utils {

    public String toJSText(String str) {
        return str == null ? null : str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("/", "\\/");
    }

    public String toUrl(String... path) {
        return "#" + Stream.of(path).filter(StringUtils::isNotBlank).map(StringTool::encodeURL).collect(Collectors.joining("/"));
    }

    public String encode(String name) {
        return StringTool.encodeURL(name);
    }

}
