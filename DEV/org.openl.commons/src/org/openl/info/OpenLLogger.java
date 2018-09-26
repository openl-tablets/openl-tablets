package org.openl.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class OpenLLogger {
    private final Logger logger;
    {
        String name = getName();
        logger = LoggerFactory.getLogger("OpenL." + name);
    }

    abstract protected String getName();

    final public void log() {
        if (logger.isInfoEnabled()) {
            try {
                discover();
            } catch (Exception exc) {
                logger.info("##### {} ", exc.toString());
            }
        }
    }

    final protected void log(String text, Object... args) {
        logger.info(text, args);
    }

    abstract protected void discover() throws Exception;
}
