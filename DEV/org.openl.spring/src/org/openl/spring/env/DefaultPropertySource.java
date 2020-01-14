package org.openl.spring.env;

import java.net.URL;
import java.util.Enumeration;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.ClassUtils;

/**
 * OpenL default property sources. Collects all openl-default.properties files.
 * 
 * Note: All openl-default.properties must contains unique keys.
 * 
 * @author Yury Molchan
 */
public class DefaultPropertySource extends CompositePropertySource {
    public static final String PROPS_NAME = "OpenL default properties";

    static final String OPENL_CONFIG_LOADED = "openl.config.loaded";

    DefaultPropertySource() {
        super(PROPS_NAME);
        try {
            ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }
            Enumeration<URL> resources = classLoader.getResources("openl-default.properties");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                addPropertySource(new ResourcePropertySource(new UrlResource(url)));
                ConfigLog.LOG.info("+        Add: '{}'", url);
            }
        } catch (Exception e) {
            ConfigLog.LOG.error("!     Error:", e);
        }
        addPropertySource(new PropertySource<Object>(OPENL_CONFIG_LOADED) {
            @Override
            public Object getProperty(String name) {
                return OPENL_CONFIG_LOADED.equals(name) ? true : null;
            }
        });
    }
}
