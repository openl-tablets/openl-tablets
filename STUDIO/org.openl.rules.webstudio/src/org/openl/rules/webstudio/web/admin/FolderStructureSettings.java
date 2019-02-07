package org.openl.rules.webstudio.web.admin;

import org.openl.config.ConfigurationManager;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.RepositoryMode;

public class FolderStructureSettings {
    private final ConfigurationManager configManager;

    private final String BASE_PATH;
    private final String FLAT_FOLDER_STRUCTURE;
    private final String FOLDER_CONFIG_FILE;

    public FolderStructureSettings(ConfigurationManager configManager, RepositoryMode repositoryMode) {
        String configPrefix;
        switch (repositoryMode) {
            case DESIGN:
                configPrefix = RepositoryFactoryInstatiator.DESIGN_REPOSITORY;
                break;
            case DEPLOY_CONFIG:
                configPrefix = RepositoryFactoryInstatiator.DEPLOY_CONFIG_REPOSITORY;
                break;
            case PRODUCTION:
                configPrefix = RepositoryFactoryInstatiator.PRODUCTION_REPOSITORY;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        this.configManager = configManager;
        BASE_PATH = configPrefix + "base.path";
        FLAT_FOLDER_STRUCTURE = configPrefix + "folder-structure.flat";
        FOLDER_CONFIG_FILE = configPrefix + "folder-structure.configuration";
    }

    public String getBasePath() {
        return configManager.getStringProperty(BASE_PATH);
    }

    public void setBasePath(String basePath) {
        String value = basePath.isEmpty() || basePath.endsWith("/") ? basePath : basePath + "/";
        configManager.setProperty(BASE_PATH, value);
    }

    public boolean isFlatFolderStructure() {
        return configManager.getBooleanProperty(FLAT_FOLDER_STRUCTURE, Boolean.TRUE);
    }

    public void setFlatFolderStructure(boolean flatFolderStructure) {
        configManager.setProperty(FLAT_FOLDER_STRUCTURE, flatFolderStructure);
    }

    public String getFolderConfigFile() {
        return configManager.getStringProperty(FOLDER_CONFIG_FILE, null);
    }

    public void setFolderConfigFile(String folderConfigFile) {
        configManager.setProperty(FOLDER_CONFIG_FILE, folderConfigFile);
    }
}
