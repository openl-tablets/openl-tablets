package org.openl.rules.calc;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class UnexpectedSpreadsheetResultFieldTypeException extends OpenlNotCheckedException {

    private static final long serialVersionUID = -4951717309605402969L;

    public UnexpectedSpreadsheetResultFieldTypeException() {
        super();
    }

    public UnexpectedSpreadsheetResultFieldTypeException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }

    public UnexpectedSpreadsheetResultFieldTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedSpreadsheetResultFieldTypeException(String message) {
        super(message);
    }

    public UnexpectedSpreadsheetResultFieldTypeException(Throwable cause) {
        super(cause);
    }

}
