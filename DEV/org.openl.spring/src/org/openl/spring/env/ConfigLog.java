package org.openl.spring.env;

import org.openl.info.OpenLInfoLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigLog {
    static final Logger LOG = LoggerFactory.getLogger("OpenL.config");
    static {
        OpenLInfoLogger.logInfo();
    }
}
