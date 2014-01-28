package org.openl.rules.project.resolving;

public class InvalidFileNamePatternException extends Exception {

    private static final long serialVersionUID = 5311460808662376815L;

    public InvalidFileNamePatternException() {
        super();
    }

    public InvalidFileNamePatternException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFileNamePatternException(String message) {
        super(message);
    }

    public InvalidFileNamePatternException(Throwable cause) {
        super(cause);
    }

}
