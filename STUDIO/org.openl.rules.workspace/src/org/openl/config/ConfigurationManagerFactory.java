package org.openl.config;

import org.openl.util.StringUtils;

public class ConfigurationManagerFactory {
    private boolean useSystemProperties;
    private String defaultPropertiesLocation;
    private String propertiesFolder;
    private String propertiesInContextFolder;

    public ConfigurationManagerFactory(boolean useSystemProperties,
            String defaultPropertiesLocation,
            String propertiesFolder) {
        this(useSystemProperties, defaultPropertiesLocation, propertiesFolder, null);
    }

    public ConfigurationManagerFactory(boolean useSystemProperties,
            String defaultPropertiesLocation,
            String propertiesFolder,
            String propertiesInContextFolder) {
        this.useSystemProperties = useSystemProperties;
        this.defaultPropertiesLocation = StringUtils.trimToNull(defaultPropertiesLocation);

        if (StringUtils
            .isNotBlank(propertiesFolder) && !propertiesFolder.endsWith("/") && !propertiesFolder.endsWith("\\")) {
            propertiesFolder += "/";
        }
        this.propertiesFolder = StringUtils.trimToEmpty(propertiesFolder);
        this.propertiesInContextFolder = propertiesInContextFolder;
    }

    public ConfigurationManager getConfigurationManager(String propertiesName) {
        String fullPath = propertiesFolder + propertiesName;
        String contextPath = propertiesInContextFolder == null ? null : propertiesInContextFolder + propertiesName;
        String defaultFile = defaultPropertiesLocation != null ? defaultPropertiesLocation : fullPath;
        return new ConfigurationManager(useSystemProperties,
            StringUtils.trimToNull(fullPath),
            StringUtils.trimToNull(contextPath),
            StringUtils.trimToNull(defaultFile),
            false);
    }
}
