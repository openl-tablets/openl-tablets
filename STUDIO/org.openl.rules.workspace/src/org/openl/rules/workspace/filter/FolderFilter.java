package org.openl.rules.workspace.filter;

import java.util.regex.Pattern;


public class FolderFilter implements PathFilter {
    private final Pattern pattern;

    public FolderFilter(String folderName) {
        pattern = Pattern.compile("(.*/)?" + folderName + "/.*");
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
        return !pattern.matcher(filename).matches();
    }
}
