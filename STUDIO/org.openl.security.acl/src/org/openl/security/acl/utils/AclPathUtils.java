package org.openl.security.acl.utils;

import org.openl.util.StringUtils;

public final class AclPathUtils {

    private AclPathUtils() {
    }

    public static String concatPaths(String path1, String path2) {
        if (StringUtils.isBlank(path1)) {
            return path2;
        }
        if (StringUtils.isBlank(path2)) {
            return path1;
        }
        var result = path1;
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result + path2;
    }

    public static String buildRepositoryPath(String repoId, String path) {
        if (StringUtils.isBlank(path)) {
            return repoId;
        }

        var parts = path.split("/");
        var normalizedPath = new StringBuilder(repoId);

        int i = 0;
        for (var part : parts) {
            if (!StringUtils.isBlank(part)) {
                if (i == 0) {
                    normalizedPath.append(':');
                }
                normalizedPath.append('/').append(part.trim());
                i++;
            }
        }

        return normalizedPath.toString();
    }

}
