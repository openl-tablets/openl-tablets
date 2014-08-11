package org.openl.rules.repository;

import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.config.SysConfigManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Repository Factory. It is Abstract Factory.
 * <p/>
 * Takes init values from repository.properties file.
 *
 * @author Aleh Bykhavets
 */
public class RulesRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(RulesRepositoryFactory.class);

    public static final String DEFAULT_PROP_FILE = "system.properties";//"rules-repository.properties";

    public static final String MSG_FAILED = "Failed to initialize RulesRepositoryFactory!";

    /**
     * default value is <code>null</code> -- fail first
     */
    private static final ConfigPropertyString confRepositoryFactoryClass = new ConfigPropertyString(
            "design-repository.factory", null);

    private RRepositoryFactory repFactory;

    private boolean isFailed;

    private ConfigSet config;

    /**
     * For backward compatibility. Used in deprecated methods only.
     */
    private static RulesRepositoryFactory instance = null;

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
            throw new RRepositoryException(MSG_FAILED, new NullPointerException());
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
            isFailed = true;
            log.error(MSG_FAILED, e);
            throw new RRepositoryException(MSG_FAILED, e);
        }

        isFailed = false;
    }

    public boolean isBroken() {
        if (!isFailed && repFactory == null) {
            // first time, lets check
            try {
                initRepositoryFactory();
            } catch (RRepositoryException e) {
                // ignore
                // isFailed = true;
            }
        }

        return isFailed;
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

    ///////////////////// Deprecated stuff /////////////////////

    /**
     * @throws RRepositoryException
     * @deprecated use instance methods instead. For example, obtain class instance using spring
     */
    @Deprecated
    public static synchronized RRepository getRepositoryInstance() throws RRepositoryException {
        return getInstance().getRulesRepositoryInstance();
    }

    /**
     * @deprecated use instance methods instead. For example, obtain class instance using spring
     */
    @Deprecated
    public static RRepositoryFactory getRepFactory() {
        return getInstance().getRepositoryFactory();
    }

    /**
     * @deprecated use instance methods instead. For example, obtain class instance using spring
     */
    @Deprecated
    public static boolean isFailed() {
        return getInstance().isBroken();
    }

    /**
     * @throws RRepositoryException
     * @deprecated use instance methods instead. For example, obtain class instance using spring
     */
    @Deprecated
    public static synchronized void release() throws RRepositoryException {
        getInstance().destroy();
    }

    /**
     * @deprecated use instance methods instead. For example, obtain class instance using spring
     */
    @Deprecated
    public static ConfigSet getConfig() {
        return getInstance().config;
    }

    /**
     * Deprecated stuff, used internally. Only for backward compatibility.
     *
     * @return RulesRepositoryFactory instance
     */
    private static RulesRepositoryFactory getInstance() {
        if (instance == null) {
            instance = new RulesRepositoryFactory();
        }

        return instance;
    }
}
