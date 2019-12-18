package org.openl.rules.webstudio.util;

import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.openl.rules.webstudio.web.servlet.StartupListener;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PreferencesManager {

    INSTANCE;

    public static final String WEBSTUDIO_WORKING_DIR_KEY = "openl.home";
    private static final String WEBSTUDIO_CONFIGURED_MARKER = "webStudioConfigured.txt";
    private final Logger log = LoggerFactory.getLogger(StartupListener.class);

    public boolean isAppConfigured(String appName) {
        String configured = System.getProperty("webstudio.configured");
        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }
        String homePath = readValue(appName, WEBSTUDIO_WORKING_DIR_KEY);
        File configuredMarker = new File(homePath + File.separator + WEBSTUDIO_CONFIGURED_MARKER);
        return configuredMarker.exists();
    }

    public void setWebStudioHomeDir(String appName, String workingDir) {
        writeValue(appName, WEBSTUDIO_WORKING_DIR_KEY, workingDir);
        setWebStudioHomeNotConfigured(workingDir);
    }

    private void setWebStudioHomeNotConfigured(String homePath) {
        File configuredMarker = new File(homePath + File.separator + WEBSTUDIO_CONFIGURED_MARKER);
        if (configuredMarker.exists()) {
            configuredMarker.delete();
        }
    }

    public void webStudioConfigured(String appName) {
        String homePath = readValue(appName, WEBSTUDIO_WORKING_DIR_KEY);
        File configuredMarker = new File(homePath + File.separator + WEBSTUDIO_CONFIGURED_MARKER);
        try {
            if (!configuredMarker.exists()) {
                configuredMarker.createNewFile();
            }
        } catch (IOException e) {
            log.error("cannot create configured file", e);
        }
    }

    private String readValue(String appName, String key) {
        String applicationNodePath = getApplicationNode(appName);
        Preferences node = Preferences.userRoot().node(applicationNodePath);
        return node.get(key, null);
    }

    private void writeValue(String appName, String key, String value) {
        String applicationNodePath = getApplicationNode(appName);
        Preferences node = Preferences.userRoot().node(applicationNodePath);
        node.put(key, value);
        try {
            // guard against loss in case of abnormal termination of the VM
            // in case of normal VM termination, the flush method is not required
            node.flush();
        } catch (BackingStoreException e) {
            log.error("cannot save preferences value", e);
        }
    }

    private String getApplicationNode(String appName) {
        return StringUtils.isEmpty(appName) ? "openl" : "openl/" + appName;
    }
}
