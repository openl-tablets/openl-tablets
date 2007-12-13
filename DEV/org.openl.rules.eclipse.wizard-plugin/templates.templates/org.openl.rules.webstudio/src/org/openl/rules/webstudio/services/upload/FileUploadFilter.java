package org.openl.rules.webstudio.services.upload;

import java.util.regex.Pattern;

public class FileUploadFilter implements UploadFilter {
    private final Pattern pattern;

    public FileUploadFilter(String filename) {
        pattern = Pattern.compile("(.*/)?" + filename);
    }

    /**
     * The filter method. Checks a filename in form of: <i>root_folder/sub_folder/.../[file_name]</i>.
     *
     * @param filename file or directory name
     * @return if filter accepts given filename
     */
    public boolean accept(String filename) {
        return !pattern.matcher(filename).matches();
    }
}
