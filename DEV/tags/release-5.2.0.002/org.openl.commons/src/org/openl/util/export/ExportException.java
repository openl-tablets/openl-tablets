package org.openl.util.export;

/**
 *  This exception is thrown on an error persisting <code>IExportable</code> instances.    
 */
public class ExportException extends Exception {
    public ExportException() {
        super();
    }

    public ExportException(Throwable cause) {
        super(cause);
    }

    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
