package org.openl.rules.webstudio.web.util;

import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class Utils {
    private String dateTimeFormat;

    @PostConstruct
    public void init() {
        dateTimeFormat = WebStudioFormats.getInstance().dateTime();
    }

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

    public static String getDescriptiveVersion(ProjectVersion version, String dateTimeFormat) {
        VersionInfo versionInfo = version.getVersionInfo();
        if (versionInfo == null) {
            return "Version not found";
        }
        String modifiedOnStr = new SimpleDateFormat(dateTimeFormat).format(versionInfo.getCreatedAt());
        return versionInfo.getCreatedBy() + ": " + modifiedOnStr;
    }

    public String getDescriptiveVersion(ProjectVersion version) {
        return getDescriptiveVersion(version, dateTimeFormat);
    }
}
