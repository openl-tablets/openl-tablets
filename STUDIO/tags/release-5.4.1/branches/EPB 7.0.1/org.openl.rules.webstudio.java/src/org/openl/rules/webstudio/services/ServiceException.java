package org.openl.rules.webstudio.services;

/**
 * ServiceException
 *
 * @author Andrey Naumenko
 */
public class ServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new <code>ServiceException</code> with <code>null</code> as its
     * detail message.
     */
    public ServiceException() {
        super();
    }

    /**
     * Constructs a new <code>ServiceException</code> with the specified detail message.
     *
     * @param message the detail message string.
     */
    public ServiceException(final String message) {
        super(message);
    }

    /**
     * Constructs a new <code>ServiceException</code> with the specified cause and a
     * detail message of <code>(cause == null ? null : cause.toString())</code> (which
     * typically contains the class and detail message of cause).
     *
     * @param cause the causing throwable. A null value is permitted and indicates that
     *        the cause is nonexistent or unknown.
     */
    public ServiceException(final Throwable cause) {
        super((cause == null) ? (String) null : cause.toString());
        initCause(cause);
    }

    /**
     * Constructs a new <code>ServiceException</code> with the specified cause and
     * message.
     *
     * @param message the detail message string.
     * @param cause the causing throwable. A null value is permitted and indicates that
     *        the cause is nonexistent or unknown.
     */
    public ServiceException(final String message, final Throwable cause) {
        super(message);
        initCause(cause);
    }
}
