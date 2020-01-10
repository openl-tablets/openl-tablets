package org.openl.spring.env;

import java.util.prefs.Preferences;

import org.openl.util.StringUtils;
import org.springframework.core.env.PropertySource;

/**
 * Properties stored in a hierarchical collection of preference data using Preferences API. See
 * {@link java.util.prefs.Preferences Preferences} for details. Preferences will be stored in a node with a key:
 * <code>openl/{appName}</code>
 */
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
