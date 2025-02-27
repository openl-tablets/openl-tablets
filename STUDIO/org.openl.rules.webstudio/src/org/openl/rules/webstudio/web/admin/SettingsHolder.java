package org.openl.rules.webstudio.web.admin;

import org.openl.config.PropertiesHolder;

public interface SettingsHolder {

    void load(PropertiesHolder properties);

    void store(PropertiesHolder properties);

    void revert(PropertiesHolder properties);

}
