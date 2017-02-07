package org.openl.rules.webstudio.web.admin;

import org.openl.config.ConfigurationManager;

public abstract class RepositorySettings {
    protected void fixState() {
    }

    protected void store(ConfigurationManager configurationManager) {
    }

    protected void onTypeChanged(JcrType newJcrType) {
    }

    public void copyContent(RepositorySettings other) {
    }
}
