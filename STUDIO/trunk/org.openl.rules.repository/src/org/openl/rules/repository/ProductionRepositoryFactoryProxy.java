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

    public static final String DEFAULT_PROP_FILE = "rules-production.properties";
    /** default value is <code>null</code> -- fail first */
    private static final ConfigPropertyString confRepositoryFactoryClass = new ConfigPropertyString(
            "production-repository.factory", null);

    private static RRepositoryFactory repFactory;

    private static ConfigSet config;

    private static Object flag = new Object();
    
    public static RProductionRepository getRepositoryInstance() throws RRepositoryException {
        if (repFactory == null) {
        	synchronized (flag) {
        		if (repFactory == null){
        			initFactory();	
        		}
			}
        }

        return (RProductionRepository) repFactory.getRepositoryInstance();
    }

    private static void initFactory() throws RRepositoryException {
        if (config == null) {
            config = SysConfigManager.getConfigManager().locate(DEFAULT_PROP_FILE);
        }
        config.updateProperty(confRepositoryFactoryClass);

        String className = confRepositoryFactoryClass.getValue();
        // TODO: check that className is not null otherwise throw meaningful
        // exception
        try {
            Class<?> c = Class.forName(className);
            Object obj = c.newInstance();
            repFactory = (RRepositoryFactory) obj;
            // initialize
            repFactory.initialize(config);
        } catch (Exception e) {
            String msg = "Failed to initialize ProductionRepositoryFactory!";
            log.error(msg, e);
            throw new RRepositoryException(msg, e);
        }
    }

    public static void release() throws RRepositoryException {
    	if (repFactory != null) {
			synchronized (flag) {
		        if (repFactory != null) {
		            repFactory.release();
		            repFactory = null;
		        }
			}
    	}
    }

    public static void reset() throws RRepositoryException {
    	synchronized (flag) {
            release();
            initFactory();
		}
    }

    /**
     * @deprecated
     */
    public static ConfigSet getConfig() {
        return config;
    }

    /**
     * @deprecated
     */
    public static void setConfig(ConfigSet config) {
        ProductionRepositoryFactoryProxy.config = config;
    }

}