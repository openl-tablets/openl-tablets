package org.openl.rules.repository.git;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.springframework.lang.NonNull;

import org.openl.util.HashingUtils;

class GitRootFactory {
    
    public GitRoot create(String repositoryId, String uri, String localRepositoriesFolder) throws IOException {
        URIish repositoryUri = getUri(uri);
        boolean remote = repositoryUri.isRemote();
        File localGitRoot;
        boolean empty;
        if (remote) {
            localGitRoot = findLocalGitRootForRemote(0, repositoryId, localRepositoriesFolder, uri);
            empty = ! localGitRoot.exists() || Objects.requireNonNull(localGitRoot.listFiles()).length == 0;
        } else {
            localGitRoot = new File(repositoryUri.getPath());
            if (localGitRoot.exists()) {
                File[] files = localGitRoot.listFiles();
                if (files == null) {
                    throw new IOException(String.format("'%s' is not a directory.", localGitRoot));
                }
                if (files.length > 0 && RepositoryCache.FileKey.resolve(localGitRoot, FS.DETECTED) == null) {
                    // Cannot overwrite existing files that is definitely not git repository
                    throw new IOException(String.format(
                            "Folder '%s' already exists and is not a git repository. Use another local path or delete the existing folder to create a git repository.",
                            localGitRoot));
                }
                empty = files.length == 0;
            } else {
                empty = true;
            }
        }
        return new GitRoot(remote, localGitRoot, empty);
    }

    @NonNull
    private File findLocalGitRootForRemote(int salt, String repositoryId, String localRepositoriesFolder, String uri) throws IOException {
        File candidate = generateLocalGitRoot(salt, repositoryId, localRepositoriesFolder, uri);
        if (candidate.exists()) {
            File[] files = candidate.listFiles();
            if (files == null) {
                return findLocalGitRootForRemote(salt + 1, repositoryId, localRepositoriesFolder, uri);
            }
            if (files.length > 0) {
                if (RepositoryCache.FileKey.resolve(candidate, FS.DETECTED) != null) {
                    return processExistingGitRepositoryForRemote(salt, repositoryId, localRepositoriesFolder, uri, candidate);
                } else {
                    return findLocalGitRootForRemote(salt + 1, repositoryId, localRepositoriesFolder, uri);
                }
            }
        }
        return candidate;
    }

    @NonNull
    private File processExistingGitRepositoryForRemote(int salt, String repositoryId, String localRepositoriesFolder, String uri, File candidate) throws IOException {
        try (Repository repository = Git.open(candidate).getRepository()) {
            String remoteUrl = repository.getConfig()
                    .getString(ConfigConstants.CONFIG_REMOTE_SECTION,
                            Constants.DEFAULT_REMOTE_NAME,
                            ConfigConstants.CONFIG_KEY_URL);
            if (remoteUrl == null) {
                return findLocalGitRootForRemote(salt + 1, repositoryId, localRepositoriesFolder, uri);
            }
            if (!uri.equals(remoteUrl)) {
                URIish proposedUri = getUri(uri);
                URIish savedUri = getUri(remoteUrl);
                if (!proposedUri.equals(savedUri)) {
                    if (isSame(proposedUri, savedUri)) {
                        StoredConfig config = repository.getConfig();
                        config.setString(ConfigConstants.CONFIG_REMOTE_SECTION,
                                Constants.DEFAULT_REMOTE_NAME,
                                ConfigConstants.CONFIG_KEY_URL,
                                uri);
                        config.save();
                    } else {
                        return findLocalGitRootForRemote(salt + 1, repositoryId, localRepositoriesFolder, uri);
                    }
                }
            }
            return candidate;
        }
    }

    @NonNull
    private File generateLocalGitRoot(int salt, String repositoryId, String localRepositoriesFolder, String uri) {
        File localPath = new File(localRepositoriesFolder);
        StringBuilder hashSource = new StringBuilder(repositoryId);
        char delimiter = ':';
        hashSource.append(delimiter);
        URIish uriObject = getUri(uri);
        uriObject = uriObject.setScheme(null); // Scheme shouldn't affect uniqueness of the URL
        String cleanedUri = uriObject.toASCIIString()
                .toLowerCase()
                .strip()
                .replaceAll("/$", "")
                .replace(":", "\\:");
        hashSource.append(cleanedUri);
        hashSource.append(delimiter);
        hashSource.append(salt);
        return new File(localPath, HashingUtils.sha256Hex(hashSource.toString()));
    }

    private URIish getUri(String uriOrPath) {
        try {
            return new URIish(uriOrPath);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(String.format("URI %s is misconfigured", uriOrPath), e);
        }
    }

    public static boolean isSame(URIish a, URIish b) {
        if (!Objects.equals(a.getRawPath(), b.getRawPath())
            && !Objects.equals(removeLastSlashes(a.getRawPath()), removeLastSlashes(b.getRawPath()))) {
            return false;
        }
        if (!Objects.equals(a.getUser(), b.getUser())) {
            return false;
        }
        if (!Objects.equals(a.getPass(), b.getPass())) {
            return false;
        }
        if (a.getHost() != null && !a.getHost().equalsIgnoreCase(b.getHost())) {
            return false;
        }
        return a.getPort() == b.getPort();
    }

    private static String removeLastSlashes(String s) {
        int i = s.length();
        while (i > 0 && s.charAt(i - 1) == '/') {
            i--;
        }
        if (i != s.length() && s.charAt(i) == '/') {
            return s.substring(0, i);
        } else {
            return s;
        }
    }
}
