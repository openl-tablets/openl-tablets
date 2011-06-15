package org.openl.rules.webstudio;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.FileConfiguration;
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
 * TODO Separate configuration sets from the manager
 */
public class ConfigManager {

    private static final Log LOG = LogFactory.getLog(ConfigManager.class);

    private boolean useSystemProperties;
    private String propsLocation;
    private String defaultPropsLocation;

    private FileConfiguration configurationToSave;
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

        configurationToSave = createFileConfiguration(propsLocation);
        if (configurationToSave != null) {
            configuration.addConfiguration(configurationToSave);
        }

        Configuration defaultConfiguration = createFileConfiguration(defaultPropsLocation);
        if (defaultConfiguration != null) {
            configuration.addConfiguration(defaultConfiguration);
        }
    }

    private FileConfiguration createFileConfiguration(String configLocation, boolean createIfNotExist) {
        PropertiesConfiguration configuration = null;
        if (configLocation != null) {
            try {
                if (createIfNotExist) {
                    configuration = new PropertiesConfiguration(new File(configLocation));
                } else {
                    configuration = new PropertiesConfiguration(configLocation);
                }
            } catch (Exception e) {
                LOG.error("Error when initializing configuration: " + configLocation, e);
            }
        }
        return configuration;
    }

    private FileConfiguration createFileConfiguration(String configLocation) {
        return createFileConfiguration(configLocation, false);
    }

    public Object getProperty(String key) {
        return configuration.getProperty(key);
    }

    public String getStringProperty(String key) {
        return configuration.getString(key);
    }

    public boolean getBooleanProperty(String key) {
        return configuration.getBoolean(key);
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
        if (key != null && value != null) {
            String propValue = configuration.getString(key);
            if (propValue != null) {
                if (!propValue.equals(value.toString())) {
                    getConfigurationToSave().setProperty(key, value);
                }
            } else {
                getConfigurationToSave().addProperty(key, value);
            }
        }
    }

    private FileConfiguration getConfigurationToSave() {
        if (configurationToSave == null) {
            configurationToSave = createFileConfiguration(propsLocation, true);
        }
        return configurationToSave;
    }

    public boolean save() {
        if (configurationToSave != null && !configurationToSave.isEmpty()) {
            try {
                getConfigurationToSave().save();
                return true;
            } catch (Exception e) {
                LOG.error("Error when saving configuration: " + configurationToSave.getBasePath(), e);
            }
        }
        return false;
    }

}
