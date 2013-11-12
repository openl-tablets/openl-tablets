package org.openl.rules.lang.xls.classes;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoggingExceptionHandler extends AbstractLocatorExceptionHandler {
    private final Log log = LogFactory.getLog(LoggingExceptionHandler.class);

    @Override
    public void handleURLParseException(Exception e) {
        if (log.isErrorEnabled()) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void handleIOException(IOException e) {
        if (log.isErrorEnabled()) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void handleClassInstatiateException(Throwable t) {
        if (log.isDebugEnabled()) {
            log.debug(t.getMessage(), t);
        }
    }

}
