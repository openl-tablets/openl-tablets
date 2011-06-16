package org.openl.rules.webstudio.web.admin;

import org.openl.rules.webstudio.ConfigManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * TODO Remove property getters/setters when migrating to JSF 2.0
 * 
 * @author Andrei Astrouski
 */
public class SystemSettingsBean {

    private static final String WORKSPACES_ROOT = "webstudio.workspacesRoot";
    private static final String DATE_PATTERN = "webstudio.datePattern";

    private static final String AUTO_LOGIN = "security.autoLogin";
    private static final String HIDE_LOGOUT = "webstudio.hideLogout";

    private ConfigManager configManager = WebStudioUtils.getWebStudio().getSystemConfigManager();

    public String getWorkspacesRoot() {
        return configManager.getStringProperty(WORKSPACES_ROOT);
    }

    public void setWorkspacesRoot(String workspacesRoot) {
        configManager.setProperty(WORKSPACES_ROOT, workspacesRoot);
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
