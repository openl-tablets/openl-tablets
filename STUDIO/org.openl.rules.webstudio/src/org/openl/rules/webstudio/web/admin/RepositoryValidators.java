package org.openl.rules.webstudio.web.admin;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.install.DelegatedPropertySource;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.util.StringUtils;

public final class RepositoryValidators {

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
     * @param repoConfig               Repository configuration
     * @param repositoryConfigurations list of all configurations
     * @throws RepositoryValidationException if repository was configured incorrectly
     */
    public static void validate(RepositoryConfiguration repoConfig,
                                List<RepositoryConfiguration> repositoryConfigurations) throws RepositoryValidationException {
        if (StringUtils.isEmpty(repoConfig.getName())) {
            String msg = "Repository name is empty. Please, enter repository name.";
            throw new RepositoryValidationException(msg);
        }
        if (!NameChecker.checkName(repoConfig.getName())) {
            String msg = String.format(
                    "Repository name '%s' contains illegal characters. Please, correct repository name.",
                    repoConfig.getName());
            throw new RepositoryValidationException(msg);
        }

        // Check for name uniqueness.
        for (RepositoryConfiguration other : repositoryConfigurations) {
            if (other != repoConfig) {
                if (repoConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists. Please, insert a new one.",
                            repoConfig.getName());
                    throw new RepositoryValidationException(msg);
                }
            }
        }

        // Check for path uniqueness. only for git
        if (RepositoryType.GIT.equals(repoConfig.getRepositoryType())) {
            Path path = Paths.get(((GitRepositorySettings) repoConfig.getSettings()).getLocalRepositoryPath());
            for (RepositoryConfiguration other : repositoryConfigurations) {
                if (other != repoConfig && RepositoryType.GIT.equals(other.getRepositoryType())) {
                    Path otherPath = Paths.get(((GitRepositorySettings) other.getSettings()).getLocalRepositoryPath());
                    if (path.equals(otherPath)) {
                        String msg = String
                                .format("Repository local path '%s' already exists. Please, insert a new one.", path);
                        throw new RepositoryValidationException(msg);
                    }
                }
            }
        }

        RepositorySettings settings = repoConfig.getSettings();

        if (settings instanceof CommonRepositorySettings) {
            validateCommonRepository(repoConfig, repositoryConfigurations);
        }

    }

    private static void validateCommonRepository(RepositoryConfiguration repoConfig,
                                                 List<RepositoryConfiguration> repositoryConfigurations) throws RepositoryValidationException {
        CommonRepositorySettings settings = (CommonRepositorySettings) repoConfig.getSettings();
        String path = settings.getUri();
        if (StringUtils.isEmpty(path)) {
            String msg = "Repository path is empty. Please, enter repository path.";
            throw new RepositoryValidationException(msg);
        }

        // Check for path uniqueness.
        for (RepositoryConfiguration other : repositoryConfigurations) {
            if (other != repoConfig) {
                if (repoConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists. Please, insert a new one.",
                            repoConfig.getName());
                    throw new RepositoryValidationException(msg);
                }

                if (other.getSettings() instanceof CommonRepositorySettings) {
                    CommonRepositorySettings otherSettings = (CommonRepositorySettings) other.getSettings();
                    if (path.equals(otherSettings.getUri()) && settings.isSecure() == otherSettings.isSecure()) {
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

    public static void validateConnection(RepositoryConfiguration repoConfig) throws RepositoryValidationException {
        validateConnection(repoConfig, null);
    }

    public static void validateConnection(RepositoryConfiguration repoConfig,
                                   RepositoryFactoryProxy repositoryFactoryProxy) throws RepositoryValidationException {
        try {
            if (repositoryFactoryProxy != null) {
                /* Close connection to repository before checking connection */
                repositoryFactoryProxy.releaseRepository(repoConfig.getConfigName());
            }

            validateInstantiation(repoConfig);
        } catch (Exception e) {
            throw new RepositoryValidationException(
                    String.format("Repository '%s' : %s", repoConfig.getName(), getMostSpecificMessage(e)),
                    e);
        }
    }

    private static void validateInstantiation(RepositoryConfiguration repoConfig) throws Exception {
        PropertyResolver propertiesResolver = DelegatedPropertySource
                .createPropertiesResolver(repoConfig.getPropertiesToValidate());
        try (Repository repository = RepositoryInstatiator
                .newRepository(Comments.REPOSITORY_PREFIX + repoConfig.getConfigName(), propertiesResolver::getProperty)) {
            // Validate instantiation
            repository.validateConnection();
        }
    }

    public static String getMostSpecificMessage(Exception e) {
        final List<Throwable> list = ExceptionUtils.getThrowableList(e);
        Throwable cause = list.isEmpty() ? null : list.get(list.size() - 1);
        if (cause == null) {
            cause = e;
        }

        // Check for common cases.
        if (cause instanceof FailedLoginException) {
            return "Invalid login or password. Try again.";
        } else if (cause instanceof ConnectException) {
            return "Connection refused. Check the repository URL and try again.";
        } else if (cause instanceof UnknownHostException) {
            final String message = cause.getMessage();
            return message != null ? String.format("Unknown host (%s).", message) : "Unknown host.";
        }

        // Obviously root cause gives more specific message. If we get empty message, we should consider wrapper
        // exception.
        ListIterator<Throwable> listIterator = list.listIterator(list.size());
        while (listIterator.hasPrevious()) {
            String message = listIterator.previous().getMessage();
            if (StringUtils.isNotBlank(message)) {
                return message;
            }
        }

        return e.getMessage();
    }
}
