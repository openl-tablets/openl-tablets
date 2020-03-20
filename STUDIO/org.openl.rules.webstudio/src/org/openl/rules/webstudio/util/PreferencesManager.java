package org.openl.rules.webstudio.util;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PreferencesManager {

    INSTANCE;

    public static final String WEBSTUDIO_WORKING_DIR_KEY = DynamicPropertySource.OPENL_HOME;

    private final Logger log = LoggerFactory.getLogger(PreferencesManager.class);

    public void setWebStudioHomeDir(String appName, String workingDir) {
        writeValue(appName, WEBSTUDIO_WORKING_DIR_KEY, workingDir);
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
