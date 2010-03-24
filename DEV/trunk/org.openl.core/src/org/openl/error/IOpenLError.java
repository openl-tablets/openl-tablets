package org.openl.error;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

/**
 * Defines OpenL error abstraction and provides information about occurred error.
 */
public interface IOpenLError {

    /**
     * Gets error message.
     * 
     * @return error message
     */
    String getMessage();

    /**
     * Gets original cause of error. It can be <code>null</code> if cause is not java exception or java error.
     * 
     * @return {@link Throwable} object if cause of error is java exception or java error; <code>null</code> - otherwise
     */
    Throwable getOriginalCause();

    /**
     * Gets source code module where the error was occurred.
     * 
     * @return source code module
     */
    IOpenSourceCodeModule getSourceModule();

    /**
     * Gets error cause location.
     * 
     * @return error cause location
     */
    ILocation getLocation();
}
