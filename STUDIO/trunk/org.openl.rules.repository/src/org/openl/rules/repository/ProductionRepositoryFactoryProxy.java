package org.openl.rules.repository;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Repository Factory Proxy.
 * <p>
 * Takes actual factory description from <i>rules-production.properties</i>
 * file.
 *
 */
public class ProductionRepositoryFactoryProxy {
    private final Log log = LogFactory.getLog(ProductionRepositoryFactoryProxy.class);
    
    public static final String DEFAULT_REPOSITORY_PROPERTIES_FILE = "rules-production.properties";
    public static final ConfigurationManagerFactory DEFAULT_CONFIGURATION_MANAGER_FACTORY = new ConfigurationManagerFactory(false, null, "");

    private ConfigurationManagerFactory configManagerFactory = DEFAULT_CONFIGURATION_MANAGER_FACTORY;
    
    /** default value is <code>null</code> -- fail first */
    private final ConfigPropertyString confRepositoryFactoryClass = new ConfigPropertyString(
            "production-repository.factory", null);
    

    private Map<String, RRepositoryFactory> factories = new HashMap<String, RRepositoryFactory>();
    private Object flag = new Object();
    
    public RProductionRepository getRepositoryInstance(String propertiesFileName) throws RRepositoryException {
        if (!factories.containsKey(propertiesFileName)) {
            synchronized (flag) {
                if (!factories.containsKey(propertiesFileName)) {
                    factories.put(propertiesFileName, createFactory(propertiesFileName));
                }
            }
        }

        return (RProductionRepository) factories.get(propertiesFileName).getRepositoryInstance();
    }
    
    public void releaseRepository(String propertiesFileName) throws RRepositoryException {
        synchronized (flag) {
            RRepositoryFactory factory = factories.get(propertiesFileName);
            if (factory != null) {
                factory.release();
                factories.remove(propertiesFileName);
            }
        }
    }

    public void destroy() throws RRepositoryException {
        synchronized (flag) {
            for (RRepositoryFactory repFactory : factories.values()) {
                repFactory.release();
            }
            factories.clear();
        }
    }

    public void setConfigManagerFactory(ConfigurationManagerFactory configManagerFactory) {
        this.configManagerFactory = configManagerFactory;
    }

    private RRepositoryFactory createFactory(String propertiesFileName) throws RRepositoryException {
        ConfigSet config = new ConfigSet();
        ConfigurationManager configurationManager = configManagerFactory.getConfigurationManager(propertiesFileName);
        config.addProperties(configurationManager.getProperties());
        config.updateProperty(confRepositoryFactoryClass);

        String className = confRepositoryFactoryClass.getValue();
        // TODO: check that className is not null otherwise throw meaningful
        // exception
        RRepositoryFactory repFactory;
        try {
            Class<?> c = Class.forName(className);
            Object obj = c.newInstance();
            repFactory = (RRepositoryFactory) obj;
            // initialize
            repFactory.initialize(config);
            return repFactory;
        } catch (Exception e) {
            String msg = "Failed to initialize ProductionRepositoryFactory!";
            log.error(msg, e);
            throw new RRepositoryException(msg, e);
        }
    }
}