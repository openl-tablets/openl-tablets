package org.openl.rules.webstudio.util;

import org.openl.rules.webstudio.web.servlet.StartupListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public enum PreferencesManager {

    INSTANCE;

    private static final String WEBSTUDIO_WORKING_DIR_KEY = "webstudio.home";
    private final Logger log = LoggerFactory.getLogger(StartupListener.class);

    public boolean isAppConfigured() {
        String homePath = readValue(WEBSTUDIO_WORKING_DIR_KEY);
        return homePath != null && !isDirEmpty(homePath);
    }

    public void setWebStudioHomeDir(String workingDir) {
        System.setProperty("webstudio.home", workingDir);
        writeValue(WEBSTUDIO_WORKING_DIR_KEY, workingDir);
    }

    public String getWebStudioHomeDir() {
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
            //guard against loss in case of abnormal termination of the VM
            //in case of normal VM termination, the flush method is not required
            node.flush();
        } catch (BackingStoreException e) {
            log.error("cannot save preferences value", e);
        }
    }

    private boolean isDirEmpty(String dirPath) {
        File directory = new File(dirPath);
        if (!directory.isDirectory()) {
            return true;
        }
        return directory.listFiles().length == 0;
    }

    private String getApplicationNode() {
        // TODO define uniq node name
        return "openl/webstudio";
    }
}
