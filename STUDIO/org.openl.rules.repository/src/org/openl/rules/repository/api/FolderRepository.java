package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.List;

/**
 * Implementations of this repository can work with folders and their versions. Typically folder's version is version of
 * the latest changed file inside that folder.
 */
public interface FolderRepository extends Repository {
    /**
     * Return a list of folders in the given path (not recursively).
     *
     * @param path the folder to scan. The path must be ended by '/' or be empty.
     * @return the list of the folder descriptors. Invalid folders are ignored.
     * @throws IOException if not possible to read the directory.
     */
    List<FileData> listFolders(String path) throws IOException;

    /**
     * Return a list of files recursively in the given folder and given version.
     *
     * @param path the folder to scan. The path must be ended by '/' or be empty.
     * @param version the version of the folder to read, can be null.
     * @return the list of the file descriptors. Invalid files are ignored.
     * @throws IOException if not possible to read the directory.
     */
    List<FileData> listFiles(String path, String version) throws IOException;

    /**
     * Save the folder.
     *
     * @param folderData    folder descriptor
     * @param files         all files inside the folder recursively or only changed (modified, added, deleted) files depending on changesetType
     * @param changesetType if {@link ChangesetType#DIFF}, only changed files. If {@link ChangesetType#FULL} all files that exist in project
     * @return the resulted folder descriptor after successful writing.
     * @throws IOException if not possible to save the folder.
     */
    FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) throws IOException;

    /**
     *  Save multiple files at once
     *  Used only in deployment services
     *
     * @param folderItems list of folder descriptor and its files
     * @param changesetType if {@link ChangesetType#DIFF}, only changed files. If {@link ChangesetType#FULL} all files that exist in project
     * @return the resulted folder descriptor after successful writing.
     * @throws IOException if not possible to save the folder.
     */
    List<FileData> save(List<FolderItem> folderItems, ChangesetType changesetType) throws IOException;
}
