package org.openl.info;

import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenLInfoLogger {
    /**
     * Logs information for investigation purposes.
     */
    public static void logInfo() {
        Logger logger = LoggerFactory.getLogger("OpenL");

        logger.info("***** OpenL Tablets v{}  ({}, #{})", OpenLVersion.getVersion(), OpenLVersion.getBuildDate(), OpenLVersion.getBuildNumber());
        logger.info("***** Site : {}", OpenLVersion.getUrl());

        String level = null;
        try {
            level = System.getenv("OPENL_INFO");
        } catch (Exception ignored) {
            logger.info("##### Cannot access to 'OPENL_INFO' environment property");
        }
        try {
            level = System.getProperty("openl.info", level);
        } catch (Exception ignored) {
            logger.info("##### Cannot access to 'openl.info' system property");
        }
        level = StringUtils.isBlank(level) ? "full" : level;

        new SysInfoLogger().log();

        if (!"full".equals(level)) {
            return;
        }

        new ClasspathLogger().log();
        new SysPropLogger().log();
        new EnvPropLogger().log();
        new JndiLogger().log();
    }
}
