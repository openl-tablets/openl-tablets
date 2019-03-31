package org.openl.rules.repository;

import java.io.InputStream;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * OpenL Rules File. It stores content of physical files.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RFile extends REntity {

    /**
     * Gets content of the file. It is highly apreciated to close stream right after it is no longer needed.
     *
     * @return content stream with content of file
     * @throws RRepositoryException if failed
     */
    InputStream getContent() throws RRepositoryException;

    /**
     * Returns content of specified version of the file.
     *
     * @param version specified version
     * @return content of specified version
     * @throws RRepositoryException if failed
     */
    InputStream getContent4Version(CommonVersion version) throws RRepositoryException;

    /**
     * Gets mime type of the file.
     *
     * @return mime type
     */
    String getMimeType();

    /**
     * Returns size of the file's content in bytes.
     *
     * @return size of content or <code>-1</code> if cannot determine it.
     */
    long getSize();

    /**
     * Reverts the file to specified version.
     *
     * @param versionName name of version
     * @throws RRepositoryException if failed
     */
    void revertToVersion(String versionName) throws RRepositoryException;

    /**
     * Sets/Updates content of the file. At the end input stream will be closed.
     *
     * @param inputStream stream with new content of the file
     * @throws RModifyException if failed
     */
    void setContent(InputStream inputStream) throws RRepositoryException;

}
