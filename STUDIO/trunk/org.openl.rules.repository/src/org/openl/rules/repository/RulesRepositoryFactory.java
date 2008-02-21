package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Repository Factory.  It is Abstract Factory.
 * <p>
 * Takes init values from repository.properties file.
 * 
 * @author Aleh Bykhavets
 *
 */
public class RulesRepositoryFactory {
    public static final String PROP_FILE = "rules-repository.properties";
    public static final String PROP_JCR_TYPE = "JCR.type";

    private static RRepositoryFactory repFactory;

    // TODO: add support for other types of concrete factories
    public static synchronized RRepository getRepositoryInstance() throws RRepositoryException {
        if (repFactory == null) {
            initFactory();
        }

        return repFactory.getRepositoryInstance();
    }

    private static void initFactory() throws RRepositoryException {
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
