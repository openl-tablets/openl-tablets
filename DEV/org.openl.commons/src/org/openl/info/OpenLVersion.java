package org.openl.info;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.util.IOUtils;

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

    public static String getUrl() {
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
        logger.info("***** Site : http:{}", getUrl());

        logSystemInfo();
        logClasspath();
        logProperties();
        logEnvironment();
    }

    private static void logSystemInfo() {
        Logger logger = LoggerFactory.getLogger("OpenL.sys");
        try {
            logger.info("    Java : {} v{} ({})",
                System.getProperty("java.vendor"),
                System.getProperty("java.version"),
                System.getProperty("java.class.version"));
            logger.info("      OS : {} v{} ({})",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
        } catch (Exception ignored) {
            logger.info("##### Cannot access to System properties");
        }
        try {
            logger.info("    Time : {} ({} - {})",
                new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss.SSS XXX (z)").format(new Date()),
                TimeZone.getDefault().getID(),
                TimeZone.getDefault().getDisplayName());
            logger.info("  Locale : {}", Locale.getDefault());
        } catch (Exception ignored) {
            logger.info("##### Cannot access to the TimeZone or Locale");
        }
        try {
            logger.info("Work dir : {}", Paths.get("").toAbsolutePath());
        } catch (Exception ignored) {
            logger.info("##### Cannot access to the FileSystem");
        }
        try {
            logger.info("App path : {}",
                OpenLVersion.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        } catch (Exception ignored) {
            logger.info("##### Cannot access to the Application location");
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
                logger.info(getClassLoaderName(classLoader));
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

    private static String getClassLoaderName(ClassLoader classLoader) {
        Class<?> clazz = classLoader.getClass();
        String name = clazz.getName();
        try {
            Class cls = clazz.getMethod("toString").getDeclaringClass();
            if (!cls.equals(Object.class)) {
                name = classLoader.toString() + "  Class: " + name;
            }
        } catch (NoSuchMethodException e) {
            // Ignore
        }

        try {
            Method getName = clazz.getMethod("getName");
            Object getNameStr = getName.invoke(classLoader);
            if (getNameStr != null) {
                name += "  Name: " + getNameStr.toString();
            }
        } catch (Exception ex) {
            // Ignore
        }
        return name;
    }
}
