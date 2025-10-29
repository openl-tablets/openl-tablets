package org.openl.rules.webstudio.web.admin;

import java.io.File;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ListIterator;
import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jgit.transport.URIish;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.StringUtils;

public final class RepositoryValidators {

    private RepositoryValidators() {
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
            validateGitUri(repoConfig, repositoryConfigurations);
        }

        RepositorySettings settings = repoConfig.getSettings();

        if (settings instanceof CommonRepositorySettings) {
            validateCommonRepository(repoConfig, repositoryConfigurations);
        }

    }

    private static void validateGitUri(RepositoryConfiguration repoConfig, List<RepositoryConfiguration> repositoryConfigurations) throws RepositoryValidationException {
        String uri = ((GitRepositorySettings) repoConfig.getSettings()).getUri();
        URIish urIish;
        try {
            urIish = new URIish(uri);
        } catch (URISyntaxException e) {
            String msg = String
                    .format("Repository URI '%s' is incorrect.", uri);
            throw new RepositoryValidationException(msg);
        }
        if (! urIish.isRemote()) {
            if (urIish.getScheme() != null) {
                throw new RepositoryValidationException("Schemes are not allowed in local repositories");
            }
            Path path = normalizeLocalPath(uri);
            for (RepositoryConfiguration other : repositoryConfigurations) {
                if (other != repoConfig 
                        && hasTheSameGitLocalPath(other, path)) {
                        String msg = String
                                .format("Repository local path '%s' already exists. Please, insert a new one.", uri);
                        throw new RepositoryValidationException(msg);
                    }

            }
        }
    }

    private static boolean hasTheSameGitLocalPath(RepositoryConfiguration configuration, Path path) {
        if (! RepositoryType.GIT.equals(configuration.getRepositoryType())) {
            return false;
        }
        String otherUri = ((GitRepositorySettings) configuration.getSettings()).getUri();
        URIish otherUriish;
        try {
            otherUriish = new URIish(otherUri);
        } catch (URISyntaxException e) {
            //Misconfigured, but doesn't affect validation of current repo, so skip
            return false;
        }
        if (!otherUriish.isRemote() && otherUriish.getScheme() == null) {
            Path otherPath = normalizeLocalPath(otherUri);
            if (File.separatorChar == '\\') {
                //We don't allow quotes in URI, therefore for Windows systems we need to ignore case
                return path.toString().equalsIgnoreCase(otherPath.toString());
            } else {
                return path.equals(otherPath);
            }
        } else {
            return false;
        }
    }

    private static Path normalizeLocalPath(String raw) {
        String pathStr;
        pathStr = raw;

        // Windows: handle forms like "/C:/repo"
        if (File.separatorChar == '\\') {
            pathStr = pathStr.replace('/', '\\')
                    .replaceFirst("^\\\\+([A-Za-z]:\\\\)", "$1"); 
        }

        Path p = Paths.get(pathStr).toAbsolutePath().normalize();

        // Trim trailing separators (except root)
        String s = p.toString().replaceAll("[/\\\\]+$", "");
        if (s.isEmpty()) {
            s = p.toString();
        }

        return Paths.get(s);
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
        try {
            var propertiesToValidate = repoConfig.getPropertiesToValidate();
            var prefix = Comments.REPOSITORY_PREFIX + repoConfig.getConfigName();
            try (var repository = RepositoryInstatiator.newRepository(prefix, propertiesToValidate::getProperty)) {
                // Validate instantiation
                repository.validateConnection();
            }
        } catch (Exception e) {
            throw new RepositoryValidationException(
                    String.format("Repository '%s' : %s", repoConfig.getName(), getMostSpecificMessage(e)),
                    e);
        }
    }

    static String getMostSpecificMessage(Exception e) {
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
