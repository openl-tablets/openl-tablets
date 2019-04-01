package org.openl.rules.lang.xls.classes;

import java.io.IOException;

/**
 * Abstract stub for exception handlers
 *
 * @author NSamatov
 */
public abstract class AbstractLocatorExceptionHandler implements LocatorExceptionHandler {

    @Override
    public void handleURLParseException(Exception e) {
        // do nothing
    }

    @Override
    public void handleClassInstatiateException(Throwable t) {
        // do nothing
    }

    @Override
    public void handleIOException(IOException e) {
        // do nothing
    }

}
