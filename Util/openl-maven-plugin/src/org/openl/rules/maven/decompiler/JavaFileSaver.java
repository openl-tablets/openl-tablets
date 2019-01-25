package org.openl.rules.maven.decompiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.jar.Manifest;

import org.apache.maven.plugin.logging.Log;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.openl.util.IOUtils;
import org.openl.util.RuntimeExceptionWrapper;

class JavaFileSaver implements IResultSaver {

    private final File root;
    private final Log log;
    private boolean fileSaved = false;

    public JavaFileSaver(File root, Log log) {
        this.root = root;
        this.log = log;
    }

    public boolean isFileSaved() {
        return fileSaved;
    }

    private String getAbsolutePath(String path) {
        return new File(root, path).getAbsolutePath();
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        File file = new File(getAbsolutePath(qualifiedName + ".java"));
        FileWriter fw = null;
        try {
            if (file.exists()) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("File '%s' exists already. It has been overwritten.", file));
                }
            }
            File folder = file.getParentFile();
            if (folder != null && !folder.mkdirs() && !folder.isDirectory()) {
                throw new IOException("Failed to create folder " + folder.getAbsolutePath());
            }
            fw = new FileWriter(file);
            fw.write(content);
            fileSaved = true;
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        } finally {
            IOUtils.closeQuietly(fw);
        }
    }

    @Override
    public void saveFolder(String path) {
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {
    }

    @Override
    public void saveDirEntry(String path, String archiveName, String entryName) {
    }

    @Override
    public void copyEntry(String source, String path, String archiveName, String entryName) {
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
    }

    @Override
    public void closeArchive(String entryName, String file) {
    }
}
