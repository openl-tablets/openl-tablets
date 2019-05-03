package org.openl.config;

import org.openl.util.StringUtils;

public class ConfigurationManagerFactory {
    private String defaultPropertiesLocation;
    private String propertiesFolder;
    private String propertiesInContextFolder;

    public ConfigurationManagerFactory(String defaultPropertiesLocation,
                                       String propertiesFolder) {
        this(defaultPropertiesLocation, propertiesFolder, null);
    }

    public ConfigurationManagerFactory(String defaultPropertiesLocation,
                                       String propertiesFolder,
                                       String propertiesInContextFolder) {
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
        return new ConfigurationManager(true,
            StringUtils.trimToNull(fullPath),
            StringUtils.trimToNull(contextPath),
            StringUtils.trimToNull(defaultFile),
            false);
    }
}
