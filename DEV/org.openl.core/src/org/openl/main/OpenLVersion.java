package org.openl.main;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For internal usage only.
 * 
 * @author Yury Molchan
 */
public class OpenLVersion {

    private static String url;
    private static String version;
    private static String buildDate;
    private static String buildNumber;

    static {
        Properties props = new Properties();

        InputStream propertiesFile = null;
        try {
            propertiesFile = OpenLVersion.class.getResourceAsStream("openl.version.properties");
            props.load(propertiesFile);
        } catch (Exception t) {
            LoggerFactory.getLogger(OpenLVersion.class).warn("openl.version.properties is not found", t);
        } finally {
            if (propertiesFile != null) {
                IOUtils.closeQuietly(propertiesFile);
            }
        }
        url = props.getProperty("openl.url", "??");
        version = props.getProperty("openl.version", "???");
        buildDate = props.getProperty("openl.build.date", "????-??-??");
        buildNumber = props.getProperty("openl.commit.hash", "????");
        logOpenLInfo();
    }

    public static String getURL() {
        return url;
    }

    public static String getVersion() {
        return version;
    }

    public static String getBuildDate() {
        return buildDate;
    }

    public static String getBuildNumber() {
        return buildNumber;
    }

    /**
     * Logs information for investigation purposes.
     */
    private static void logOpenLInfo() {
        Logger logger = LoggerFactory.getLogger("OpenL");

        logger.info("***** OpenL Tablets v{}  ({}, #{})", getVersion(), getBuildDate(), getBuildNumber());
        logger.info("***** Site : http:{}", getURL());

        logSystemInfo();
        logClasspath();
        logProperties();
        logEnvironment();
    }

    private static void logSystemInfo() {
        Logger logger = LoggerFactory.getLogger("OpenL.sys");
        try {
            logger.info("  Java : {} v{} ({})",
                System.getProperty("java.vendor"),
                System.getProperty("java.version"),
                System.getProperty("java.class.version"));
            logger.info("    OS : {} v{} ({})",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
            logger.info("  Time : {} ({} - {})",
                new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss.SSS XXX (z)").format(new Date()),
                TimeZone.getDefault().getID(),
                TimeZone.getDefault().getDisplayName());
            logger.info("Locale : {}", Locale.getDefault());
        } catch (Exception ignored) {
            logger.info("##### Cannot access to System properties");
        }
    }

    private static void logProperties() {
        Logger logger = LoggerFactory.getLogger("OpenL.prop");
        try {
            logger.info("System properties:");
            for (Map.Entry<?, ?> prop : System.getProperties().entrySet()) {
                logger.info("  {} = {}", prop.getKey(), prop.getValue());
            }
        } catch (Exception ignored) {
            logger.info("##### Cannot access to System properties");
        }
    }

    private static void logEnvironment() {
        Logger logger = LoggerFactory.getLogger("OpenL.env");
        try {
            logger.info("System environment:");
            for (Map.Entry<?, ?> prop : System.getenv().entrySet()) {
                logger.info("  {} = {}", prop.getKey(), prop.getValue());
            }
        } catch (Exception ignored) {
            logger.info("##### Cannot access to System properties");
        }
    }

    private static void logClasspath() {
        Logger logger = LoggerFactory.getLogger("OpenL.cp");
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = OpenLVersion.class.getClassLoader();
            }
            logger.info("Libs in the classpath:");
            while (classLoader != null) {
                logger.info(classLoader.getClass().getName());
                if (classLoader instanceof URLClassLoader) {
                    URL[] urls = ((URLClassLoader) classLoader).getURLs();
                    for (URL url : urls) {
                        logger.info("  {}", url);
                    }
                }
                classLoader = classLoader.getParent();
            }
        } catch (Exception ignored) {
            logger.info("##### Cannot list classpath");
        }
    }
}
