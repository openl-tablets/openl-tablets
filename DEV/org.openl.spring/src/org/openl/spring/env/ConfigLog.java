package org.openl.spring.env;

import org.openl.info.OpenLVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigLog {
    static final Logger LOG = LoggerFactory.getLogger("OpenL.config");
    private static final String VERSION = OpenLVersion.getVersion(); // Just for init OpenLVersion class.
}
