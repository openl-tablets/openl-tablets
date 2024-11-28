package org.openl.spring.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.info.OpenLInfoLogger;

final class ConfigLog {
    static final Logger LOG = LoggerFactory.getLogger("OpenL.config");

    static {
        OpenLInfoLogger.logInfo();
    }

    private ConfigLog() {
    }
}
