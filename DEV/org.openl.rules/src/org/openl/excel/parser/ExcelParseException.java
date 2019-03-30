package org.openl.excel.parser;

public class ExcelParseException extends RuntimeException {
    private static final long serialVersionUID = -3043843512554237562L;

    public ExcelParseException() {
    }

    public ExcelParseException(String message) {
        super(message);
    }

    public ExcelParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExcelParseException(Throwable cause) {
        super(cause);
    }

    public ExcelParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
