package org.openl.rules.common;

public class ProjectException extends CommonException {
    private static final long serialVersionUID = -2918146804954398129L;

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *            method.
     */
    public ProjectException(String message) {
        super(message);
    }

    public ProjectException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ProjectException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
