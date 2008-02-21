package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Repository Factory Proxy.
 * <p>
 * Takes actual factory description from <i>rules-production.properties</i> file.
 *
 */
public class ProductionRepositoryFactoryProxy {
    public static final String PROP_FILE = "rules-production.properties";
    public static final String PROP_JCR_TYPE = "JCR.type";

    private static RRepositoryFactory repFactory;

    static {
        try {
            initFactory();
        } catch (RRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized RProductionRepository getRepositoryInstance() throws RRepositoryException {
        return (RProductionRepository) repFactory.getRepositoryInstance();
    }

    public static synchronized void reset() throws RRepositoryException {
        release();
        initFactory();
    }

    public static synchronized void release() throws RRepositoryException {
        if (repFactory != null) {
            repFactory.release();
        }
    }


    private static synchronized void initFactory() throws RRepositoryException {
        SmartProps props = new SmartProps(PROP_FILE);

        String className = props.getStr(PROP_JCR_TYPE);
        try {
            Class c = Class.forName(className);
            Object obj = c.newInstance();
            repFactory = (RRepositoryFactory) obj;
            // initialize
            repFactory.initialize(props);
        } catch (Exception e) {
            throw new RRepositoryException("Failed to initialize Factory: " + e.getMessage(), e);
        }
    }
}