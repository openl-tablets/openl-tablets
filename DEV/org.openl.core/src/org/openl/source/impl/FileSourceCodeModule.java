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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 *
 */
public class FileSourceCodeModule extends ASourceCodeModule {

    private File file;
    private String externalUri;
    private long lastModified;

    public FileSourceCodeModule(String fileName, String uri) {
        this(new File(fileName), uri);
    }

    public FileSourceCodeModule(File file, String uri) {
        this.file = file;
        lastModified = file.lastModified();
        this.externalUri = uri;
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
            return externalUri != null ? externalUri : file.getCanonicalFile().toURI().toURL().toExternalForm();
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap("", e);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        
        if (!(obj instanceof FileSourceCodeModule)) {
            return false;
        }
        
        FileSourceCodeModule fileSource = (FileSourceCodeModule) obj;

        return new EqualsBuilder()
            .append(file, fileSource.file)
            .append(uri, fileSource.uri)
            .isEquals();
    }

    @Override
    public int hashCode() {
        int hashCode = new HashCodeBuilder()
            .append(file)
            .append(uri)
            .toHashCode();
        
        return hashCode;
    }
    
    @Override
    public String toString() {    
        return file.toString();
    }

    public boolean isModified() {
        return file.lastModified() != lastModified;
    }

    @Override
    public void resetModified() {
        lastModified = file.lastModified();
    }

}
