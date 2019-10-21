package org.openl.rules.repository.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * The interface of the repository abstraction. It contains a minimal set of methods to be compatible with various of
 * internet storages like Amazon S3, Google Cloud Storage or Azure Storage. And to be simple to implement own storage
 * based on a Database or a File System. This repository does not assume that it should support transactions, sessions or
 * concurrent access. This repository must support only the atomicity of file modification, so the file always contains
 * valid data.
 *
 * All path names in the repository MUST BE relative and satisfy to the following rules:
 * <ol>
 * <li>Only '/' symbol MUST be used to separate folders</li>
 * <li>The first symbol of the path MUST NOT be started from '/'</li>
 * <li>The path to the folder MUST be ended with '/'</li>
 * <li>The path to the file MUST NOT be ended with '/'</li>
 * <li>The path to the root folder is the empty path</li>
 * </ol>
 *
 * Examples:
 * <ul>
 * <li>'' - the root folder</li>
 * <li>'file_name' - file</li>
 * <li>'folder_name/' - folder</li>
 * <li>'folder_name/inner_file' - file</li>
 * <li>'folder_name/inner_folder/' - folder</li>
 * </ul>
 *
 * @author Yury Molchan
 */
public interface Repository {

    /**
     * Return a list of files recursively in the given folder.
     *
     * @param path the folder to scan. The path must be ended by '/' or be empty.
     * @return the list of the file descriptors. Invalid files are ignored.
     * @throws IOException if not possible to read the directory.
     */
    List<FileData> list(String path) throws IOException;

    /**
     * Read a file descriptor by the given path name.
     *
     * @param name the path name of the file to read.
     * @return the file descriptor or null if the file is absent.
     * @throws IOException if not possible to read the file descriptor.
     */
    FileData check(String name) throws IOException;

    /**
     * Read a file by the given path name.
     *
     * @param name the path name of the file to read.
     * @return the file descriptor or null if the file is absent.
     * @throws IOException if not possible to read the file.
     */
    FileItem read(String name) throws IOException;

    /**
     * Save a file.
     *
     * @param data the file descriptor.
     * @param stream the stream to save with the specified file descriptor.
     * @return the resulted file descriptor after successful writing.
     * @throws IOException if not possible to save the file.
     */
    FileData save(FileData data, InputStream stream) throws IOException;

    /**
     * Save multiple projects in one transaction. Used only in deployment services
     *
     * @param fileItems list of file descriptor and stream pairs
     * @throws IOException if not possible to save the file.
     */
    List<FileData> save(List<FileItem> fileItems) throws IOException;

    /**
     * Delete a file or mark it as deleted.
     *
     * @param data the file descriptor to delete.
     * @return true if file has been deleted successfully or false if the file is absent or cannot be deleted.
     */
    boolean delete(FileData data);

    /**
     * Set a listener to monitor changes in the repository.
     *
     * @param callback the listener.
     */
    void setListener(Listener callback);

    /**
     * List a versions of the given file. If the repository does not support file versions, then it will return one
     * record of the given file. The order of the file descriptions is undefined, but the first element is the actual
     * file which can be access by {@link #read(String)} method.
     *
     * @param name the file name.
     * @return the list of file descriptions.
     * @throws IOException if not possible to read the directory.
     */
    List<FileData> listHistory(String name) throws IOException;

    /**
     * Read a file descriptor by the given path name of the given version. If the version is null, then it will work
     * like {@link #check(String)} method.
     *
     * @param name the path name of the file to read.
     * @param version the version of the file to read, can be null.
     * @return the file descriptor or null if the file is absent.
     * @throws IOException if not possible to read the file descriptor.
     * @see #read(String)
     */
    FileData checkHistory(String name, String version) throws IOException;

    /**
     * Read a file by the given path name of the given version. If the version is null, then it will work like
     * {@link #read(String)} method.
     *
     * @param name the path name of the file to read.
     * @param version the version of the file to read, can be null.
     * @return the file descriptor or null if the file is absent.
     * @throws IOException if not possible to read the file.
     * @see #read(String)
     */
    FileItem readHistory(String name, String version) throws IOException;

    /**
     * Delete a file from the history. If the version is null, then it will delete all versions of the file from the
     * history.
     *
     * @param data the file descriptor to delete. The fields "name", "version", "author" and "comment" must be
     *            initialized.
     * @return true if file has been deleted successfully or false if the file is absent or cannot be deleted.
     * @see #delete(FileData)
     */
    boolean deleteHistory(FileData data);

    /**
     * Copy a file of the given version to the destination file. If the version is null, then copy latest version.
     *
     * @param srcName the file to copy.
     * @param destData the destination file descriptor.
     * @param version the version of the file to copy
     * @return the file descriptor of the resulted file.
     * @throws IOException if not possible to copy the file
     */
    FileData copyHistory(String srcName, FileData destData, String version) throws IOException;

    /**
     * Get the features supported by the repository. If specific feature is supported, repository instance can be casted
     * to interface class that support that feature.
     *
     * @return Supported features
     */
    Features supports();
}
