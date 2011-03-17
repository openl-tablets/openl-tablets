package org.openl.rules.eclipse.wizard;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The main plugin class to be used in the desktop.
 */
public class RulesWizardPlugin2 extends AbstractUIPlugin {
    // The shared instance.
    private static RulesWizardPlugin2 plugin;
    // Resource bundle.
    private ResourceBundle resourceBundle;

    /**
     * Returns the shared instance.
     */
    public static RulesWizardPlugin2 getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = RulesWizardPlugin2.getDefault().getResourceBundle();
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the workspace instance.
     */
    public static IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    /**
     * The constructor.
     */
    public RulesWizardPlugin2() {
        super();
        plugin = this;
        try {
            resourceBundle = ResourceBundle.getBundle("org.openl.rules.eclipse.wizard.PluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
}
