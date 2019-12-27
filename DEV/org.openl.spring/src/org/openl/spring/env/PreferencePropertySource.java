package org.openl.spring.env;

import java.util.prefs.Preferences;

import org.openl.util.StringUtils;
import org.springframework.core.env.PropertySource;

public class PreferencePropertySource extends PropertySource<Preferences> {
    public static final String PROPS_NAME = "OpenL preference properties";

    PreferencePropertySource(String appName) {
        super(PROPS_NAME, Preferences.userRoot().node(StringUtils.isEmpty(appName) ? "openl" : "openl/" + appName));
    }

    @Override
    public Object getProperty(String name) {
        return getSource().get(name, null);
    }
}
