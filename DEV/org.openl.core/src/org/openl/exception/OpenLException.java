package org.openl.exception;

import org.openl.util.text.ILocation;

public interface OpenLException {

    String getMessage();

    Throwable getCause();

    /**
     * Gets error cause location.
     *
     * @return error cause location
     */
    ILocation getLocation();

    /**
     * Gets source code module where the error was occurred.
     *
     * @return source code module
     */
    String getSourceCode();

    /**
     * Gets error source location URI
     *
     * @return source location URI
     */
    String getSourceLocation();

}
