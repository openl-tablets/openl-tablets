package org.openl.rules.webstudio.web.admin;

import org.openl.config.PropertiesHolder;
import org.springframework.core.env.PropertyResolver;

public class FolderStructureSettings {

    private final String BASE_PATH;
    private final String FLAT_FOLDER_STRUCTURE;
    private final String FOLDER_CONFIG_FILE;
    private final PropertyResolver propertyResolver;
    private PropertiesHolder properties;

    public FolderStructureSettings(PropertyResolver propertyResolver, String configPrefix, PropertiesHolder properties) {
        String withPrefix = "repository." + configPrefix.toLowerCase();
        BASE_PATH = withPrefix + ".base.path";
        FLAT_FOLDER_STRUCTURE = withPrefix + ".folder-structure.flat";
        FOLDER_CONFIG_FILE = withPrefix + ".folder-structure.configuration";
        this.propertyResolver = propertyResolver;
        this.properties = properties;
    }

    public String getBasePath() {
        return propertyResolver.getProperty(BASE_PATH);
    }

    public void setBasePath(String basePath) {
        String value = basePath.isEmpty() || basePath.endsWith("/") ? basePath : basePath + "/";
        properties.setProperty(BASE_PATH, value);
    }

    public boolean isFlatFolderStructure() {
        return Boolean.parseBoolean(propertyResolver.getProperty(FLAT_FOLDER_STRUCTURE, String.valueOf(Boolean.TRUE)));
    }

    public void setFlatFolderStructure(boolean flatFolderStructure) {
        properties.setProperty(FLAT_FOLDER_STRUCTURE, flatFolderStructure);
    }

    public String getFolderConfigFile() {
        return propertyResolver.getProperty(FOLDER_CONFIG_FILE);
    }

    public void setFolderConfigFile(String folderConfigFile) {
        properties.setProperty(FOLDER_CONFIG_FILE, folderConfigFile);
    }

    public FolderStructureValidators getValidators() {
        return new FolderStructureValidators();
    }
}
