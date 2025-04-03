package org.openl.rules.webstudio.web.admin;

import org.openl.config.PropertiesHolder;

@Deprecated(forRemoval = true)
public class FolderStructureSettings {
    private final RepositoryConfiguration configuration;

    public FolderStructureSettings(RepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getBasePath() {
        return configuration.getSettings().getBasePath();
    }

    public void setBasePath(String basePath) {
        configuration.getSettings().setBasePath(basePath);
    }

    public boolean isFlatFolderStructure() {
        return configuration.getSettings().isFlatFolderStructure();
    }

    public void setFlatFolderStructure(boolean flatFolderStructure) {
        configuration.getSettings().setFlatFolderStructure(flatFolderStructure);
    }

    public FolderStructureValidators getValidators() {
        return new FolderStructureValidators();
    }

    private PropertiesHolder getProperties() {
        return configuration.getProperties();
    }
}
