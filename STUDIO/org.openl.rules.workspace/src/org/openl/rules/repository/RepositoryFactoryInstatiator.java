package org.openl.rules.repository;

import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Factory class to instantiate repository factories by class name
 *
 * @author Yury Molchan
 */
public class RepositoryFactoryInstatiator {
    public static final String DESIGN_REPOSITORY = "repository.design";
    public static final String DEPLOY_CONFIG_REPOSITORY = "repository.deploy-config";
    public static final String PRODUCTION_REPOSITORY = "repository.production";

    private static Logger log() {
        return LoggerFactory.getLogger(RepositoryFactoryInstatiator.class);
    }

    /**
     * Create new instance of 'className' repository with defined configuration.
     */
    public static Repository newFactory(Environment environment, String configName) throws RRepositoryException {
        try {
            return RepositoryInstatiator.newRepository(configName.toLowerCase(), environment);
        } catch (Exception e) {
            String className = "";
            String message = "Failed to initialize repository: " + className;
            log().error(message, e);
            throw new RRepositoryException(message, e);
        }
    }
}
