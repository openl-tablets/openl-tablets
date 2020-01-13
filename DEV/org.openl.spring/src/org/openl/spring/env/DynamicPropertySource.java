package org.openl.spring.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.openl.util.StringUtils;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertyResolver;

/**
 * Loads always actual properties from an external file located in ${openl.home} directory.
 *
 * @author Yury Molchan
 */
public class DynamicPropertySource extends EnumerablePropertySource<Object> {
    public static final String PROPS_NAME = "Dynamic properties";

    public static final String OPENL_HOME = "openl.home";

    private final PropertyResolver resolver;
    private final String appName;

    public DynamicPropertySource(String appName, PropertyResolver resolver) {
        super(PROPS_NAME);
        this.resolver = resolver;
        this.appName = appName;
    }

    @Override
    public String[] getPropertyNames() {
        Properties properties = getProperties();

        return properties.keySet().toArray(StringUtils.EMPTY_STRING_ARRAY);
    }

    private Properties getProperties() {
        String property = resolver.getProperty(OPENL_HOME);
        File file = new File(property, appName + ".properties");
        Properties properties = new Properties();
        if (file.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                ConfigLog.LOG.error("Failed to load", e);
            }
        }
        return properties;
    }

    @Override
    public Object getProperty(String name) {
        if (OPENL_HOME.equals(name)) {
            // prevent cycled call
            return null;
        }
        return getProperties().getProperty(name);
    }
}
