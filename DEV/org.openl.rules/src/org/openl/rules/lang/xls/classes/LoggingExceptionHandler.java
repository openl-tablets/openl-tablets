package org.openl.rules.lang.xls.classes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoggingExceptionHandler extends AbstractLocatorExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(LoggingExceptionHandler.class);

    @Override
    public void handleURLParseException(Exception e) {
        log.error(e.getMessage(), e);
    }

    @Override
    public void handleIOException(IOException e) {
        log.error(e.getMessage(), e);
    }

    @Override
    public void handleClassInstatiateException(Throwable t) {
        log.debug(t.getMessage(), t);
    }
}
