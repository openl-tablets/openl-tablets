package org.openl.exception;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public interface OpenLException {

    /**
     * Gets original error message.
     * 
     * @return error message
     */
    String getOriginalMessage();

    /**
     * Gets original cause of error.
     * It can be <code>null</code> if cause is not java exception or java error.
     * 
     * @return {@link Throwable} object if cause of error is java exception or
     *         java error; <code>null</code> - otherwise
     */
    Throwable getOriginalCause();

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
    IOpenSourceCodeModule getSourceModule();

}
