package org.openl.rules.webstudio.web.repository.upload;

import java.util.Set;

import org.openl.rules.workspace.filter.PathFilter;
import org.openl.util.StringUtils;

/**
 * Ectractor for folder name paths from the top root folder.
 *
 * @author DLiauchuk
 *
 */
public class RootFolderExtractor {
    private Set<String> folderNames;
    private String rootName;
    private PathFilter filter;

    public RootFolderExtractor(Set<String> folderNames, PathFilter filter) {
        this.folderNames = folderNames;
        this.filter = filter;
        initRootFolderPath();
    }

    /**
     * Inits the root folder name is exists from the set of folder names.
     */
    private void initRootFolderPath() {
        if (needToExtract()) {
            for (String folderName : folderNames) {
                int ind = folderName.indexOf('/');
                if (ind > 0) {
                    rootName = folderName.substring(0, ind + 1);
                    return;
                }
            }
        }
    }

    /**
     * Extracs the folder from the root folder(if exists).
     *
     *
     * @param folderName folder name to be extracted from the root folder.
     * @return extracted folder name from the root folder.<br>
     *         E.g. folderName: <code>org/package/sources/</code>. if the root folder is <code>org</code>, the result
     *         will be <code>package/sources/</code>.
     */
    public String extractFromRootFolder(String folderName) {
        String result = null;
        if (rootName != null) {
            if (folderName != null && folderName.startsWith(rootName) && !folderName.equals(rootName)) {
                result = folderName.substring(rootName.length());
            }
            return result;
        }
        return folderName;
    }

    /**
     * Check if there is a single root folder in the set of folder names. Algorithm: if there is only one folder path
     * which contains first found symbol '/', and it`s number is equal to path length.
     *
     * @return true if there is a single root folder in the set of folder names
     */
    private boolean needToExtract() {
        String firstFolderName = null;

        for (String name : folderNames) {
            if (!name.contains("/")) {
                return false;
            }
            if (isValidFolderName(name)) {
                String secondFolderName = getFolderName(name);
                firstFolderName = StringUtils.isNotEmpty(firstFolderName) ? firstFolderName : secondFolderName;

                if (!secondFolderName.equals(firstFolderName)) {
                    return false;
                }
            }
        }

        return StringUtils.isNotEmpty(firstFolderName);
    }

    private boolean isValidFolderName(String name) {
        return filter == null || filter.accept(getFolderName(name));
    }

    private String getFolderName(String path) {
        int ind = path.indexOf('/');
        if (ind > -1) {
            return path.substring(0, ind);
        }

        return "";
    }

}
