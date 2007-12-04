package org.openl.rules.webstudio.services.upload;

/**
 * An interface to filter out unwanted files and directories when uploading a project.
 * As an example an application may choose to ignore CSV files (e.g <i>.svn</i> or <i>.cvs</i>) directories.
 */
public interface UploadFilter {
    /**
     * The filter method. Checks a filename in form of: <i>root_folder/sub_folder/.../[file_name]</i>. 
     * 
     * @param filename file or directory name
     * @return if filter accepts given filename
     */
    boolean accept(String filename);
}
