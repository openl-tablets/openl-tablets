package org.openl.util.export;

/**
 * This exception is thrown on an error persisting <code>IExportable</code>
 * instances.
 */
public class ExportException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 7953655844659754008L;

    public ExportException() {
        super();
    }

    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExportException(Throwable cause) {
        super(cause);
    }
}
