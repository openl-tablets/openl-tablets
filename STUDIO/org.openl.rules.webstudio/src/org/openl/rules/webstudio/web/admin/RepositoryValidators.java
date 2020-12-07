package org.openl.rules.webstudio.web.admin;

import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.install.DelegatedPropertySource;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

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
        validate(config, Collections.emptyList());
    }

    /**
     * Check that name, path are configured correctly and repository name does not have duplicates.
     *
     * @param prodConfig Repository configuration
     * @param productionRepositoryConfigurations list of all production configurations
     * @throws RepositoryValidationException if repository was configured incorrectly
     */
    public static void validate(RepositoryConfiguration prodConfig,
            List<RepositoryConfiguration> productionRepositoryConfigurations) throws RepositoryValidationException {
        if (StringUtils.isEmpty(prodConfig.getName())) {
            String msg = "Repository name is empty. Please, enter repository name.";
            throw new RepositoryValidationException(msg);
        }
        if (PROHIBITED_CHARACTERS.matcher(prodConfig.getName()).find()) {
            String msg = String.format(
                "Repository name '%s' contains illegal characters. Please, correct repository name.",
                prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }

        // Check for name uniqueness.
        for (RepositoryConfiguration other : productionRepositoryConfigurations) {
            if (other != prodConfig) {
                if (prodConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists. Please, insert a new one.",
                        prodConfig.getName());
                    throw new RepositoryValidationException(msg);
                }
            }
        }

        // Check for path uniqueness. only for git
        if (RepositoryType.GIT.equals(prodConfig.getRepositoryType())) {
            Path path = Paths.get(((GitRepositorySettings) prodConfig.getSettings()).getLocalRepositoryPath());
            for (RepositoryConfiguration other : productionRepositoryConfigurations) {
                if (other != prodConfig && RepositoryType.GIT.equals(other.getRepositoryType())) {
                    Path otherPath = Paths.get(((GitRepositorySettings) other.getSettings()).getLocalRepositoryPath());
                    if (path.equals(otherPath)) {
                        String msg = String.format(
                            "Repository local path '%s' already exists. Please, insert a new one.",
                            path.toString());
                        throw new RepositoryValidationException(msg);
                    }
                }
            }
        }

        List<String> names = new ArrayList<>();
        if (prodConfig.getType().equalsIgnoreCase("git")) {
            String localRepositoryPath = ((GitRepositorySettings) prodConfig.getSettings()).getLocalRepositoryPath();
            if (names.contains(localRepositoryPath)) {
                throw new RepositoryValidationException("ALARMA!!!");
            }
            names.add(localRepositoryPath);
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
            String msg = "Repository path is empty. Please, enter repository path.";
            throw new RepositoryValidationException(msg);
        }

        // Check for path uniqueness.
        for (RepositoryConfiguration other : productionRepositoryConfigurations) {
            if (other != prodConfig) {
                if (prodConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists. Please, insert a new one.",
                        prodConfig.getName());
                    throw new RepositoryValidationException(msg);
                }

                if (other.getSettings() instanceof CommonRepositorySettings) {
                    CommonRepositorySettings otherSettings = (CommonRepositorySettings) other.getSettings();
                    if (path.equals(otherSettings.getPath()) && settings.isSecure() == otherSettings.isSecure()) {
                        // Different users can access different schemas
                        String login = settings.getLogin();
                        if (!settings.isSecure() || login != null && login.equals(otherSettings.getLogin())) {
                            String msg = String.format("Repository path '%s' already exists. Please, insert a new one.",
                                path);
                            throw new RepositoryValidationException(msg);
                        }
                    }
                }
            }
        }
    }

    static void validateConnectionForDesignRepository(RepositoryConfiguration repoConfig,
            DesignTimeRepository designTimeRepository) throws RepositoryValidationException {
        try {
            DesignTimeRepositoryImpl dtr = (DesignTimeRepositoryImpl) designTimeRepository;
            // Close connection to repository before checking connection
            dtr.destroy();

            validateInstantiation(repoConfig);
        } catch (Exception e) {
            Throwable resultException = ExceptionUtils.getRootCause(e);
            if (resultException == null) {
                resultException = e;
            }
            throw new RepositoryValidationException(resultException.getMessage(), resultException);
        }
    }

    static void validateConnection(RepositoryConfiguration repoConfig,
            RepositoryFactoryProxy repositoryFactoryProxy) throws RepositoryValidationException {
        try {
            /* Close connection to repository before checking connection */
            repositoryFactoryProxy.releaseRepository(repoConfig.getConfigName());

            validateInstantiation(repoConfig);
        } catch (Exception e) {
            Throwable resultException = ExceptionUtils.getRootCause(e);
            if (resultException == null) {
                resultException = e;
            }

            if (repoConfig.getSettings() instanceof CommonRepositorySettings) {
                if (resultException instanceof FailedLoginException) {
                    throw new RepositoryValidationException(
                        String.format("Repository '%s' : Invalid login or password. Please, check login and password.",
                            repoConfig.getName()));
                } else if (resultException instanceof ConnectException) {
                    throw new RepositoryValidationException("Connection refused. Please, check repository URL.");
                }
            }

            throw new RepositoryValidationException(
                String.format("Repository '%s' : %s", repoConfig.getName(), resultException.getMessage()));
        }
    }

    public static void validateInstantiation(RepositoryConfiguration repoConfig) throws Exception {
        PropertyResolver propertiesResolver = DelegatedPropertySource
            .createPropertiesResolver(repoConfig.getPropertiesToValidate());
        try (Repository repository = RepositoryInstatiator.newRepository(Comments.REPOSITORY_PREFIX + repoConfig.getConfigName(), propertiesResolver::getProperty)) {
            // Validate instantiation
            Objects.requireNonNull(repository);
        }
    }
}