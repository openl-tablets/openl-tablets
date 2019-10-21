/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class PropertyFileLoader loads a property file using the following algorithm:
 *
 * 1) if exists property <code>propertiesFileProperty</code> it's value becomes <code>property_file_name</code>
 * otherwise <code>propertiesFileDefaultName</code> is used.
 *
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

    public static final Properties NO_PROPERTIES = new Properties();

    private String propertiesFileDefaultName;

    private String propertiesFileProperty;

    private Properties properties = null;

    private IConfigurableResourceContext context;

    private PropertyFileLoader parent = null;

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

    Properties getProperties() {
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
            properties = parent == null ? NO_PROPERTIES : parent.getProperties();
        }

        return properties;

    }

    public String getProperty(String propertyName) {
        String res = getProperties().getProperty(propertyName);

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
        } catch (Throwable t) {
            // System.out.println("File not as found: " + url);
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
        } catch (Throwable t) {
            // Log.debug("Loading as resource: ", t);
            return false;
        }
    }

    boolean loadAsURL(String url) {
        try {
            properties = loadProperties(new URL(url));
            return true;
        } catch (Throwable t) {
            // Log.debug("Loading as url: ", t);
            return false;
        }
    }

    private Properties loadProperties(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            Properties props = new Properties();
            props.load(in);
            return props;
        }
    }

    private Properties loadProperties(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            Properties props = new Properties();
            props.load(in);
            return props;
        }
    }

}
