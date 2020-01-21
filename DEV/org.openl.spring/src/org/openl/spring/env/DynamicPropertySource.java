package org.openl.spring.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
        File file = getFile();
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

    private File getFile() {
        String property = resolver.getProperty(OPENL_HOME);
        return new File(property, appName + ".properties");
    }

    @Override
    public Object getProperty(String name) {
        if (OPENL_HOME.equals(name)) {
            // prevent cycled call
            return null;
        }

        String value = getProperties().getProperty(name);
            return decode(name, value);
    }


    static DynamicPropertySource THE;

    public static DynamicPropertySource get() {
        return THE;
    }
    public void save(Map<String, String> config) throws IOException {
        Properties properties = getProperties();
        for (Map.Entry<String, String> pair : config.entrySet()) {
            String propertyName = pair.getKey();
            String value = pair.getValue();
            if (value == null) {
                properties.remove(propertyName);
            } else {
                if (propertyName.endsWith("password")) {
                    try {
                        value = PassCoder.encode(value, getSecretKey());
                    } catch (Exception e) {
                        ConfigLog.LOG.error("Error when setting password property: {}", propertyName, e);
                        continue;
                    }
                }
                properties.setProperty(propertyName, value);
            }
        }
        File file = getFile();
        File parent = file.getParentFile();
        if (!parent.mkdirs() && !parent.exists()) {
            throw new FileNotFoundException("Can't create the folder " + parent.getAbsolutePath());
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            properties.store(writer, null);
        }
    }

    static String decode(String name, String value) {
        if (name.endsWith("password")) {
            try {
                return PassCoder.decode(value, DynamicPropertySource.get().getSecretKey());
            }
            catch (Exception e) {
                ConfigLog.LOG.error("Error when getting password property: {}", name, e);
                return "";
            }
        } else {
            return value;
        }
    }

    private String getSecretKey() {
        String passKey = resolver.getProperty("repository.encode.decode.key");
        return passKey != null ? StringUtils.trimToEmpty(passKey) : "";
    }
}
