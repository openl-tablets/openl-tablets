package org.openl.rules.webstudio.web.admin;

import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.LoginException;
import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.util.StringUtils;

public final class RepositoryValidators {
    private static final Pattern PROHIBITED_CHARACTERS = Pattern.compile("[\\p{Punct}]+");

    private RepositoryValidators() {
    }

    /**
     * Same as {@link #validate(RepositoryConfiguration, java.util.List)} but don't check for name uniqueness (for example for Design repository).
     *
     * @param config Repository configuration
     * @throws RepositoryValidationException if repository was configured incorrectly
     */
    public static void validate(RepositoryConfiguration config) throws RepositoryValidationException {
        validate(config, Collections.<RepositoryConfiguration>emptyList());
    }

    /**
     * Check that name, path are configured correctly and repository name doesn't have duplicates.
     *
     * @param prodConfig Repository configuration
     * @param productionRepositoryConfigurations list of all production configurations
     * @throws RepositoryValidationException if repository was configured incorrectly
     */
    public static void validate(RepositoryConfiguration prodConfig,
            List<RepositoryConfiguration> productionRepositoryConfigurations) throws RepositoryValidationException {
        if (StringUtils.isEmpty(prodConfig.getName())) {
            String msg = "Repository name is empty. Please, enter repository name";
            throw new RepositoryValidationException(msg);
        }
        if (StringUtils.isEmpty(prodConfig.getPath())) {
            String msg = "Repository path is empty. Please, enter repository path";
            throw new RepositoryValidationException(msg);
        }

        if (PROHIBITED_CHARACTERS.matcher(prodConfig.getName()).find()) {
            String msg = String.format(
                    "Repository name '%s' contains illegal characters. Please, correct repository name",
                    prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }

        // workingDirValidator(prodConfig.getPath(),
        // "Production Repository directory");

        // Check for name uniqueness.
        for (RepositoryConfiguration other : productionRepositoryConfigurations) {
            if (other != prodConfig) {
                if (prodConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists. Please, insert a new one",
                            prodConfig.getName());
                    throw new RepositoryValidationException(msg);
                }

                if (prodConfig.getPath().equals(other.getPath())) {
                    String msg = String.format("Repository path '%s' already exists. Please, insert a new one",
                            prodConfig.getPath());
                    throw new RepositoryValidationException(msg);
                }
            }
        }
    }

    public static void validateConnectionForDesignRepository(RepositoryConfiguration repoConfig, DesignTimeRepository designTimeRepository) throws RepositoryValidationException {
        try {
            DesignTimeRepositoryImpl dtr = (DesignTimeRepositoryImpl) designTimeRepository;
            // Close connection to jcr before checking connection
            dtr.destroy();
            RRepositoryFactory rulesRepositoryInstance = dtr.createConnection(repoConfig.getProperties());
            // Close repo connection after validation
            rulesRepositoryInstance.release();
        } catch (Exception e) {
            Throwable resultException = ExceptionUtils.getRootCause(e);
            if (resultException == null) {
                resultException = e;
            }
            throw new RepositoryValidationException(resultException.getMessage(), resultException);
        }
    }

    public static void validateConnection(RepositoryConfiguration repoConfig,
            ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy) throws RepositoryValidationException {
        try {
            /**Close connection to jcr before checking connection*/
            productionRepositoryFactoryProxy.releaseRepository(repoConfig.getConfigName());
            RRepositoryFactory repoFactory = productionRepositoryFactoryProxy.getFactory(
                    repoConfig.getProperties());
            try {
                RRepository repository = repoFactory.getRepositoryInstance();
                /*Close repo connection after validation*/
                repository.release();
            } finally {
                // Release a factory to prevent memory leak
                repoFactory.release();
            }
        } catch (RRepositoryException e) {
            Throwable resultException = ExceptionUtils.getRootCause(e);
            if (resultException == null) {
                resultException = e;
            }

            if (resultException instanceof LoginException) {
                if (!repoConfig.isSecure()) {
                    throw new RepositoryValidationException("Repository \"" + repoConfig.getName()
                            + "\" : Connection is secure. Please, insert login and password");
                } else {
                    throw new RepositoryValidationException("Repository \"" + repoConfig.getName()
                            + "\" : Invalid login or password. Please, check login and password");
                }
            } else if (resultException instanceof FailedLoginException) {
                throw new RepositoryValidationException("Repository \"" + repoConfig.getName()
                        + "\" : Invalid login or password. Please, check login and password");
            } else if (resultException instanceof ConnectException) {
                throw new RepositoryValidationException("Connection refused. Please, check repository URL");
            }

            throw new RepositoryValidationException("Repository \"" + repoConfig.getName() + "\" : "
                    + resultException.getMessage());
        }
    }
}