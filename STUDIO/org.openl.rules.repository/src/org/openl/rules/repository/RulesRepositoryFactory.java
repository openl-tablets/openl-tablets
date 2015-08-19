package org.openl.rules.repository;

import java.util.Map;

import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.config.SysConfigManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository Factory. It is Abstract Factory.
 * <p/>
 * Takes init values from repository.properties file.
 *
 * @author Aleh Bykhavets
 */
public class RulesRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(RulesRepositoryFactory.class);

    public static final String DEFAULT_PROP_FILE = "system.properties";// "rules-repository.properties";

    public static final String MSG_FAILED = "Failed to initialize RulesRepositoryFactory!";

    /**
     * default value is <code>null</code> -- fail first
     */
    private static final ConfigPropertyString confRepositoryFactoryClass = new ConfigPropertyString("design-repository.factory",
        null);

    private RRepositoryFactory repFactory;

    private ConfigSet config;

    public synchronized RRepository getRulesRepositoryInstance() throws RRepositoryException {
        if (repFactory == null) {
            initRepositoryFactory();
        }

        return repFactory.getRepositoryInstance();
    }

    public RRepositoryFactory getRepositoryFactory() {
        return repFactory;
    }

    private void initRepositoryFactory() throws RRepositoryException {
        if (config == null) {
            config = SysConfigManager.getConfigManager().locate(DEFAULT_PROP_FILE);
        }
        if (config == null) {
            throw new RRepositoryException(MSG_FAILED, new IllegalStateException("Config isn't initialized"));
        }

        config.updateProperty(confRepositoryFactoryClass);

        String className = confRepositoryFactoryClass.getValue();
        try {
            Class<?> c = Class.forName(className);
            Object obj = c.newInstance();
            repFactory = (RRepositoryFactory) obj;
            // initialize
            repFactory.initialize(config);
            if (repFactory instanceof RulesRepositoryFactoryAware) {
                ((RulesRepositoryFactoryAware) repFactory).setRulesRepositoryFactory(this);
            }
        } catch (Exception e) {
            log.error(MSG_FAILED, e);
            throw new RRepositoryException(MSG_FAILED, e);
        } catch (UnsupportedClassVersionError e) {
            String message = "Library was compiled using newer version of JDK";
            log.error(message, e);
            throw new RRepositoryException(message, e);
        }
    }

    public synchronized void destroy() throws RRepositoryException {
        if (repFactory != null) {
            repFactory.release();
            repFactory = null;
        }
    }

    public void setConfigSet(ConfigSet config) {
        this.config = config;
    }

    public void setConfig(Map<String, Object> config) {
        ConfigSet rulesConfig = new ConfigSet();
        rulesConfig.addProperties(config);
        setConfigSet(rulesConfig);
    }
}
