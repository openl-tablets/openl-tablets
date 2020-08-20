package org.openl.spring.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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
    public static final String OPENL_HOME_SHARED = "openl.home.shared";

    private final PropertyResolver resolver;
    private final String appName;

    private Properties settings;
    private long timestamp;

    public DynamicPropertySource(String appName, PropertyResolver resolver) {
        super(PROPS_NAME);
        this.resolver = resolver;
        this.appName = appName;
        loadProperties();
        ConfigLog.LOG.info("+        Add: '{}'", getFile());
    }

    @Override
    public String[] getPropertyNames() {
        return settings.keySet().toArray(StringUtils.EMPTY_STRING_ARRAY);
    }

    @Override
    public boolean containsProperty(String name) {
        return settings.containsKey(name);
    }

    public boolean reloadIfModified() {
        long l = getFile().lastModified();
        boolean modified = l != timestamp;
        if (modified) {
            loadProperties();
        }
        return modified;
    }

    private synchronized void loadProperties() {
        File file = getFile();
        Properties properties = new Properties();
        long lastModified = file.lastModified();
        if (file.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                ConfigLog.LOG.error("Failed to load", e);
            }
        }
        settings = properties;
        timestamp = lastModified;
    }

    private File getFile() {
        String property = resolver.getProperty(OPENL_HOME_SHARED);
        File file = new File(property, appName + ".properties");
        return file;
    }

    @Override
    public Object getProperty(String name) {
        if (OPENL_HOME.equals(name) || OPENL_HOME_SHARED.equals(name)) {
            // prevent cycled call
            return null;
        }
        String property = settings.getProperty(name);
        if (property == null) {
            return null;
        }
        property = StringUtils.trimToEmpty(property);
        return decode(property);
    }

    static DynamicPropertySource THE;

    public static DynamicPropertySource get() {
        return THE;
    }

    public void setOpenLHomeDir(String workingDir) {
        Preferences node = PreferencePropertySource.THE.getSource();
        node.put(DynamicPropertySource.OPENL_HOME, workingDir);
        try {
            // guard against loss in case of abnormal termination of the VM
            // in case of normal VM termination, the flush method is not required
            node.flush();
        } catch (BackingStoreException e) {
            ConfigLog.LOG.error("Cannot save preferences value", e);
        }
        loadProperties();
    }

    public synchronized void save(Map<String, String> config) throws IOException {
        Properties properties = new Properties();
        properties.putAll(settings);
        for (Map.Entry<String, String> pair : config.entrySet()) {
            String propertyName = pair.getKey();
            String value = pair.getValue();
            if (value == null) {
                properties.remove(propertyName);
            } else {
                if (propertyName.endsWith("password")) {
                    try {
                        String secretKey = getSecretKey();
                        String cipher = getCipher();
                        if (StringUtils.isNotBlank(value) && StringUtils.isNotBlank(secretKey) && StringUtils
                            .isNotBlank(cipher)) {
                            value = "ENC(" + PassCoder.encode(value, secretKey, cipher) + ")";
                        }
                    } catch (Exception e) {
                        ConfigLog.LOG.error("Error when setting password property: {}", propertyName, e);
                        continue;
                    }
                }
                properties.setProperty(propertyName, value);
            }
        }
        Properties origin = settings;
        settings = new Properties(); // 'unconfigure' settings for matching with defaults. to get settings not from a file

        Iterator<Map.Entry<Object, Object>> props = properties.entrySet().iterator();
        while (props.hasNext()) { // Do clean up from default values
            Map.Entry<Object, Object> e = props.next();
            String key = e.getKey().toString();
            if (Objects.equals(resolver.getProperty(key), e.getValue())) {
                props.remove();
            }
        }

        settings = properties;
        if (!origin.equals(properties)) {
            // Save the difference only
            File settingsFile = getFile();
            File parent = settingsFile.getParentFile();
            if (!parent.mkdirs() && !parent.exists()) {
                throw new FileNotFoundException("Can't create the folder " + parent.getAbsolutePath());
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(settingsFile),
                StandardCharsets.UTF_8)) {
                properties.store(writer, null);
            }
        }
    }

    static String decode(String value) {
        if (value != null && value.startsWith("ENC(") && value.endsWith(")")) {
            try {
                return PassCoder.decode(value.substring(4, value.length() - 1),
                    DynamicPropertySource.get().getSecretKey(),
                    DynamicPropertySource.get().getCipher());
            } catch (Exception e) {
                return "";
            }
        } else {
            return value;
        }
    }

    private String getSecretKey() {
        return StringUtils.trimToNull(resolver.getProperty("secret.key"));
    }

    private String getCipher() {
        return StringUtils.trimToNull(resolver.getProperty("secret.cipher"));
    }

}
