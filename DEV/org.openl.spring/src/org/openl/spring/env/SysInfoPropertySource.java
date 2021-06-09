package org.openl.spring.env;

import java.util.Map;

import org.openl.info.OpenLVersion;
import org.openl.util.StringUtils;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * Contain system information about OpenL and environment. Allows to monitor resources
 */
public class SysInfoPropertySource extends EnumerablePropertySource<Map<String, String>> {
    public static final String PROPS_NAME = "OpenL Info";

    SysInfoPropertySource() {
        super(PROPS_NAME, OpenLVersion.get());
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
