package org.openl.spring.env;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.info.OpenLVersion;
import org.openl.util.StringUtils;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * Contain system information about OpenL and environment. Allows to monitor resources
 */
public class SysInfoPropertySource extends EnumerablePropertySource<Map<String, String>> {
    public static final String PROPS_NAME = "OpenL Info";

    SysInfoPropertySource() {
        super(PROPS_NAME, new ConcurrentHashMap<>());
        source.put("openl.site", OpenLVersion.getUrl());
        source.put("openl.version", OpenLVersion.getVersion());
        source.put("openl.build.date", OpenLVersion.getBuildDate());
        source.put("openl.build.number", OpenLVersion.getBuildNumber());
        source.put("openl.start.time", ZonedDateTime.now().toString());
        source.put("openl.start.milli", Long.toString(Instant.now().toEpochMilli()));
    }

    @Override
    public String[] getPropertyNames() {
        return source.keySet().toArray(StringUtils.EMPTY_STRING_ARRAY);
    }

    @Override
    public Object getProperty(String name) {
        return source.get(name);
    }
}
