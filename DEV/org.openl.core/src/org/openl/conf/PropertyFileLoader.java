package org.openl.conf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.util.PropertiesUtils;

/**
 * Class PropertyFileLoader loads a property file using the following algorithm:
 * <p>
 * 1) if exists property <code>propertiesFileProperty</code> it's value becomes <code>property_file_name</code>
 * otherwise <code>propertiesFileDefaultName</code> is used.
 * <p>
 * 2) It tries to load properties file in the following order: 2.1) as URL 2.2) as resource in context classpath 2.3) as
 * file in context filesystem
 *
 * @see org.openl.conf.IConfigurableResourceContext
 *
 * @author snshor
 *
 */
public class PropertyFileLoader {

    private final Logger log = LoggerFactory.getLogger(PropertyFileLoader.class);

    private final String propertiesFileDefaultName;
    private final String propertiesFileProperty;
    private Map<String, String> properties;
    private final IConfigurableResourceContext context;
    private final PropertyFileLoader parent;

    public PropertyFileLoader(String propertiesFileDefaultName,
            String propertiesFileProperty,
            IConfigurableResourceContext context,
            PropertyFileLoader parent) {
        this.context = context;
        this.propertiesFileDefaultName = propertiesFileDefaultName;
        this.propertiesFileProperty = propertiesFileProperty;
        this.parent = parent;
    }

    protected IConfigurableResourceContext getContext() {
        return context;
    }

    Map<String, String> getProperties() {
        if (properties != null) {
            return properties;
        }

        // check the propertiesFileProperty first
        String propertiesFileName = getContext().findProperty(propertiesFileProperty);
        if (propertiesFileName == null) {
            propertiesFileName = propertiesFileDefaultName;
        }

        // is it valid URL?
        log.debug("Looking for '{}'.", propertiesFileName);
        if (!loadAsURL(propertiesFileName) && !loadAsResource(propertiesFileName) && !loadAsFile(propertiesFileName)) {
            properties = parent == null ? Collections.emptyMap() : parent.getProperties();
        }

        return properties;
    }

    public String getProperty(String propertyName) {
        String res = getProperties().get(propertyName);

        if (res != null) {
            return res;
        }

        res = getContext().findProperty(propertyName);

        if (res != null) {
            return res;
        }

        return parent == null ? null : parent.getProperty(propertyName);
    }

    boolean loadAsFile(String url) {
        try {
            File f = getContext().findFileSystemResource(url);
            if (f == null) {
                return false;
            }
            properties = loadProperties(f);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean loadAsResource(String name) {
        try {
            URL url = getContext().findClassPathResource(name);
            if (url == null) {
                return false;
            }
            properties = loadProperties(url);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean loadAsURL(String url) {
        try {
            properties = loadProperties(new URL(url));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Map<String, String> loadProperties(URL url) throws IOException {
        var props = new HashMap<String, String>();
        PropertiesUtils.load(url, props::put);
        return props;
    }

    private Map<String, String> loadProperties(File f) throws IOException {
        var props = new HashMap<String, String>();
        PropertiesUtils.load(f.toPath(), props::put);
        return props;
    }

}
