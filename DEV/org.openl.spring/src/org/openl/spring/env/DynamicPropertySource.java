package org.openl.spring.env;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.springframework.core.env.EnumerablePropertySource;

import org.openl.info.OpenLVersion;
import org.openl.util.FileUtils;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;

/**
 * Loads always actual properties from an external file located in ${openl.home} directory.
 *
 * @author Yury Molchan
 */
public class DynamicPropertySource extends EnumerablePropertySource<Object> {
    public static final String PROPS_NAME = "Dynamic properties";

    public static final String OPENL_HOME = "openl.home";
    public static final String OPENL_HOME_SHARED = "openl.home.shared";

    private static final String PROP_VERSION = ".version";

    private final FirewallPropertyResolver resolver;
    private final String appName;

    private volatile Map<String, String> settings;
    private volatile String version;
    private volatile long timestamp;

    public DynamicPropertySource(String appName, FirewallPropertyResolver resolver) {
        super(PROPS_NAME);
        this.resolver = resolver;
        this.appName = appName;
        loadProperties();
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
        var properties = new LinkedHashMap<String, String>();
        long lastModified = file.lastModified();
        if (file.exists()) {
            try {
                PropertiesUtils.load(file.toPath(), properties::put);
                ConfigLog.LOG.info("+       Load: '{}' ({} properties)", getFile(), properties.size());
            } catch (IOException e) {
                ConfigLog.LOG.error("!     Error:", e);
            }
            version = properties.get(PROP_VERSION);
        } else {
            // If the file does not exist, then it is default settings for the current version.
            version = OpenLVersion.getVersion();
        }
        settings = properties;
        timestamp = lastModified;
    }

    private File getFile() {
        String property = resolver.getProperty(OPENL_HOME_SHARED);
        return new File(property, appName + ".properties");
    }

    @Override
    public String getProperty(String name) {
        if (OPENL_HOME.equals(name) || OPENL_HOME_SHARED.equals(name)) {
            // prevent cycled call
            return null;
        }
        String property = settings.get(name);
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

    /**
     * Returns the OpenL version of these properties.
     */
    public String version() {
        return version;
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
        final var properties = new TreeMap<>(settings);
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
                properties.put(propertyName, value);
            }
        }
        var origin = settings;

        // 'unconfigure' settings for matching with defaults. to get settings not from a file
        settings = Collections.emptyMap();

        // Do clean up from default values
        properties.entrySet()
            .removeIf(e -> Objects.equals(resolver.getRawProperty(e.getKey().toString()), e.getValue().toString()));

        // Remove version for correct determining of properties to save
        properties.remove(PROP_VERSION);

        boolean noPropsToSave = properties.isEmpty();

        version = OpenLVersion.getVersion();
        settings = properties;

        if (noPropsToSave) {
            // Nothing to save. Delete old settings.
            File settingsFile = getFile();
            FileUtils.deleteQuietly(settingsFile);
            return;
        }

        // Mark version of the settings for migration purposes.
        properties.put(PROP_VERSION, OpenLVersion.getVersion());

        if (!origin.equals(properties)) {
            // Save the difference only
            var settingsFile = getFile();
            var parent = settingsFile.getParentFile();
            if (!parent.mkdirs() && !parent.exists()) {
                throw new FileNotFoundException("Can't create the folder " + parent.getAbsolutePath());
            }
            PropertiesUtils.store(settingsFile.toPath(), properties.entrySet());
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

    public Map<String, String> getProperties() {
        return settings;
    }

    private String getSecretKey() {
        return StringUtils.trimToNull(resolver.getProperty("secret.key"));
    }

    private String getCipher() {
        return StringUtils.trimToNull(resolver.getProperty("secret.cipher"));
    }

}
