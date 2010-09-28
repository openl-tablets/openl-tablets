/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.source.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 *
 */
public class FileSourceCodeModule extends ASourceCodeModule {

    private File file;
    private String externalUri;

    public FileSourceCodeModule(String fileName, String uri) {
        this(new File(fileName), uri);
    }

    public FileSourceCodeModule(File file, String uri) {
        this.file = file;
        this.externalUri = uri;
    }

    public FileSourceCodeModule(String fileName, String uri, int tabSize) {
        this(new File(fileName), uri);
        this.tabSize = tabSize;
    }
    
    public File getFile() {
        return file;
    }
    
    public InputStream getByteStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw RuntimeExceptionWrapper.wrap("", e);
        }
    }

    public Reader getCharacterStream() {
        try {
            return new FileReader(file);
        } catch (FileNotFoundException e) {
            throw RuntimeExceptionWrapper.wrap("", e);
        }
    }

    /**
     * Produces source code module relative to this source code module
     *
     * @param relativePath
     * @return
     */
    public FileSourceCodeModule getRelativeSourceCodeModule(String relativePath) {

        try {
            String dir = file.getParentFile().getCanonicalPath();

            File newFile = new File(dir + "/" + relativePath).getCanonicalFile();

            return new FileSourceCodeModule(newFile, null);
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    @Override
    protected String makeUri() {
        try {
            return externalUri != null ? externalUri : file.getCanonicalFile().toURL().toExternalForm();
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap("", e);
        }
    }

}
