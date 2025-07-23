package org.openl.rules.webstudio.web.util;

import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;

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

    public String toUrl(String path1, String path2, String path3) {
        return makeUrl(path1, path2, path3);
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
        if (versionInfo == null || versionInfo.getCreatedAt() == null || versionInfo.getCreatedBy() == null) {
            return "Version not found";
        }
        String modifiedOnStr = new SimpleDateFormat(dateTimeFormat).format(versionInfo.getCreatedAt());
        return versionInfo.getCreatedBy() + ": " + modifiedOnStr;
    }

    public String getDescriptiveVersion(ProjectVersion version) {
        return getDescriptiveVersion(version, dateTimeFormat);
    }

    public boolean supportsMappedFolders(AProjectArtefact artefact) {
        if (artefact == null) {
            return false;
        }
        Repository repository;
        if (artefact instanceof UserWorkspaceProject) {
            repository = ((UserWorkspaceProject) artefact).getDesignRepository();
            if (repository == null) {
                repository = artefact.getRepository();
            }
        } else {
            repository = artefact.getRepository();
        }
        return repository.supports().mappedFolders();
    }

    public String descriptiveProjectVersion(AProjectArtefact artefact) {
        if (artefact == null || artefact.getVersion() == null) {
            return "";
        }
        return getDescriptiveVersion(artefact.getVersion(), dateTimeFormat);
    }
}
