package org.openl.rules.calc;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class SpreadsheetCellsLoopException extends OpenlNotCheckedException {
    private static final long serialVersionUID = 1L;

    public SpreadsheetCellsLoopException() {
        super();
    }

    public SpreadsheetCellsLoopException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }

    public SpreadsheetCellsLoopException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpreadsheetCellsLoopException(String message) {
        super(message);
    }

    public SpreadsheetCellsLoopException(Throwable cause) {
        super(cause);
    }

}
