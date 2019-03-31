package org.openl.rules.project.resolving;

public class NoMatchFileNameException extends Exception {

    private static final long serialVersionUID = 1L;

    public NoMatchFileNameException() {
        super();
    }

    public NoMatchFileNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoMatchFileNameException(String message) {
        super(message);
    }

    public NoMatchFileNameException(Throwable cause) {
        super(cause);
    }

}
