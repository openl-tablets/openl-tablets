package org.openl.rules.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.config.SysConfigManager;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Repository Factory Proxy.
 * <p>
 * Takes actual factory description from <i>rules-production.properties</i>
 * file.
 *
 */
public class ProductionRepositoryFactoryProxy {
    private static final Log log = LogFactory.getLog(ProductionRepositoryFactoryProxy.class);

    public static final String PROP_FILE = "rules-production.properties";
    /** default value is <code>null</code> -- fail first */
    private static final ConfigPropertyString confRepositoryFactoryClass = new ConfigPropertyString(
            "repository.factory", null);

    private static RRepositoryFactory repFactory;

    public static synchronized RProductionRepository getRepositoryInstance() throws RRepositoryException {
        if (repFactory == null) {
            initFactory();
        }

        return (RProductionRepository) repFactory.getRepositoryInstance();
    }

    private static synchronized void initFactory() throws RRepositoryException {
        ConfigSet confSet = SysConfigManager.getConfigManager().locate(PROP_FILE);
        confSet.updateProperty(confRepositoryFactoryClass);

        String className = confRepositoryFactoryClass.getValue();
        // TODO: check that className is not null otherwise throw meaningful
        // exception
        try {
            Class c = Class.forName(className);
            Object obj = c.newInstance();
            repFactory = (RRepositoryFactory) obj;
            // initialize
            repFactory.initialize(confSet);
        } catch (Exception e) {
            String msg = "Failed to initialize ProductionRepositoryFactory!";
            log.error(msg, e);
            throw new RRepositoryException(msg, e);
        }
    }

    public static synchronized void release() throws RRepositoryException {
        if (repFactory != null) {
            repFactory.release();
            repFactory = null;
        }
    }

    public static synchronized void reset() throws RRepositoryException {
        release();
        initFactory();
    }
}