package org.openl.rules.maven.decompiler;

import org.apache.maven.plugin.logging.Log;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;

class FernflowerLoggerAdapter extends IFernflowerLogger {

    private final Log log;

    public FernflowerLoggerAdapter(Log log) {
        this.log = log;
    }

    Log getLog() {
        return log;
    }

    @Override
    public void writeMessage(String message, Severity severity) {
        if (this.accepts(severity)) {
            switch (severity) {
                case TRACE:
                    log.debug(message);
                    break;
                case WARN:
                    log.warn(message);
                    break;
                case INFO:
                    log.info(message);
                    break;
                case ERROR:
                    log.error(message);
                    break;
            }
        }

    }

    @Override
    public void writeMessage(String message, Severity severity, Throwable ex) {
        if (this.accepts(severity)) {
            switch (severity) {
                case TRACE:
                    log.debug(message, ex);
                    break;
                case WARN:
                    log.warn(message, ex);
                    break;
                case INFO:
                    log.info(message, ex);
                    break;
                case ERROR:
                    log.error(message, ex);
                    break;
            }
        }
    }
}
