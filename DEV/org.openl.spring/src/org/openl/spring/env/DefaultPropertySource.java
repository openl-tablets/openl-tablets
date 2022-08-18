package org.openl.spring.env;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.ClassUtils;

/**
 * OpenL default property sources. Collects all openl-default.properties files.
 * <p>
 * Note: All openl-default.properties must contains unique keys.
 * 
 * @author Yury Molchan
 */
public class DefaultPropertySource extends EnumerablePropertySource<Map<String, String>> {

    public static final String PROPS_NAME = "OpenL default properties";

    static final String OPENL_CONFIG_LOADED = "openl.config.loaded";

    DefaultPropertySource() {
        super(PROPS_NAME, new HashMap<>());

        try {
            ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }
            Enumeration<URL> resources = classLoader.getResources("openl-default.properties");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream is = url.openStream()) {
                    PropertiesUtils.load(is, source::put);
                }
                ConfigLog.LOG.info("+       Load: '{}'", url);
            }
        } catch (Exception e) {
            ConfigLog.LOG.error("!     Error:", e);
        }
        source.put(OPENL_CONFIG_LOADED, Boolean.TRUE.toString());
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> propertyNames = source.keySet();
        return propertyNames.toArray(StringUtils.EMPTY_STRING_ARRAY);
    }

    @Override
    public Object getProperty(String name) {
        return source.get(name);
    }
}
