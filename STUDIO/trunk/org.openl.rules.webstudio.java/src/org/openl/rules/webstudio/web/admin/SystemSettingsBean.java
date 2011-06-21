package org.openl.rules.webstudio.web.admin;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.openl.rules.webstudio.ConfigManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * TODO Remove property getters/setters when migrating to JSF 2.0
 * 
 * @author Andrei Astrouski
 */
public class SystemSettingsBean {

    private static final String WORKSPACES_ROOT = "webstudio.workspacesRoot";
    private static final String LOCAL_WORKSPACE = "webstudio.localWorkspace";
    private static final String PROJECT_HISTORY_HOME = "project.history.home";
    private static final String DATE_PATTERN = "webstudio.datePattern";

    private static final String AUTO_LOGIN = "security.autoLogin";
    private static final String HIDE_LOGOUT = "webstudio.hideLogout";

    private static final String DESIGN_REPOSITORY_FACTORY = "design-repository.factory";
    private static final String DESIGN_REPOSITORY_NAME = "design-repository.name";
    /** @deprecated */
    private static final BidiMap DESIGN_REPOSITORY_TYPE_FACTORY_MAP = new DualHashBidiMap();
    static {
        DESIGN_REPOSITORY_TYPE_FACTORY_MAP.put("local", "org.openl.rules.repository.factories.LocalJackrabbitDesignRepositoryFactory");
        DESIGN_REPOSITORY_TYPE_FACTORY_MAP.put("rmi", "org.openl.rules.repository.factories.RmiJackrabbitDesignRepositoryFactory");
        DESIGN_REPOSITORY_TYPE_FACTORY_MAP.put("webdav", "org.openl.rules.repository.factories.WebDavJackrabbitDesignRepositoryFactory");
    };
    /** @deprecated */
    private static final Map<String, String> DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP = new HashMap<String, String>();
    static {
        DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("local", "design-repository.local.home");
        DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("rmi", "design-repository.remote.rmi.url");
        DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("webdav", "design-repository.remote.webdav.url");
    };

    private static final String PRODUCTION_REPOSITORY_FACTORY = "production-repository.factory";
    private static final String PRODUCTION_REPOSITORY_NAME = "production-repository.name";
    /** @deprecated */
    private static final BidiMap PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP = new DualHashBidiMap();
    static {
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("local", "org.openl.rules.repository.factories.LocalJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("rmi", "org.openl.rules.repository.factories.RmiJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("webdav", "org.openl.rules.repository.factories.WebDavJackrabbitProductionRepositoryFactory");
    };
    /** @deprecated */
    private static final Map<String, String> PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP = new HashMap<String, String>();
    static {
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("local", "production-repository.local.home");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("rmi", "production-repository.remote.rmi.url");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("webdav", "production-repository.remote.webdav.url");
    };

    private ConfigManager configManager = WebStudioUtils.getWebStudio().getSystemConfigManager();

    public String getWorkspacesRoot() {
        return configManager.getStringProperty(WORKSPACES_ROOT);
    }

    public void setWorkspacesRoot(String workspacesRoot) {
        configManager.setProperty(WORKSPACES_ROOT, workspacesRoot);
    }

    public String getLocalWorkspace() {
        return configManager.getStringProperty(LOCAL_WORKSPACE);
    }

    public void setLocalWorkspace(String localWorkspace) {
        configManager.setProperty(LOCAL_WORKSPACE, localWorkspace);
    }

    public String getDatePattern() {
        return configManager.getStringProperty(DATE_PATTERN);
    }

    public void setDatePattern(String datePattern) {
        configManager.setProperty(DATE_PATTERN, datePattern);
    }

    public boolean isAutoLogin() {
        return configManager.getBooleanProperty(AUTO_LOGIN);
    }

    public void setAutoLogin(boolean autoLogin) {
        configManager.setProperty(AUTO_LOGIN, autoLogin);
    }

    public boolean isHideLogout() {
        return configManager.getBooleanProperty(HIDE_LOGOUT);
    }

    public void setHideLogout(boolean hideLogout) {
        configManager.setProperty(HIDE_LOGOUT, hideLogout);
    }

    public String getProjectHistoryHome() {
        return configManager.getStringProperty(PROJECT_HISTORY_HOME);
    }

    public void setProjectHistoryHome(String projectHistoryHome) {
        configManager.setProperty(PROJECT_HISTORY_HOME, projectHistoryHome);
    }

    public String getDesignRepositoryType() {
        String factory = configManager.getStringProperty(DESIGN_REPOSITORY_FACTORY);
        return (String) DESIGN_REPOSITORY_TYPE_FACTORY_MAP.getKey(factory);
    }

    public String getDesignRepositoryName() {
        return configManager.getStringProperty(DESIGN_REPOSITORY_NAME);
    }

    public void setDesignRepositoryName(String name) {
        configManager.setProperty(DESIGN_REPOSITORY_NAME, name);
    }

    public void setDesignRepositoryType(String type) {
        configManager.setProperty(
                DESIGN_REPOSITORY_FACTORY, DESIGN_REPOSITORY_TYPE_FACTORY_MAP.get(type));
    }

    public String getDesignRepositoryPath() {
        String type = getDesignRepositoryType();
        return configManager.getStringProperty(
                DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type));
    }

    public void setDesignRepositoryPath(String path) {
        String type = getDesignRepositoryType();
        configManager.setProperty(
                DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type), path);
    }

    public String getProductionRepositoryName() {
        return configManager.getStringProperty(PRODUCTION_REPOSITORY_NAME);
    }

    public void setProductionRepositoryName(String name) {
        configManager.setProperty(PRODUCTION_REPOSITORY_NAME, name);
    }

    public String getProductionRepositoryType() {
        String factory = configManager.getStringProperty(PRODUCTION_REPOSITORY_FACTORY);
        return (String) PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.getKey(factory);
    }

    public void setProductionRepositoryType(String type) {
        configManager.setProperty(
                PRODUCTION_REPOSITORY_FACTORY, PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.get(type));
    }

    public String getProductionRepositoryPath() {
        String type = getDesignRepositoryType();
        return configManager.getStringProperty(
                PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type));
    }

    public void setProductionRepositoryPath(String path) {
        String type = getDesignRepositoryType();
        configManager.setProperty(
                PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type), path);
    }

    public void applyChanges() {
        boolean saved = configManager.save();
        if (saved) {
            WebStudioUtils.getWebStudio().setNeedRestart(true);
        }
    }

    public void restoreDefaults() {
        boolean restored = configManager.restoreDefaults();
        if (restored) {
            WebStudioUtils.getWebStudio().setNeedRestart(true);
        }
    }

}
