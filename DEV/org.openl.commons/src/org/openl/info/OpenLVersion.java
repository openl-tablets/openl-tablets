package org.openl.info;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.openl.util.IOUtils;
import org.slf4j.LoggerFactory;

/**
 * For internal usage only.
 *
 * @author Yury Molchan
 */
public final class OpenLVersion {

    private static final String url;
    private static final String version;
    private static final String buildDate;
    private static final String buildNumber;
    public static Map<String, String> info;

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
        String email = props.getProperty("openl.email");
        version = props.getProperty("openl.version", "???");
        buildDate = props.getProperty("openl.build.date", "????-??-??");
        buildNumber = props.getProperty("openl.commit.hash", "????");

        HashMap<String, String> source = new HashMap<>(6);
        source.put("openl.site", url);
        source.put("openl.email", email);
        source.put("openl.version", version);
        source.put("openl.build.date", buildDate);
        source.put("openl.build.number", buildNumber);
        source.put("openl.start.time", ZonedDateTime.now().toString());
        source.put("openl.start.milli", Long.toString(Instant.now().toEpochMilli()));
        info = Collections.unmodifiableMap(source);
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

    public static Map<String, String> get() {
        return info;
    }
}
