package org.openl.rules.repository;

import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository Factory Proxy.
 * <p/>
 * Takes actual factory description from <i>rules-production.properties</i>
 * file.
 */
public class ProductionRepositoryFactoryProxy {

    public static final String DEFAULT_REPOSITORY_PROPERTIES_FILE = "rules-production.properties";
    public static final ConfigurationManagerFactory DEFAULT_CONFIGURATION_MANAGER_FACTORY = new ConfigurationManagerFactory(true, null, "");

    private ConfigurationManagerFactory configManagerFactory = DEFAULT_CONFIGURATION_MANAGER_FACTORY;

    /**
     * default value is <code>null</code> -- fail first
     */
    private final ConfigPropertyString confRepositoryFactoryClass = new ConfigPropertyString(
            "production-repository.factory", null);

    private Map<String, RRepositoryFactory> factories = new HashMap<String, RRepositoryFactory>();

    public Repository getRepositoryInstance(String propertiesFileName) throws RRepositoryException {
        if (!factories.containsKey(propertiesFileName)) {
            synchronized (this) {
                if (!factories.containsKey(propertiesFileName)) {
                    factories.put(propertiesFileName, createFactory(propertiesFileName));
                }
            }
        }

        return factories.get(propertiesFileName).getRepositoryInstance();
    }

    public void releaseRepository(String propertiesFileName) throws RRepositoryException {
        synchronized (this) {
            RRepositoryFactory factory = factories.get(propertiesFileName);
            if (factory != null) {
                factory.release();
                factories.remove(propertiesFileName);
            }
        }
    }

    public void destroy() throws RRepositoryException {
        synchronized (this) {
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
        ConfigurationManager configurationManager = configManagerFactory.getConfigurationManager(propertiesFileName);
        Map<String, Object> properties = configurationManager.getProperties();

        return getFactory(properties);
    }

    public RRepositoryFactory getFactory(Map<String, Object> props) throws RRepositoryException {
        ConfigSet config = new ConfigSet();
        config.addProperties(props);
        config.updateProperty(confRepositoryFactoryClass);
        String className = confRepositoryFactoryClass.getValue();

        return RepositoryFactoryInstatiator.newFactory(className, config, false);
    }
}