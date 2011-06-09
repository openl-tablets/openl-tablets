package org.openl.rules.webstudio;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Configuration manager.
 * 
 * @author Andrei Astrouski
 * 
 * TODO Move to Commons project
 */
public class ConfigManager {

    private static final Log LOG = LogFactory.getLog(ConfigManager.class);

    private boolean useSystemProperties;
    private String propsLocation;
    private String defaultPropsLocation;

    private CompositeConfiguration configuration;

    public ConfigManager(boolean useSystemProperties,
            String propsLocation, String defaultPropsLocation) {
        this.useSystemProperties = useSystemProperties;
        this.propsLocation = propsLocation;
        this.defaultPropsLocation = defaultPropsLocation;

        init();
    }

    private void init() {
        configuration = new CompositeConfiguration();

        if (useSystemProperties) {
            configuration.addConfiguration(new SystemConfiguration());
        }

        Configuration propsConfiguration = createConfiguration(propsLocation);
        if (propsConfiguration != null) {
            configuration.addConfiguration(propsConfiguration);
        }

        Configuration defaultConfiguration = createConfiguration(defaultPropsLocation);
        if (defaultConfiguration != null) {
            configuration.addConfiguration(defaultConfiguration);
        }
    }

    private Configuration createConfiguration(String configLocation) {
        PropertiesConfiguration configuration = null;
        if (configLocation != null) {
            try {
                configuration = new PropertiesConfiguration(configLocation);
            } catch (Exception e) {
                LOG.error("Error when initializing configuration: " + configLocation, e);
            }
        }
        return configuration;
    }

    public Object getProperty(String key) {
        return configuration.getProperty(key);
    }

    public String getStringProperty(String key) {
        return configuration.getString(key);
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        for (Iterator<?> iterator = configuration.getKeys(); iterator.hasNext();) {
            String key = (String) iterator.next();
            properties.put(key, getProperty(key));
        }
        return properties;
    }

    public void setProperty(String key, Object value) {
    }

}
