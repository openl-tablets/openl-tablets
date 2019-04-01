package org.openl.rules.lang.xls.classes;

import java.io.IOException;

/**
 * Classes that implement this interface will handle exceptions thrown while locating classes Use cases: logging, adding
 * error messages to WebStudio, rethrowing unchecked exception etc.
 *
 * @author NSamatov
 *
 */
public interface LocatorExceptionHandler {

    /**
     * Handle exceptions thrown when resource URL is parsed
     *
     * @param e exception
     */
    void handleURLParseException(Exception e);

    /**
     * Handle exceptions thrown when class is instantiated
     *
     * @param t exception. Note: sometimes ClassLoader can throw a subclass of Error while a class instantiating.
     */
    void handleClassInstatiateException(Throwable t);

    /**
     * Handle exceptions thrown when Input/Output operations is performed
     *
     * @param e exception
     */
    void handleIOException(IOException e);
}
