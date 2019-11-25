package org.openl.rules.binding;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class RecursiveSpreadsheetMethodPreBindingException extends OpenlNotCheckedException {

    private static final long serialVersionUID = 1L;

    public RecursiveSpreadsheetMethodPreBindingException() {
        super();
    }

    public RecursiveSpreadsheetMethodPreBindingException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }

    public RecursiveSpreadsheetMethodPreBindingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecursiveSpreadsheetMethodPreBindingException(String message) {
        super(message);
    }

    public RecursiveSpreadsheetMethodPreBindingException(Throwable cause) {
        super(cause);
    }

}
