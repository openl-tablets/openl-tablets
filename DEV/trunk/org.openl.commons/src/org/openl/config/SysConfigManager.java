package org.openl.config;

/**
 * SysConfigManager is static oriented class.
 * <p>
 * It should be used for old-style or complicated cases only.
 * 
 * @author Aleh Bykhavets
 */
public class SysConfigManager {
    /** system config manager */
    private static ConfigManager configManager;

    static {
        // create default config manager
        configManager = new ConfigManager();
        // just seek in class path
        configManager.addLocator(new ClassPathConfigLocator());
    }

    /**
     * Returns system config manager.
     * <p>
     * It is one for all classes within current class loader and its scope.
     * 
     * @return system config manager
     */
    public static ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Replace default ConfigManager with supplied one.
     * 
     * @param manager new config manager
     */
    public static void setConfigManager(ConfigManager manager) {
        configManager = manager;
    }
}
