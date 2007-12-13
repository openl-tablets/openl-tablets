package org.openl.rules.webstudio.services.upload;

import org.openl.rules.webstudio.services.ServiceException;


/**
 * Thrown during uploading if provided file is corrupted zip archive.
 *
 * @author Andrey Naumenko
 */
public class NotUnzippedFileException extends ServiceException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new <code>NotUnzippedFileException</code> with <code>null</code> as
     * its detail message.
     */
    public NotUnzippedFileException() {
        super();
    }

    /**
     * Constructs a new <code>NotUnzippedFileException</code> with the specified detail
     * message.
     *
     * @param message the detail message string.
     */
    public NotUnzippedFileException(final String message) {
        super(message);
    }

    /**
     * Constructs a new <code>NotUnzippedFileException</code> with the specified cause.
     *
     * @param cause the causing throwable. A null value is permitted and indicates that
     *        the cause is nonexistent or unknown.
     */
    public NotUnzippedFileException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new <code>NotUnzippedFileException</code> with the specified cause
     * and message.
     *
     * @param message the detail message string.
     * @param cause the causing throwable. A null value is permitted and indicates that
     *        the cause is nonexistent or unknown.
     */
    public NotUnzippedFileException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
