package org.openl.rules.webstudio.services.upload;

import java.util.regex.Pattern;

public class FolderUploadFilter implements UploadFilter {
    /**
     * A filter that filters out version control utility files for following version control systems:
     * <ul>
     * <li>SVN</li>
     * <li>CVS</li>
     * </ul>
     */
    public static final UploadFilter VCS_FILES_FILTER = new CompoundUploadFilter(new FolderUploadFilter(".svn"),
            new FolderUploadFilter("CVS"), new FileUploadFilter(".cvsignore"));

    private final Pattern pattern;

    public FolderUploadFilter(String folderName) {
        pattern = Pattern.compile("(.*/)?" + folderName + "/.*");
    }

    /**
     * The filter method. Checks a filename in form of: <i>root_folder/sub_folder/.../[file_name]</i>
     *
     * @param filename file or directory name
     * @return if filter accepts given filename
     */
    public boolean accept(String filename) {
        return !pattern.matcher(filename).matches();
    }
}
