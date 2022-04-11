package org.openl.info;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

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
    private static final Map<String, String> info;
    private static final Map<Object, Object> buildInfo;

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
        String bd = props.getProperty("openl.build.date", "????-??-??");
        // If openl.version.properties is used from classpath and this property is not initialized at build time
        if ("${build.date}".equals(bd)) {
            bd = "????-??-??";
        }
        buildDate = bd;
        buildNumber = props.getProperty("openl.commit.hash", "????");

        HashMap<String, String> source = new HashMap<>(6);
        source.put("openl.site", url);
        source.put("openl.email", email);
        source.put("openl.version", version);
        source.put("openl.build.date", buildDate);
        source.put("openl.build.number", buildNumber);
        source.put("openl.start.time", ZonedDateTime.now().toString());
        source.put("openl.start.milli", Long.toString(Instant.now().toEpochMilli()));
        source.put("openl.start.hash",
            new Random().ints(65, 91)
                .limit(8)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString());
        info = Collections.unmodifiableMap(source);

        props = new Properties();

        Enumeration<URL> resources = null;
        try {
            resources = OpenLVersion.class.getClassLoader().getResources("build-info.properties");
        } catch (IOException e) {
            LoggerFactory.getLogger(OpenLVersion.class).warn("Failed to load 'buildInfo.properties' file.", e);
        }
        if (resources != null) {
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (InputStream propsStream = resource.openStream()) {
                    props.load(propsStream);
                } catch (IOException t) {
                    LoggerFactory.getLogger(OpenLVersion.class).warn("Failed to load '{}' file.", resource, t);
                }
            }
        }
        props.putAll(info);
        buildInfo = Collections.unmodifiableMap(props);
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

    /**
     * Custom build information from {@code build-info.properties} files and default OpenL information
     */
    public static Map<Object, Object> getBuildInfo() {
        return buildInfo;
    }
}
