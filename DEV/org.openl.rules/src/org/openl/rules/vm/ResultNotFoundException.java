package org.openl.rules.vm;

public class ResultNotFoundException extends Exception {

    private static final long serialVersionUID = 2767662067900098124L;

    public ResultNotFoundException() {
        super();
    }

    public ResultNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultNotFoundException(String message) {
        super(message);
    }

    public ResultNotFoundException(Throwable cause) {
        super(cause);
    }

}
