package org.openl.info;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.LoggerFactory;

import org.openl.util.PropertiesUtils;

/**
 * For internal usage only.
 *
 * @author Yury Molchan
 */
public final class OpenLVersion {

    private OpenLVersion() {
        // Utility class
    }

    private static final String SITE_URL;
    private static final String VERSION;
    private static final String BUILD_DATE;
    private static final String BUILD_NUMBER;
    private static final Map<String, String> info;
    private static final Map<Object, Object> buildInfo;

    static {
        var props = new LinkedHashMap<String, String>();

        try {
            PropertiesUtils.load(OpenLVersion.class.getResource("openl.version.properties"), props::put);
        } catch (Exception t) {
            LoggerFactory.getLogger(OpenLVersion.class).warn("openl.version.properties is not found.", t);
        }
        SITE_URL = props.getOrDefault("openl.url", "??");
        VERSION = props.getOrDefault("openl.version", "???");
        var bd = props.getOrDefault("openl.build.date", "????-??-??");
        // If openl.version.properties is used from classpath and this property is not initialized at build time
        if ("${build.date}".equals(bd)) {
            bd = "????-??-??";
        }
        BUILD_DATE = bd;
        BUILD_NUMBER = props.getOrDefault("openl.commit.hash", "????");

        var source = HashMap.<String, String>newHashMap(6);
        source.put("openl.site", SITE_URL);
        source.put("openl.version", VERSION);
        source.put("openl.build.date", BUILD_DATE);
        source.put("openl.build.number", BUILD_NUMBER);
        source.put("openl.start.time", ZonedDateTime.now().toString());
        source.put("openl.start.milli", Long.toString(Instant.now().toEpochMilli()));
        source.put("openl.start.hash",
                new Random().ints(65, 91)
                        .limit(8)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString());
        info = Collections.unmodifiableMap(source);

        props.clear();

        Enumeration<URL> resources = null;
        try {
            resources = OpenLVersion.class.getClassLoader().getResources("build-info.properties");
        } catch (IOException e) {
            LoggerFactory.getLogger(OpenLVersion.class).warn("Failed to load 'buildInfo.properties' file.", e);
        }
        if (resources != null) {
            while (resources.hasMoreElements()) {
                var resource = resources.nextElement();
                try {
                    PropertiesUtils.load(resource, props::put);
                } catch (IOException t) {
                    LoggerFactory.getLogger(OpenLVersion.class).warn("Failed to load '{}' file.", resource, t);
                }
            }
        }
        props.putAll(info);
        buildInfo = Collections.unmodifiableMap(props);
    }

    public static String getUrl() {
        return SITE_URL;
    }

    public static String getVersion() {
        return VERSION;
    }

    public static String getBuildDate() {
        return BUILD_DATE;
    }

    public static String getBuildNumber() {
        return BUILD_NUMBER;
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
