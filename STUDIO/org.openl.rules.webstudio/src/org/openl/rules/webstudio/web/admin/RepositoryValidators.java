package org.openl.rules.webstudio.web.admin;

import java.io.Closeable;
import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.LoginException;
import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

public final class RepositoryValidators {
    private static final Pattern PROHIBITED_CHARACTERS = Pattern.compile("[\\p{Punct}]+");

    private RepositoryValidators() {
    }

    /**
     * Same as {@link #validate(RepositoryConfiguration, java.util.List)} but don't check for name uniqueness (for
     * example for Design repository).
     *
     * @param config Repository configuration
     * @throws RepositoryValidationException if repository was configured incorrectly
     */
    public static void validate(RepositoryConfiguration config) throws RepositoryValidationException {
        validate(config, Collections.<RepositoryConfiguration> emptyList());
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
        if (PROHIBITED_CHARACTERS.matcher(prodConfig.getName()).find()) {
            String msg = String.format(
                "Repository name '%s' contains illegal characters. Please, correct repository name",
                prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }

        // Check for name uniqueness.
        for (RepositoryConfiguration other : productionRepositoryConfigurations) {
            if (other != prodConfig) {
                if (prodConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists. Please, insert a new one",
                        prodConfig.getName());
                    throw new RepositoryValidationException(msg);
                }
            }
        }

        RepositorySettings settings = prodConfig.getSettings();

        if (settings instanceof CommonRepositorySettings) {
            validateCommonRepository(prodConfig, productionRepositoryConfigurations);
        }

    }

    private static void validateCommonRepository(RepositoryConfiguration prodConfig,
            List<RepositoryConfiguration> productionRepositoryConfigurations) throws RepositoryValidationException {
        CommonRepositorySettings settings = (CommonRepositorySettings) prodConfig.getSettings();
        String path = settings.getPath();
        if (StringUtils.isEmpty(path)) {
            String msg = "Repository path is empty. Please, enter repository path";
            throw new RepositoryValidationException(msg);
        }

        // Check for path uniqueness.
        for (RepositoryConfiguration other : productionRepositoryConfigurations) {
            if (other != prodConfig) {
                if (prodConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists. Please, insert a new one",
                        prodConfig.getName());
                    throw new RepositoryValidationException(msg);
                }

                if (other.getSettings() instanceof CommonRepositorySettings) {
                    CommonRepositorySettings otherSettings = (CommonRepositorySettings) other.getSettings();
                    if (path.equals(otherSettings.getPath()) && settings.isSecure() == otherSettings.isSecure()) {
                        // Different users can access different schemas
                        String login = settings.getLogin();
                        if (!settings.isSecure() || login != null && login.equals(otherSettings.getLogin())) {
                            String msg = String.format("Repository path '%s' already exists. Please, insert a new one",
                                path);
                            throw new RepositoryValidationException(msg);
                        }
                    }
                }
            }
        }
    }

    public static void validateConnectionForDesignRepository(RepositoryConfiguration repoConfig,
            DesignTimeRepository designTimeRepository,
            RepositoryMode repositoryMode) throws RepositoryValidationException {
        try {
            DesignTimeRepositoryImpl dtr = (DesignTimeRepositoryImpl) designTimeRepository;
            // Close connection to jcr before checking connection
            dtr.destroy();
            Repository repository = RepositoryFactoryInstatiator.newFactory(repoConfig.getProperties(), repositoryMode);
            if (repository instanceof Closeable) {
                // Close repo connection after validation
                IOUtils.closeQuietly((Closeable) repository);
            }
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
            /* Close connection to jcr before checking connection */
            productionRepositoryFactoryProxy.releaseRepository(repoConfig.getConfigName());
            Repository repository = RepositoryFactoryInstatiator.newFactory(repoConfig.getProperties(),
                RepositoryMode.PRODUCTION);
            if (repository instanceof Closeable) {
                // Close repo connection after validation
                IOUtils.closeQuietly((Closeable) repository);
            }
        } catch (RRepositoryException e) {
            Throwable resultException = ExceptionUtils.getRootCause(e);
            if (resultException == null) {
                resultException = e;
            }

            if (repoConfig.getSettings() instanceof CommonRepositorySettings) {
                CommonRepositorySettings settings = (CommonRepositorySettings) repoConfig.getSettings();

                if (resultException instanceof LoginException) {
                    if (!settings.isSecure()) {
                        throw new RepositoryValidationException("Repository \"" + repoConfig
                            .getName() + "\" : Connection is secure. Please, insert login and password");
                    } else {
                        throw new RepositoryValidationException("Repository \"" + repoConfig
                            .getName() + "\" : Invalid login or password. Please, check login and password");
                    }
                } else if (resultException instanceof FailedLoginException) {
                    throw new RepositoryValidationException("Repository \"" + repoConfig
                        .getName() + "\" : Invalid login or password. Please, check login and password");
                } else if (resultException instanceof ConnectException) {
                    throw new RepositoryValidationException("Connection refused. Please, check repository URL");
                }
            }

            throw new RepositoryValidationException(
                "Repository \"" + repoConfig.getName() + "\" : " + resultException.getMessage());
        }
    }
}