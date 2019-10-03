package org.openl.rules.webstudio.util;

import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.openl.rules.webstudio.web.servlet.StartupListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PreferencesManager {

    INSTANCE;

    private static final String WEBSTUDIO_WORKING_DIR_KEY = "webstudio.home";
    private static final String WEBSTUDIO_CONFIGURED_MARKER = "webStudioConfigured.txt";
    private final Logger log = LoggerFactory.getLogger(StartupListener.class);

    public boolean isAppConfigured() {
        String configured = System.getProperty("webstudio.configured");
        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }
        String homePath = readValue(WEBSTUDIO_WORKING_DIR_KEY);
        File configuredMarker = new File(homePath + File.separator + WEBSTUDIO_CONFIGURED_MARKER);
        return configuredMarker.exists();
    }

    public void setWebStudioHomeDir(String workingDir) {
        System.setProperty("webstudio.home", workingDir);
        writeValue(WEBSTUDIO_WORKING_DIR_KEY, workingDir);
        setWebStudioHomeNotConfigured(workingDir);
    }

    private void setWebStudioHomeNotConfigured(String homePath) {
        File configuredMarker = new File(homePath + File.separator + WEBSTUDIO_CONFIGURED_MARKER);
        if (configuredMarker.exists()) {
            configuredMarker.delete();
        }
    }

    public void webStudioConfigured() {
        String homePath = readValue(WEBSTUDIO_WORKING_DIR_KEY);
        File configuredMarker = new File(homePath + File.separator + WEBSTUDIO_CONFIGURED_MARKER);
        try {
            if (!configuredMarker.exists()) {
                configuredMarker.createNewFile();
            }
        } catch (IOException e) {
            log.error("cannot create configured file", e);
        }
    }

    public String getWebStudioHomeDir() {
        String webStudioHomeDirProp = System.getProperty("webstudio.home");
        if (webStudioHomeDirProp != null) {
            return webStudioHomeDirProp;
        }
        return readValue(WEBSTUDIO_WORKING_DIR_KEY);
    }

    private String readValue(String key) {
        String applicationNodePath = getApplicationNode();
        Preferences node = Preferences.userRoot().node(applicationNodePath);
        String value = node.get(key, null);
        return value;
    }

    private void writeValue(String key, String value) {
        String applicationNodePath = getApplicationNode();
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

    private String getApplicationNode() {
        // TODO define uniq node name
        return "openl/webstudio";
    }
}
