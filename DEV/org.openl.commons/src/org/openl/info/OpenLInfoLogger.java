package org.openl.info;

import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.function.Consumer;

public final class OpenLInfoLogger {

    /**
     * Logs information for investigation purposes.
     */
    public static void logInfo() {
        Logger logger = LoggerFactory.getLogger("OpenL");

        logger.info("***** OpenL Tablets v{}  ({}, #{})", OpenLVersion.getVersion(), OpenLVersion.getBuildDate(), OpenLVersion.getBuildNumber());
        logger.info("***** Site : {}", OpenLVersion.getUrl());

        Level level = Level.FULL;
        try {
            level = Level.getLevel(System.getenv("OPENL_INFO"));
        } catch (Exception ignored) {
            logger.info("##### Cannot access to 'OPENL_INFO' environment property");
        }
        try {
            level = Level.getLevel(System.getProperty("openl.info", level.name()));
        } catch (Exception ignored) {
            logger.info("##### Cannot access to 'openl.info' system property");
        }

        level.runFor(Level.SYS, SysInfoLogger);
        if (level)
        new SysInfoLogger().log();

        new ClasspathLogger().log();

        logger.info("*********************************************************");
        logger.info("* The following logs may contain sensitive information! *");
        logger.info("* To disable output of this information define one of:  *");
        logger.info("*  -  OPENL_INFO=main       environment property        *");
        logger.info("*  -  -Dopenl.info=main     system property             *");
        logger.info("*  Note: If you can't see something,                    *");
        logger.info("*        it doesn't mean that others can't see it.      *");
        logger.info("*********************************************************");

        if (!"full".equals(level)) {
            return;
        }

        new SysPropLogger().log();
        new EnvPropLogger().log();
        new JndiLogger().log();
    }
}
//Just because you can't see something doesn't mean it doesn't exist.