package org.openl.rules.webstudio.web.admin;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang.StringUtils;
import org.openl.config.ConfigurationManager;

public class RepositoryConfiguration {
    private ConfigurationManager configManager;

    private static final String PRODUCTION_REPOSITORY_FACTORY = "production-repository.factory";
    private static final String PRODUCTION_REPOSITORY_NAME = "production-repository.name";
    /** @deprecated */
    private static final BidiMap PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP = new DualHashBidiMap();
    static {
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("local",
                "org.openl.rules.repository.factories.LocalJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("rmi",
                "org.openl.rules.repository.factories.RmiJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("webdav",
                "org.openl.rules.repository.factories.WebDavJackrabbitProductionRepositoryFactory");
    };
    /** @deprecated */
    private static final Map<String, String> PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP = new HashMap<String, String>();
    static {
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("local", "production-repository.local.home");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("rmi", "production-repository.remote.rmi.url");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("webdav", "production-repository.remote.webdav.url");
    };

    private String configName;

    private String oldName = null;

    public RepositoryConfiguration(String configName, ConfigurationManager configManager) {
        this.configName = configName.toLowerCase();
        this.configManager = configManager;
    }

    public String getName() {
        return configManager.getStringProperty(PRODUCTION_REPOSITORY_NAME);
    }

    public void setName(String name) {
        // TODO name validation: only characters, numbers, underscore, hyphen
        // and space
        oldName = getName();
        configManager.setProperty(PRODUCTION_REPOSITORY_NAME, StringUtils.trimToEmpty(name));
    }

    public String getType() {
        String factory = configManager.getStringProperty(PRODUCTION_REPOSITORY_FACTORY);
        return (String) PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.getKey(factory);
    }

    public void setType(String type) {
        configManager.setProperty(PRODUCTION_REPOSITORY_FACTORY, PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.get(type));
    }

    public String getPath() {
        return configManager.getStringProperty(PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(getType()));
    }

    public void setPath(String path) {
        configManager.setProperty(PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(getType()),
                StringUtils.trimToEmpty(path));
    }

    public String getConfigName() {
        return configName;
    }

    public boolean save() {
        return configManager.save();
    }

    public boolean restoreDefaults() {
        return configManager.restoreDefaults();
    }

    public boolean delete() {
        return configManager.delete();
    }

    public void copyContent(RepositoryConfiguration other) {
        // do not copy id, only content
        setName(other.getName());
        setType(other.getType());
        setPath(other.getPath());
    }

    public boolean isNameChanged() {
        String name = getName();
        return name != null && !name.equals(oldName) || name == null && oldName != null;
    }

    public boolean isNameChangedIgnoreCase() {
        String name = getName();
        return name != null && !name.equalsIgnoreCase(oldName) || name == null && oldName != null;
    }

    public boolean isProductionRepositoryPathSystem() {
        String type = getType();
        return configManager.isSystemProperty(PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type));
    }
}
