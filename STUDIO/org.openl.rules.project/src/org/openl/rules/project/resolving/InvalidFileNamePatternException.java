package org.openl.rules.project.resolving;

public class InvalidFileNamePatternException extends Exception {

    private static final long serialVersionUID = 5311460808662376815L;

    /**
     * An error message to the platform, when a pattern is not parsed by {@link PropertiesFileNameProcessor}
     * 
     * @param message - human readable message to understand what happened and where.
     */
    public InvalidFileNamePatternException(String message) {
        super(message);
    }

}
