package org.openl.rules.project.resolving;

public class NoMatchFileNameException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * A warning message to the platform, when a file name is not matched by {@link PropertiesFileNameProcessor}
     * 
     * @param message - human readable message to understand what happened and where.
     */
    public NoMatchFileNameException(String message) {
        super(message);
    }
}
