package org.openl.rules.repository.lwspace.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openl.rules.repository.lwspace.LocalWorkspace;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Implementation of Local Workspace.
 * 
 * @author Aleh Bykhavets
 *
 */
public class LocalWorkspaceImpl implements LocalWorkspace {
    private static final int BUFFER_SIZE = 1024 * 16;

    /** Temporary folder */
    private File tempFolder;
    /** OpenL Project in JCR */
    private RProject project;

    /** {@inheritDoc} */
    public void initialize(RProject project, File tempLocation) throws RRepositoryException {
        if (!tempLocation.exists()) {
            // there is no temporary folder -- create it
            if (!tempLocation.mkdir()) {
                throw new RuntimeException("Failed to create temporary folder");
            }
        } else {
            // check tempLocation, it must be a folder
            if (!tempLocation.isDirectory()) {
                throw new RuntimeException("Temporary location is not a folder");
            }
        }

        tempFolder = tempLocation;
        this.project = project;

        // clean up... just in case
        cleanFolder(tempLocation);

        // downloads project files from JCR
        downloadProject();
    }

    /** {@inheritDoc} */
    public void clean() {
        cleanFolder(tempFolder);

        // GC clean up
        project = null;
        tempFolder = null;
    }

    /** {@inheritDoc} */
    public void revert() throws RRepositoryException {
        cleanFolder(tempFolder);

        downloadProject();
    }

    /** {@inheritDoc} */
    public void commit() throws RRepositoryException  {
        //TODO: implement commit
        // 1. delete from JCR that is absent in Local Workspace
        // 2. add new folders
        // 3. compare/upload updated/new files
//		uploadProject();
    }

    // ------ private methods ------

    /**
     * Clean folder recursively.
     * It deletes all files, sub folders and the folder itself.
     *
     * @param folder root folder
     */
    private void cleanFolder(File folder) {
        File[] files = folder.listFiles();

        for (File f : files) {
            if (f.isDirectory()) {
                // delete recursively
                cleanFolder(f);
            } else {
                // delete file
                f.delete();
            }
        }

        // delete folder itself
        folder.delete();
    }

    private void downloadProject() throws RRepositoryException {
        RFolder root = project.getRootFolder();

        try {
            downloadJcrFolder(root, tempFolder);
        } catch (IOException e) {
            throw new RRepositoryException("Failed to download project from JCR: " + e.getMessage(), e);
        }
    }

    private void downloadJcrFolder(RFolder jcrFolder, File tempFolder) throws IOException, RRepositoryException {
        List<RFolder> subFolders = jcrFolder.getFolders();

        for (RFolder subFolder : subFolders) {
            // create temporary sub folder
            String name = subFolder.getName();
            File subTemp = new File(tempFolder, name);
            subTemp.mkdir();
            // process recursively
            downloadJcrFolder(subFolder, subTemp);
        }

        List<RFile> files = jcrFolder.getFiles();
        for (RFile file : files) {
            String name = file.getName();

            File f = new File(tempFolder, name);
            writeFile(f, file);
        }
    }

    /**
     * Writes file in temporary location from JCR.
     *
     * @param f new file
     * @param file repository file
     */
    private void writeFile(File f, RFile file) throws IOException, RRepositoryException {
        InputStream is = file.getContent();
        FileOutputStream fos = new FileOutputStream(f);

        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            // transfer data
            while (true) {
                int readed = is.read(buffer);
                if (readed <= 0)
                    break; // nothing to write

                fos.write(buffer, 0, readed);
            }
        } finally {
            fos.close();
            is.close();
        }
    }
}
