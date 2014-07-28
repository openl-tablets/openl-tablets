package org.openl.config;

import org.apache.commons.lang3.StringUtils;

public class ConfigurationManagerFactory {
    private boolean useSystemProperties;
    private String defaultPropertiesLocation;
    private String propertiesFolder;

    public ConfigurationManagerFactory(boolean useSystemProperties, String defaultPropertiesLocation,
            String propertiesFolder) {
        this.useSystemProperties = useSystemProperties;
        this.defaultPropertiesLocation = StringUtils.trimToNull(defaultPropertiesLocation);

        if (!StringUtils.isBlank(propertiesFolder) && !propertiesFolder.endsWith("/")
                && !propertiesFolder.endsWith("\\")) {
            propertiesFolder += "/";
        }
        this.propertiesFolder = StringUtils.trimToEmpty(propertiesFolder);
    }

    public ConfigurationManager getConfigurationManager(String propertiesName) {
        String fullPath = propertiesFolder + propertiesName;
        String defaultFile = defaultPropertiesLocation != null ? defaultPropertiesLocation : fullPath;
        return new ConfigurationManager(useSystemProperties, StringUtils.trimToNull(fullPath),
                StringUtils.trimToNull(defaultFile));
    }
}
