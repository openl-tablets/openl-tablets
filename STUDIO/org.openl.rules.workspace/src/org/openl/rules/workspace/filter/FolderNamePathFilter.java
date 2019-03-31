package org.openl.rules.workspace.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Filters path names for certain folder names.
 *
 * @author Aliaksandr Antonik
 * @author Andrey Naumenko
 */
public class FolderNamePathFilter implements PathFilter {
    private final Collection<Pattern> patterns;

    public FolderNamePathFilter(Collection<String> folderNames) {
        patterns = new ArrayList<>();
        for (String folderName : folderNames) {
            patterns.add(Pattern.compile("(.*/)?" + folderName + "/.*"));
        }
    }

    /**
     * The filter method. Checks a filename in form of:
     * <i>root_folder/sub_folder/.../[file_name]</i>
     *
     * @param filename file or directory name
     *
     * @return if filter accepts given filename
     */
    public boolean accept(String filename) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(filename).matches()) {
                return false;
            }
        }
        return true;
    }
}
