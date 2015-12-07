package org.openl.rules.repository;

import org.openl.config.ConfigSet;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class to instantiate repository factories by class name
 * 
 * @author Yury Molchan
 */
public class RepositoryFactoryInstatiator {

    /**
     * Create new instance of 'className' repository with defined configuration.
     */
    public static RRepositoryFactory newFactory(String className, ConfigSet config) throws RRepositoryException {
        Logger log = LoggerFactory.getLogger(RepositoryFactoryInstatiator.class);
        String clazz = changeClassName(className);
        RRepositoryFactory repFactory;
        try {
            Class<?> c = Class.forName(clazz);
            Object obj = c.newInstance();
            repFactory = (RRepositoryFactory) obj;
            // initialize
            repFactory.initialize(config);
        } catch (Exception e) {
            String message = "Failed to initialize repository: " + className + " , like: " + clazz;
            log.error(message, e);
            throw new RRepositoryException(message, e);
        } catch (UnsupportedClassVersionError e) {
            String message = "Library was compiled using newer version of JDK";
            log.error(message, e);
            throw new RRepositoryException(message, e);
        }
        return repFactory;
    }

    // To support old factory names
    public static String changeClassName(String className) {
        return className.replace("ProductionRepositoryFactory", "RepositoryFactory")
            .replace("DesignRepositoryFactory", "RepositoryFactory")
            .replace("WebDavJackrabbit", "WebDav");
    }
}
