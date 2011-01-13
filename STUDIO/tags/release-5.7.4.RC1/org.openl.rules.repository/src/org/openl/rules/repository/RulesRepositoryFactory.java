package org.openl.rules.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.config.SysConfigManager;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Repository Factory. It is Abstract Factory.
 * <p>
 * Takes init values from repository.properties file.
 *
 * @author Aleh Bykhavets
 *
 */
public class RulesRepositoryFactory {
    private static final Log log = LogFactory.getLog(RulesRepositoryFactory.class);

    public static final String PROP_FILE = "rules-repository.properties";

    public static final String MSG_FAILED = "Failed to initialize RulesRepositoryFactory!";

    /** default value is <code>null</code> -- fail first */
    private static final ConfigPropertyString confRepositoryFactoryClass = new ConfigPropertyString(
            "repository.factory", null);

    private static RRepositoryFactory repFactory;

    private static boolean isFailed;

    public static synchronized RRepository getRepositoryInstance() throws RRepositoryException {
        if (repFactory == null) {
            initFactory();
        }

        return repFactory.getRepositoryInstance();
    }

    public static RRepositoryFactory getRepFactory() {
        return repFactory;
    }

    private static void initFactory() throws RRepositoryException {
        ConfigSet confSet = SysConfigManager.getConfigManager().locate(PROP_FILE);
        if (confSet == null) {
            throw new RRepositoryException(MSG_FAILED, new NullPointerException());
        }

        confSet.updateProperty(confRepositoryFactoryClass);

        String className = confRepositoryFactoryClass.getValue();
        try {
            Class<?> c = Class.forName(className);
            Object obj = c.newInstance();
            repFactory = (RRepositoryFactory) obj;
            // initialize
            repFactory.initialize(confSet);
        } catch (Exception e) {
            isFailed = true;
            log.error(MSG_FAILED, e);
            throw new RRepositoryException(MSG_FAILED, e);
        }

        isFailed = false;
    }

    public static boolean isFailed() {
        if (isFailed == false && repFactory == null) {
            // first time, lets check
            try {
                initFactory();
            } catch (RRepositoryException e) {
                // ignore
                // isFailed = true;
            }
        }

        return isFailed;
    }
}
