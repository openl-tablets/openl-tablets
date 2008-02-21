package org.openl.rules.webstudio.services.upload;

import org.openl.rules.webstudio.services.ServiceException;


/**
 * Thrown when size of uploaded file is 0.
 *
 * @author Andrey Naumenko
 */
public class EmptyFileException extends ServiceException {
    private static final long serialVersionUID = 1L;
    private String detailCode;

    /**
     * Constructs a new <code>EmptyFileException</code> with <code>null</code> as its
     * detail message.
     */
    public EmptyFileException() {
        super();
    }

    /**
     * Constructs a new <code>EmptyFileException</code> with the specified detail message
     * and specified detail code.
     *
     * @param message the detail message string.
     * @param detailCode detail code
     */
    public EmptyFileException(final String message, String detailCode) {
        super(message);
        this.detailCode = detailCode;
    }

    /**
     * Constructs a new <code>EmptyFileException</code> with the specified.
     *
     * @param cause the causing throwable. A null value is permitted and indicates that
     *        the cause is nonexistent or unknown.
     */
    public EmptyFileException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new <code>EmptyFileException</code> with the specified cause and
     * message.
     *
     * @param message the detail message string.
     * @param cause the causing throwable. A null value is permitted and indicates that
     *        the cause is nonexistent or unknown.
     */
    public EmptyFileException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Detail code.
     *
     * @return detail code
     */
    public String getDetailCode() {
        return detailCode;
    }

    public void setDetailCode(String detailCode) {
        this.detailCode = detailCode;
    }
}
