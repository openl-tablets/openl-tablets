package org.openl.info;

import java.io.InputStream;
import java.util.Properties;

import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For internal usage only.
 *
 * @author Yury Molchan
 */
public final class OpenLVersion {

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
            LoggerFactory.getLogger(OpenLVersion.class).warn("openl.version.properties is not found.", t);
        } finally {
            if (propertiesFile != null) {
                IOUtils.closeQuietly(propertiesFile);
            }
        }
        url = props.getProperty("openl.url", "??");
        version = props.getProperty("openl.version", "???");
        buildDate = props.getProperty("openl.build.date", "????-??-??");
        buildNumber = props.getProperty("openl.commit.hash", "????");
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

}
