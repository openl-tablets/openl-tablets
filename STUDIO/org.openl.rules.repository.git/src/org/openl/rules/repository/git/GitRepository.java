package org.openl.rules.repository.git;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.hooks.CommitMsgHook;
import org.eclipse.jgit.hooks.PreCommitHook;
import org.eclipse.jgit.hooks.PrePushHook;
import org.eclipse.jgit.internal.storage.file.LockFile;
import org.eclipse.jgit.lfs.BuiltinLFS;
import org.eclipse.jgit.lfs.LfsBlobFilter;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeMessageFormatter;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.AuthorRevFilter;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;
import org.eclipse.jgit.revwalk.filter.OrRevFilter;
import org.eclipse.jgit.revwalk.filter.PatternMatchRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.revwalk.filter.SubStringRevFilter;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.LfsFactory;
import org.eclipse.jgit.util.RawCharSequence;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.dataformat.yaml.YamlMapperFactory;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.api.RepositorySettingsAware;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.rules.repository.common.RevisionGetter;
import org.openl.rules.repository.git.branch.BranchDescription;
import org.openl.rules.repository.git.branch.BranchesData;
import org.openl.rules.xls.merge.XlsWorkbookMerger;
import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.WorkbookDiffResult;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

public class GitRepository implements BranchRepository, RepositorySettingsAware, Closeable {
    static final String DELETED_MARKER_FILE = ".archived";

    private final Logger log = LoggerFactory.getLogger(GitRepository.class);

    private String id;
    private String name;
    private String uri;
    private String login;
    private String password;
    private String localRepositoryPath;
    private String branch = Constants.MASTER;
    private String baseBranch = branch;
    private String tagPrefix = StringUtils.EMPTY;
    private int listenerTimerPeriod = 10;
    private int connectionTimeout = 60;
    private String commentTemplate;
    private String escapedCommentTemplate;
    private CommitMessageParser commitMessageParser;
    private String commentTemplateOld;
    private CommitMessageParser commitMessageParserOld;
    private RepositorySettings repositorySettings;
    private Date settingsSyncDate = new Date();
    private volatile boolean noVerify;
    private Boolean gcAutoDetach;
    private int failedAuthenticationSeconds;
    private Integer maxAuthenticationAttempts;
    private WildcardBranchNameFilter protectedBranchFilter = WildcardBranchNameFilter.NO_MATCH;

    private boolean useLFS = false;

    private ChangesMonitor monitor;
    private volatile Git git;
    private NotResettableCredentialsProvider credentialsProvider;

    private ReadWriteLock repositoryLock = new ReentrantReadWriteLock();
    private ReentrantLock remoteRepoLock = new ReentrantLock();

    private BranchesData branches = new BranchesData();

    private boolean closed;
    private String branchesConfigFile = "design/branches.yaml";
    private final YAMLMapper mapper = YamlMapperFactory.getYamlMapper();

    public void setId(String id) {
        this.id = id;
        if (id != null) {
            branchesConfigFile = id + "/branches.yaml";
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBaseBranch() {
        return baseBranch;
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        initializeGit(true);

        if (isEmpty()) {
            return Collections.emptyList();
        }
        ObjectId objectId = resolveBranchId();
        if (objectId == null) {
            return Collections.emptyList();
        }
        return iterate(path, new ListCommand(objectId));
    }

    @Override
    public FileData check(String name) throws IOException {
        initializeGit(true);

        return iterate(name, new CheckCommand());
    }

    @Override
    public FileItem read(String name) throws IOException {
        initializeGit(true);

        return iterate(name, new ReadCommand());
    }

    @Override
    @SuppressWarnings("squid:S2095") // resources are closed by IOUtils
    public FileData save(FileData data, InputStream stream) throws IOException {
        initializeGit(true);

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("save(data, stream): lock");
            writeLock.lock();
            initLfsCredentials();

            saveSingleFile(data, stream);
        } catch (IOException e) {
            reset();
            throw e;
        } catch (Exception e) {
            reset();
            throw new IOException(e.getMessage(), e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("save(data, stream): unlock");
        }

        monitor.fireOnChange();

        return check(data.getName());
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        initializeGit(true);

        List<FileData> result = new ArrayList<>();
        Lock writeLock = repositoryLock.writeLock();
        String firstCommitId = null;
        try {
            log.debug("save(multipleFiles): lock");
            writeLock.lock();
            initLfsCredentials();

            checkoutForcedOrReset(branch);

            for (FileItem fileItem : fileItems) {
                RevCommit commit = createCommit(fileItem.getData(), fileItem.getStream());
                if (firstCommitId == null) {
                    firstCommitId = commit.getId().getName();
                }

                resolveAndMerge(fileItem.getData(), false, commit);
                addTagToCommit(commit, firstCommitId, fileItem.getData().getAuthor());
            }
            push();
        } catch (IOException e) {
            reset(firstCommitId);
            throw e;
        } catch (Exception e) {
            reset(firstCommitId);
            throw new IOException(e.getMessage(), e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("save(multipleFiles): unlock");
        }
        monitor.fireOnChange();

        for (FileItem fileItem : fileItems) {
            result.add(check(fileItem.getData().getName()));
        }
        return result;
    }

    private void saveSingleFile(FileData data, InputStream stream) throws IOException {
        String commitId = null;
        try {
            String parentVersion = data.getVersion();
            boolean checkoutOldVersion = isCheckoutOldVersion(data.getName(), parentVersion);
            checkoutForcedOrReset(checkoutOldVersion ? parentVersion : branch);
            RevCommit commit = createCommit(data, stream);
            commitId = commit.getId().getName();

            resolveAndMerge(data, checkoutOldVersion, commit);
            addTagToCommit(commit, data.getAuthor());

            push();
        } catch (IOException e) {
            reset(commitId);
            throw e;
        } catch (Exception e) {
            reset(commitId);
            throw new IOException(e.getMessage(), e);
        }
    }

    private RevCommit createCommit(FileData data, InputStream stream) throws GitAPIException, IOException {
        String fileInRepository = data.getName();

        File file = new File(localRepositoryPath, fileInRepository);
        createParent(file);
        IOUtils.copyAndClose(stream, new FileOutputStream(file));

        git.add().addFilepattern(fileInRepository).call();
        return git.commit()
                .setMessage(formatComment(CommitType.SAVE, data))
                .setOnly(fileInRepository)
                .setNoVerify(noVerify)
                .setCommitter(data.getAuthor().getDisplayName(),
                        Optional.ofNullable(data.getAuthor().getEmail()).orElse(""))
                .call();
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        initializeGit(true);

        if (deleteInternal(data)) {
            monitor.fireOnChange();
            return true;
        }
        return false;
    }

    private boolean deleteInternal(FileData data) throws IOException {
        String commitId = null;

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("delete(): lock");
            writeLock.lock();
            initLfsCredentials();

            checkoutForcedOrReset(branch);

            String name = data.getName();
            File file = new File(localRepositoryPath, name);
            if (!file.exists()) {
                return false;
            }

            if (file.isDirectory()) {
                String commitMessage = formatComment(CommitType.ARCHIVE, data);

                // Create marker file if it absents and write current time
                try (DataOutputStream os = new DataOutputStream(
                        new FileOutputStream(new File(file, DELETED_MARKER_FILE)))) {
                    os.writeLong(System.currentTimeMillis());
                }

                String markerFile = name + "/" + DELETED_MARKER_FILE;
                git.add().addFilepattern(markerFile).call();
                RevCommit commit = git.commit()
                        .setMessage(commitMessage)
                        .setOnly(markerFile)
                        .setNoVerify(noVerify)
                        .setCommitter(data.getAuthor().getDisplayName(),
                                Optional.ofNullable(data.getAuthor().getEmail()).orElse(""))
                        .call();
                commitId = commit.getId().getName();

                addTagToCommit(commit, data.getAuthor());
            } else {
                // Files cannot be archived. Only folders.
                git.rm().addFilepattern(name).call();
                RevCommit commit = git.commit()
                        .setMessage(formatComment(CommitType.ERASE, data))
                        .setNoVerify(noVerify)
                        .setCommitter(data.getAuthor().getDisplayName(),
                                Optional.ofNullable(data.getAuthor().getEmail()).orElse(""))
                        .call();
                commitId = commit.getId().getName();

                addTagToCommit(commit, data.getAuthor());
            }

            push();

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            reset(commitId);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            reset(commitId);
            throw new IOException(e.getMessage(), e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("delete(): unlock");
        }

        return true;
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        initializeGit(true);

        boolean deleted = false;
        for (FileData f : data) {
            deleted |= deleteInternal(f);
        }
        if (deleted) {
            monitor.fireOnChange();
        }
        return deleted;
    }

    @SuppressWarnings("squid:S2095") // resources are closed by IOUtils
    private FileData copy(String srcName, FileData destData) throws IOException {
        String commitId = null;

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("copy(): lock");
            writeLock.lock();
            initLfsCredentials();

            checkoutForcedOrReset(branch);

            File src = new File(localRepositoryPath, srcName);
            File dest = new File(localRepositoryPath, destData.getName());
            IOUtils.copyAndClose(new FileInputStream(src), new FileOutputStream(dest));

            git.add().addFilepattern(destData.getName()).call();
            RevCommit commit = git.commit()
                    .setMessage(formatComment(CommitType.SAVE, destData))
                    .setNoVerify(noVerify)
                    .setCommitter(destData.getAuthor().getDisplayName(),
                            Optional.ofNullable(destData.getAuthor().getEmail()).orElse(""))
                    .call();
            commitId = commit.getId().getName();

            addTagToCommit(commit, destData.getAuthor());

            push();
        } catch (IOException e) {
            reset(commitId);
            throw e;
        } catch (Exception e) {
            reset(commitId);
            throw new IOException(e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("copy(): unlock");
        }

        monitor.fireOnChange();

        return check(destData.getName());
    }

    @Override
    public void setListener(Listener callback) {
        if (monitor != null) {
            monitor.setListener(callback);
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        initializeGit(true);

        return iterateHistory(name, new ListHistoryVisitor(), null, false, Pageable.unpaged());
    }

    @Override
    public List<FileData> listHistory(String name, String globalFilter, boolean techRevs, Pageable page) throws IOException {
        initializeGit(true);
        return iterateHistory(name, new ListHistoryVisitor(), globalFilter, techRevs, page);
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        initializeGit(true);

        return parseHistory(path, version, new ListFilesHistoryVisitor(version));
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        initializeGit(true);

        return parseHistory(name, version, new CheckHistoryVisitor(version));
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        initializeGit(true);

        return parseHistory(name, version, new ReadHistoryVisitor(version));
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        initializeGit(true);

        String name = data.getName();
        String version = data.getVersion();
        String commitId = null;

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("deleteHistory(): lock");
            writeLock.lock();
            initLfsCredentials();

            checkoutForcedOrReset(branch);

            RevCommit commit;
            if (version == null) {
                git.rm().addFilepattern(name).call();
                String commitMessage = formatComment(CommitType.ERASE, data);
                commit = git.commit()
                        .setMessage(commitMessage)
                        .setOnly(name)
                        .setNoVerify(noVerify)
                        .setCommitter(data.getAuthor().getDisplayName(),
                                Optional.ofNullable(data.getAuthor().getEmail()).orElse(""))
                        .call();
            } else {
                FileData fileData = checkHistory(name, version);
                if (fileData == null) {
                    return false;
                }

                if (!fileData.isDeleted()) {
                    // We can "delete" only archived versions. Other version cannot be deleted.
                    return false;
                }

                String markerFile = name + "/" + DELETED_MARKER_FILE;
                git.rm().addFilepattern(markerFile).call();
                String commitMessage = formatComment(CommitType.RESTORE, data);
                commit = git.commit()
                        .setMessage(commitMessage)
                        .setOnly(markerFile)
                        .setNoVerify(noVerify)
                        .setCommitter(data.getAuthor().getDisplayName(),
                                Optional.ofNullable(data.getAuthor().getEmail()).orElse(""))
                        .call();
            }

            commitId = commit.getId().getName();
            addTagToCommit(commit, data.getAuthor());

            push();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            reset(commitId);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            reset(commitId);
            throw new IOException(e.getMessage(), e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("deleteHistory(): unlock");
        }

        monitor.fireOnChange();
        return true;
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        initializeGit(true);

        if (version == null) {
            return copy(srcName, destData);
        }

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("copyHistory(): lock");
            writeLock.lock();
            initLfsCredentials();

            checkoutForcedOrReset(branch);

            File src = new File(localRepositoryPath, srcName);
            if (src.isDirectory()) {
                List<FileItem> files = new ArrayList<>();
                try {
                    List<FileData> fileData = listFiles(srcName + "/", version);
                    for (FileData data : fileData) {
                        String fileFrom = data.getName();
                        FileItem fileItem = readHistory(fileFrom, data.getVersion());
                        String fileTo = destData.getName() + fileFrom.substring(srcName.length());
                        files.add(new FileItem(fileTo, fileItem.getStream()));
                    }
                    saveMultipleFiles(destData, files, ChangesetType.FULL);
                } finally {
                    for (FileItem file : files) {
                        IOUtils.closeQuietly(file.getStream());
                    }
                }
            } else {
                FileItem fileItem = null;
                try {
                    fileItem = readHistory(srcName, version);

                    destData.setSize(fileItem.getData().getSize());

                    saveSingleFile(destData, fileItem.getStream());
                } finally {
                    if (fileItem != null) {
                        IOUtils.closeQuietly(fileItem.getStream());
                    }
                }
            }
        } catch (IOException e) {
            reset();
            throw e;
        } catch (Exception e) {
            reset();
            throw new IOException(e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("copyHistory(): unlock");
        }

        monitor.fireOnChange();
        return check(destData.getName());
    }

    @Override
    public void validateConnection() throws IOException {
        initializeGit(true);
        if (uri != null) {
            try {
                initLfsCredentials();
                git.fetch()
                        .setCredentialsProvider(credentialsProvider)
                        .setTimeout(connectionTimeout)
                        .setDryRun(true)
                        .call();
            } catch (GitAPIException e) {
                throw new IOException(e);
            } finally {
                resetLfsCredentials();
            }
        }
    }

    public void initialize() {
        initializeGit(false);

        monitor = new ChangesMonitor(new GitRevisionGetter(), listenerTimerPeriod);
    }

    private void initializeGit(boolean failOnError) {
        if (git != null) {
            return;
        }

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("initialize(): lock");
            writeLock.lock();

            if (git != null) {
                return;
            }

            if (StringUtils.isNotBlank(login) && StringUtils.isNotBlank(password)) {
                credentialsProvider = new NotResettableCredentialsProvider(login,
                        password,
                        name,
                        failedAuthenticationSeconds,
                        maxAuthenticationAttempts);
            }

            File local = new File(localRepositoryPath);

            // If LFS is enabled, we will use only built-in LFS.
            BuiltinLFS.register();

            boolean clonedOrCreated = cloneOrInit(local);

            git = Git.open(local);
            updateGitConfigs();

            // Track all remote branches as local branches
            trackRemoteBranches(git);

            // Check if we should skip hooks.
            detectCanRunHooks();

            if (!clonedOrCreated && uri != null) {
                try (Repository repository = Git.open(local).getRepository()) {
                    configureBuiltInLFS(repository);

                    initLfsCredentials();
                    FetchResult fetchResult = fetchAll();
                    doFastForward(fetchResult);
                    fastForwardNotMergedCommits(fetchResult);
                } finally {
                    resetLfsCredentials();
                }
            }

            readBranchesWithLock();

            if (credentialsProvider != null) {
                credentialsProvider.successAuthentication(GitActionType.INIT);
            }
            tryToUnlockIndex();
        } catch (Exception e) {
            if (git != null) {
                try {
                    git.close();
                } catch (Exception ignored) {
                }
                git = null;
            }
            if (failOnError) {
                throwClearException(e);
            } else {
                log.error(e.getMessage(), e);
            }
        } finally {
            writeLock.unlock();
            log.debug("initialize(): unlock");
        }
    }

    /**
     * NOTE: It will not work properly, If multiple Git instances will be configured to the same folder. We assume, that
     * only ONE git instance will be configured to the same repo folder at the same time! Because it will be deleted and
     * may result unexpected behaviour!
     */
    private void tryToUnlockIndex() {
        // try to unlock index if locked
        if (!LockFile.unlock(git.getRepository().getIndexFile())) {
            log.warn("Failed to unlock index for '{}' repository", name);
        }
    }

    private boolean cloneOrInit(File local) throws IOException, GitAPIException {
        boolean shouldCloneOrInit;
        boolean shouldUpdateOrigin = false;
        if (!local.exists()) {
            shouldCloneOrInit = true;
        } else {
            File[] files = local.listFiles();
            if (files == null) {
                throw new IOException(String.format("'%s' is not a directory.", local));
            }

            if (files.length > 0) {
                if (RepositoryCache.FileKey.resolve(local, FS.DETECTED) != null) {
                    log.debug("Reuse existing git repository {}", local);
                    try (Repository repository = Git.open(local).getRepository()) {
                        if (uri != null) {
                            String remoteUrl = repository.getConfig()
                                    .getString(ConfigConstants.CONFIG_REMOTE_SECTION,
                                            Constants.DEFAULT_REMOTE_NAME,
                                            ConfigConstants.CONFIG_KEY_URL);
                            if (!uri.equals(remoteUrl)) {
                                URI proposedUri = getUri(uri);
                                URI savedUri = getUri(remoteUrl);
                                if (!proposedUri.equals(savedUri)) {
                                    if (savedUri != null && isSame(proposedUri, savedUri)) {
                                        shouldUpdateOrigin = true;
                                    } else {
                                        throw new IOException(String.format(
                                                "Folder '%s' already contains local git repository, but is configured to different URI (%s).\nDelete it or choose another local path or set correct URL for repository.",
                                                local,
                                                remoteUrl));
                                    }
                                }
                            }
                        }
                    }
                    shouldCloneOrInit = false;
                } else {
                    // Cannot overwrite existing files that is definitely not git repository
                    throw new IOException(String.format(
                            "Folder '%s' already exists and is not a git repository. Use another local path or delete the existing folder to create a git repository.",
                            local));
                }
            } else {
                shouldCloneOrInit = true;
            }
        }

        if (shouldCloneOrInit) {
            try {
                if (uri != null) {
                    CloneCommand cloneCommand = Git.cloneRepository()
                            .setURI(uri)
                            .setDirectory(local)
                            .setBranch(branch)
                            .setNoCheckout(true)
                            .setCloneAllBranches(true);

                    CredentialsProvider credentialsProvider = getCredentialsProvider(GitActionType.CLONE);
                    if (credentialsProvider != null) {
                        cloneCommand.setCredentialsProvider(credentialsProvider);
                    }

                    Git cloned = cloneCommand.call();
                    successAuthentication(GitActionType.CLONE);

                    // After cloning without checkout we don't have HEAD and local branches. Need to create them.
                    trackRemoteBranches(cloned);

                    // Detect if our repository needs to use built-in LFS.
                    configureBuiltInLFS(cloned.getRepository());

                    try {
                        // Checkout after clone with LFS enabled.
                        initLfsCredentials();
                        cloned.checkout().setName(branch).setForced(true).call();
                    } finally {
                        resetLfsCredentials();
                    }

                    cloned.close();
                } else {
                    Git repo = Git.init().setDirectory(local).call();
                    repo.close();
                }
            } catch (Exception e) {
                FileUtils.deleteQuietly(local);
                throw e;
            } finally {
                resetLfsCredentials();
            }
        } else if (shouldUpdateOrigin) {
            try (Repository repository = Git.open(local).getRepository()) {
                StoredConfig config = repository.getConfig();
                config.setString(ConfigConstants.CONFIG_REMOTE_SECTION,
                        Constants.DEFAULT_REMOTE_NAME,
                        ConfigConstants.CONFIG_KEY_URL,
                        uri);
                config.save();
            }
        }
        return shouldCloneOrInit;
    }

    private void updateGitConfigs() throws IOException {
        StoredConfig config = git.getRepository().getConfig();

        if (gcAutoDetach != null) {
            config.setBoolean(ConfigConstants.CONFIG_GC_SECTION,
                    null,
                    ConfigConstants.CONFIG_KEY_AUTODETACH,
                    gcAutoDetach);
        }

        config.save();
    }

    private void trackRemoteBranches(Git git) throws GitAPIException {
        List<Ref> remoteBranches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
        TreeSet<String> localBranches = getAvailableBranches(git);
        String remotePrefix = Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/";
        for (Ref remoteBranch : remoteBranches) {
            if (remoteBranch.isSymbolic()) {
                log.debug("Skip the symbolic branch '{}'.", remoteBranch.getName());
                continue;
            }
            if (!remoteBranch.getName().startsWith(remotePrefix)) {
                log.warn("The branch {} will not be tracked", remoteBranch.getName());
                continue;
            }
            String branchName = remoteBranch.getName().substring(remotePrefix.length());
            try {
                if (!localBranches.contains(branchName)) {
                    createRemoteTrackingBranch(git, branchName);
                }
            } catch (RefAlreadyExistsException e) {
                // the error may appear on non-case sensitive OS
                log.warn(
                        "The branch '{}' will not be tracked because a branch with the same name already exists. Branches with the same name, but different capitalization do not work on non-case sensitive OS.",
                        remoteBranch.getName());
            }
        }
    }

    private void detectCanRunHooks() {
        noVerify = false;
        File hookDir = new File(git.getRepository().getDirectory(), Constants.HOOKS);
        File preCommitHook = new File(hookDir, PreCommitHook.NAME);
        File commitMsgHook = new File(hookDir, CommitMsgHook.NAME);
        if (!preCommitHook.isFile() && !commitMsgHook.isFile()) {
            log.debug("Hooks are absent");
            noVerify = true;
        } else {
            try {
                if (!Files.isExecutable(preCommitHook.toPath()) || !Files.isExecutable(commitMsgHook.toPath())) {
                    log.debug("Hook exists but not executable");
                    noVerify = true;
                }
            } catch (SecurityException e) {
                log.warn("Hook exists but there is no access to invoke the file.", e);
                noVerify = true;
            }
        }
    }

    private void throwClearException(Exception e) {
        Throwable cause = ExceptionUtils.getRootCause(e);
        if (cause == null) {
            cause = e;
        }

        // Unknown host
        if (cause instanceof UnknownHostException) {
            String error;
            final String message = cause.getMessage();
            if (message != null) {
                error = String.format("Unknown host (%s) for URL %s.", message, uri);
            } else {
                error = String.format("Unknown host for URL %s.", uri);
            }
            throw new IllegalArgumentException(error);
        } else if (cause instanceof NoRemoteRepositoryException) {
            throw new IllegalArgumentException(String.format("Remote repository \"%s\" does not exist.", uri));
        }

        if (e instanceof TransportException) {
            try {
                if ((new URIish(uri)).getScheme() == null) {
                    throw new IllegalStateException("Incorrect URL.");
                }
            } catch (URISyntaxException uriSyntaxException) {
                throw new IllegalStateException("Incorrect URL.");
            }
        }

        // Other cases
        throw new IllegalStateException("Failed to initialize a repository: " + e.getMessage(), e);
    }

    @Override
    public void close() {
        closed = true;

        if (monitor != null) {
            monitor.release();
            monitor = null;
        }
        if (git != null) {
            git.close();
            git = null;
        }
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setLocalRepositoryPath(String localRepositoryPath) {
        this.localRepositoryPath = localRepositoryPath;
    }

    public void setBranch(String branch) {
        this.branch = StringUtils.isBlank(branch) ? Constants.MASTER : branch;
        this.baseBranch = this.branch;
    }

    public void setTagPrefix(String tagPrefix) {
        this.tagPrefix = StringUtils.trimToEmpty(tagPrefix);
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setFailedAuthenticationSeconds(int failedAuthenticationSeconds) {
        this.failedAuthenticationSeconds = failedAuthenticationSeconds;
    }

    public void setMaxAuthenticationAttempts(Integer maxAuthenticationAttempts) {
        this.maxAuthenticationAttempts = maxAuthenticationAttempts;
    }

    public void setCommentTemplate(String commentTemplate) {
        this.commentTemplate = commentTemplate;
        String ct = commentTemplate.replace("{commit-type}", "{0}").replace("{user-message}", "{1}");
        this.escapedCommentTemplate = escapeCurlyBrackets(ct);
        this.commitMessageParser = new CommitMessageParser(commentTemplate);
    }

    public void setCommentTemplateOld(String commentTemplateOld) {
        this.commentTemplateOld = commentTemplateOld;
        this.commitMessageParserOld = new CommitMessageParser(commentTemplateOld);
    }

    @Override
    public void setRepositorySettings(RepositorySettings repositorySettings) {
        this.repositorySettings = repositorySettings;
    }

    public void setProtectedBranches(String... patterns) {
        this.protectedBranchFilter = WildcardBranchNameFilter.create(patterns);
    }

    @Override
    public boolean isBranchProtected(String branch) {
        return protectedBranchFilter.accept(branch);
    }

    /**
     * If null, don't modify "gc.autoDetach" state. Otherwise, save it as a git repository setting.
     */
    public void setGcAutoDetach(Boolean gcAutoDetach) {
        this.gcAutoDetach = gcAutoDetach;
    }

    private static TreeWalk buildTreeWalk(Repository repository,
                                          String path,
                                          RevTree tree) throws IOException {
        TreeWalk treeWalk;
        if (StringUtils.isEmpty(path)) {
            treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setPostOrderTraversal(false);
        } else {
            treeWalk = TreeWalk.forPath(repository, path, tree);
        }

        if (treeWalk == null) {
            throw new FileNotFoundException(
                    String.format("Missed expected path '%s' in tree '%s'.", path, tree.getName()));
        }
        return treeWalk;
    }

    private FileData createFileData(TreeWalk dirWalk, String baseFolder, ObjectId start) {
        String fullPath = baseFolder + dirWalk.getPathString();
        return new LazyFileData(branch,
                fullPath,
                this,
                start,
                getFileId(dirWalk),
                commitMessageParser,
                commitMessageParserOld);
    }

    private boolean isEmpty() throws IOException {
        Ref headRef = git.getRepository().exactRef(Constants.HEAD);
        return headRef == null || headRef.getObjectId() == null;
    }

    private ObjectId resolveBranchId() throws IOException {
        if (git.getRepository().findRef(branch) != null) {
            return git.getRepository().resolve(branch);
        }
        return null;
    }

    private FileData createFileData(TreeWalk dirWalk, RevCommit fileCommit) {
        String fullPath = dirWalk.getPathString();

        return new LazyFileData(branch,
                fullPath,
                this,
                fileCommit,
                getFileId(dirWalk),
                commitMessageParser,
                commitMessageParserOld);
    }

    private ObjectId getFileId(TreeWalk dirWalk) {
        int fileModeBits = dirWalk.getFileMode().getBits();
        ObjectId fileId = null;
        if ((fileModeBits & FileMode.TYPE_FILE) != 0) {
            fileId = dirWalk.getObjectId(0);
        }
        return fileId;
    }

    ObjectId getLastRevision() throws GitAPIException, IOException {
        FetchResult fetchResult = null;

        Lock readLock = repositoryLock.readLock();

        if (uri != null) {
            try {
                readLock.lock();
                initLfsCredentials();

                boolean remoteLocked = remoteRepoLock.tryLock();
                if (!remoteLocked) {
                    // Skip because is already fetching by other thread.
                    return null;
                }
                try {
                    fetchResult = fetchAll();
                } finally {
                    remoteRepoLock.unlock();
                }
            } finally {
                resetLfsCredentials();
                readLock.unlock();
            }
        }

        boolean branchesChanged = false;
        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("getLastRevision(): lock write");
            writeLock.lock();
            initLfsCredentials();

            if (fetchResult != null) {
                branchesChanged = doFastForward(fetchResult);
                fastForwardNotMergedCommits(fetchResult);
            }

            TreeSet<String> availableBranches = getAvailableBranches();

            BranchesData branches = getBranches(true);
            Set<String> projectBranches = branches.getDescriptions()
                    .stream()
                    .map(BranchDescription::getName)
                    .collect(Collectors.toCollection(HashSet::new));
            branches.getProjectBranches().values().forEach(projectBranches::addAll);

            List<String> branchesToRemove = new ArrayList<>();
            projectBranches.forEach(projectBranch -> {
                if (!availableBranches.contains(projectBranch)) {
                    branchesToRemove.add(projectBranch);
                }
            });

            if (!branchesToRemove.isEmpty()) {
                lockSettings();
                try {
                    for (String projectBranch : branchesToRemove) {
                        branches.removeBranch(null, projectBranch);
                    }
                    saveBranches();
                } finally {
                    unlockSettings();
                }
            }
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("getLastRevision(): unlock write");
        }

        if (branchesChanged) {
            monitor.fireOnChange();
        }

        try {
            log.debug("getLastRevision(): lock");
            readLock.lock();
            return git.getRepository().resolve(branch);
        } finally {
            readLock.unlock();
            log.debug("getLastRevision(): unlock");
        }
    }

    private BranchesData getBranches(boolean withLock) throws IOException {
        if (repositorySettings != null) {
            boolean modified = !repositorySettings.getSyncDate().equals(settingsSyncDate);
            if (!modified) {
                FileData fileData = repositorySettings.getRepository().check(branchesConfigFile);
                modified = fileData != null && settingsSyncDate.before(fileData.getModifiedAt());
            }
            if (modified) {
                if (withLock) {
                    readBranchesWithLock();
                } else {
                    readBranches();
                }
            }
        }
        return branches;
    }

    /**
     * @return true if need to force listener invocation. It can be if some branch was added or deleted.
     */
    private boolean doFastForward(FetchResult fetchResult) throws GitAPIException, IOException {
        boolean branchesChanged = false;
        for (TrackingRefUpdate refUpdate : fetchResult.getTrackingRefUpdates()) {
            RefUpdate.Result result = refUpdate.getResult();
            switch (result) {
                case FAST_FORWARD:
                    if (!isEmpty()) {
                        checkoutForced(refUpdate.getRemoteName());
                    }
                    if (!(Constants.R_HEADS + branch).equals(refUpdate.getRemoteName())) {
                        branchesChanged = true;
                    }
                    // It's assumed that we don't have unpushed commits at this point so there must be no additional
                    // merge
                    // while checking last revision. Accept only fast forwards.
                    git.merge()
                            .include(refUpdate.getNewObjectId())
                            .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
                            .call();
                    break;
                case REJECTED_CURRENT_BRANCH:
                    checkoutForced(baseBranch); // On the next fetch the branch probably will be deleted
                    break;
                case FORCED:
                    if (ObjectId.zeroId().equals(refUpdate.getNewObjectId())) {
                        String remoteName = refUpdate.getRemoteName();
                        if (remoteName.startsWith(Constants.R_HEADS)) {
                            // Delete the branch
                            String branchToDelete = Repository.shortenRefName(remoteName);
                            String currentBranch = Repository.shortenRefName(git.getRepository().getFullBranch());
                            if (branchToDelete.equals(currentBranch)) {
                                String branchToCheckout = git.lsRemote()
                                        .setCredentialsProvider(getCredentialsProvider(GitActionType.FETCH_ALL))
                                        .callAsMap()
                                        .get("HEAD")
                                        .getObjectId()
                                        .getName();
                                checkoutForced(branchToCheckout);
                            }
                            git.branchDelete().setBranchNames(branchToDelete).setForce(true).call();
                            branchesChanged = true;
                        }
                    }
                    break;
                case NEW:
                    if (ObjectId.zeroId().equals(refUpdate.getOldObjectId())) {
                        String remoteName = refUpdate.getRemoteName();
                        if (remoteName.startsWith(Constants.R_HEADS)) {
                            createRemoteTrackingBranch(git, Repository.shortenRefName(remoteName));
                            branchesChanged = true;
                        }
                    }
                    break;
                case REJECTED:
                    if (refUpdate.getRemoteName().startsWith(Constants.R_HEADS)) {
                        // Force update for branch
                        git.fetch()
                                .setCredentialsProvider(getCredentialsProvider(GitActionType.FETCH_ALL))
                                .setForceUpdate(true)
                                .setRefSpecs(refUpdate.getRemoteName() + ":" + refUpdate.getLocalName())
                                .call();

                        checkoutForced(refUpdate.getRemoteName());
                        // Reset local branch to match remote
                        git.reset().setMode(ResetCommand.ResetType.HARD)
                                .setRef(refUpdate.getLocalName())
                                .call();

                        if (!(Constants.R_HEADS + branch).equals(refUpdate.getRemoteName())) {
                            branchesChanged = true;
                        }
                    }
                    break;
                case NO_CHANGE:
                    // Do nothing
                    break;
                default:
                    log.warn("Unsupported type of fetch result type: {}", result);
                    break;
            }
        }

        return branchesChanged;
    }

    private void fastForwardNotMergedCommits(FetchResult fetchResult) throws IOException, GitAPIException {
        // Support the case when for some reason commits were fetched but not merged to current branch earlier.
        // In this case fetchResult.getTrackingRefUpdates() can be empty.
        // If everything is merged into current branch, this method does nothing.
        // Obviously this method is not needed. It's invoked only to fix unexpected errors during work with repository.
        Ref advertisedRef = fetchResult.getAdvertisedRef(Constants.R_HEADS + branch);
        Ref localRef = git.getRepository().findRef(branch);
        if (localRef != null && advertisedRef != null && !localRef.getObjectId().equals(advertisedRef.getObjectId())) {
            if (isMergedInto(advertisedRef.getObjectId(), localRef.getObjectId(), false)) {
                // We enter here only if we have inconsistent repository state. Need additional investigation if this
                // occurred. For example this can happen if we discarded locally some commit but the branch didn't move
                // to the desired new HEAD.
                log.warn(
                        "Advertised commit is already merged into current head in branch '{}'. Current HEAD: {}, advertised ref: {}",
                        branch,
                        localRef.getObjectId().name(),
                        advertisedRef.getObjectId().name());
            } else {
                // Typically this shouldn't occur. But if found such case, should fast-forward local repository and
                // write
                // warning for future investigation.
                log.warn(
                        "Found commits that are not fast forwarded in branch '{}'. Current HEAD: {}, advertised ref: {}",
                        branch,
                        localRef.getObjectId().name(),
                        advertisedRef.getObjectId().name());
                checkoutForced(branch);
                git.merge().include(advertisedRef).setFastForward(MergeCommand.FastForwardMode.FF_ONLY).call();
            }
        }
    }

    private void pull(String commitToRevert, UserInfo mergeAuthor) throws GitAPIException, IOException {
        if (uri == null) {
            return;
        }

        FetchResult fetchResult;
        try {
            remoteRepoLock.lock();
            fetchResult = fetchAll();
        } finally {
            remoteRepoLock.unlock();
        }

        try {
            Ref r = fetchResult.getAdvertisedRef(branch);
            if (r == null) {
                r = fetchResult.getAdvertisedRef(Constants.R_HEADS + branch);
            }
            if (r == null) {
                r = git.getRepository().findRef(branch);
            }

            if (r == null) {
                return;
            }

            String mergeMessage = getMergeMessage(r);
            MergeResult mergeResult = git.merge()
                    .include(r.getObjectId())
                    .setStrategy(MergeStrategy.RECURSIVE)
                    .setMessage(mergeMessage)
                    .setCommit(false)
                    .call();

            validateNonConflictingMerge(mergeResult);
            validateMergeConflict(mergeResult, true, branch, mergeAuthor);

            applyMergeCommit(mergeResult, mergeMessage, mergeAuthor);

        } catch (GitAPIException | IOException e) {
            reset(commitToRevert);
            throw e;
        } finally {
            try {
                doFastForward(fetchResult);
            } catch (Exception e) {
                // Don't override exception thrown in catch block.
                log.error(e.getMessage(), e);
            }
        }
    }

    private void applyMergeCommit(MergeResult mergeResult,
                                  String mergeMessage,
                                  UserInfo mergeAuthor) throws GitAPIException {
        if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.MERGED_NOT_COMMITTED)) {
            git.commit()
                    .setMessage(mergeMessage)
                    .setNoVerify(noVerify)
                    .setCommitter(mergeAuthor.getDisplayName(), Optional.ofNullable(mergeAuthor.getEmail()).orElse(""))
                    .call();
        }
    }

    private void validateNonConflictingMerge(MergeResult mergeResult) throws IOException {
        log.debug("Merge result: {}", mergeResult);
        MergeResult.MergeStatus status = mergeResult.getMergeStatus();
        if (!status.isSuccessful()) {
            if (status == MergeResult.MergeStatus.CONFLICTING) {
                // Conflicting merge result is processed in validateMergeConflict().
                return;
            }

            log.debug("Merge status: {}", mergeResult.getMergeStatus());
            throw new IOException("Cannot merge: " + StringUtils.trim(mergeResult.toString()));
        }
    }

    private void validateMergeConflict(MergeResult mergeResult,
                                       boolean theirToOur,
                                       String branchFrom,
                                       UserInfo userInfo) throws GitAPIException, IOException {
        if (mergeResult.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING) {
            ObjectId[] mergedCommits = mergeResult.getMergedCommits();
            Repository repository = git.getRepository();
            List<Ref> tags = git.tagList().call();

            String baseCommit = getVersionName(repository, tags, mergeResult.getBase());

            String ourCommit = null;
            String theirCommit = null;
            ObjectId ourId = null;
            ObjectId theirId = null;

            if (mergedCommits.length > 0) {
                String commit = getVersionName(repository, tags, mergedCommits[0]);
                if (theirToOur) {
                    ourId = mergedCommits[0];
                    ourCommit = commit;
                } else {
                    theirId = mergedCommits[0];
                    theirCommit = commit;
                }
            }
            if (mergedCommits.length > 1) {
                String commit = getVersionName(repository, tags, mergedCommits[1]);
                if (theirToOur) {
                    theirId = mergedCommits[1];
                    theirCommit = commit;
                } else {
                    ourId = mergedCommits[1];
                    ourCommit = commit;
                }
            }

            Set<String> conflictedFiles = mergeResult.getConflicts().keySet();
            Map<String, String> diffs = new HashMap<>();

            if (ourId != null && theirId != null) {
                AbstractTreeIterator ourTreeParser = prepareTreeParser(repository, ourId);
                AbstractTreeIterator theirTreeParser = prepareTreeParser(repository, theirId);

                List<DiffEntry> diff = git.diff()
                        .setOldTree(theirTreeParser)
                        .setNewTree(ourTreeParser)
                        .setPathFilter(PathFilterGroup.createFromStrings(conflictedFiles))
                        .call();

                Pattern oldPathPattern = Pattern.compile("(diff --git .+\\n.+--- \"a/).+?(\".*\\n@@.+)",
                        Pattern.DOTALL);
                Pattern newPathPattern = Pattern.compile("(diff --git .+\\n.+\\+\\+\\+ \"b/).+?(\".*\\n@@.+)",
                        Pattern.DOTALL);
                for (DiffEntry entry : diff) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try (DiffFormatter formatter = new DiffFormatter(outputStream)) {
                        formatter.setRepository(repository);
                        formatter.setQuotePaths(false);
                        formatter.format(entry);
                        String path = entry.getChangeType() == DiffEntry.ChangeType.DELETE ? entry.getOldPath()
                                : entry.getNewPath();
                        String comparison = outputStream.toString(StandardCharsets.UTF_8);

                        // JGit currently doesn't support switching off quoting symbols with code < 0x80, so we used
                        // decode paths ourselves.
                        Matcher oldPathMatcher = oldPathPattern.matcher(comparison);
                        if (oldPathMatcher.matches()) {
                            comparison = oldPathMatcher.replaceFirst("$1" + entry.getOldPath() + "$2");
                        }
                        Matcher newPathMatcher = newPathPattern.matcher(comparison);
                        if (newPathMatcher.matches()) {
                            comparison = newPathMatcher.replaceFirst("$1" + entry.getNewPath() + "$2");
                        }
                        diffs.put(path, comparison);
                    }
                }
            }

            Map<String, WorkbookDiffResult> toAutoResolve = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            boolean allCanAutoResolve = true;
            for (String conflictedFile : conflictedFiles) {
                if (!FileTypeHelper.isExcelFile(conflictedFile)) {
                    // skip non-excel resources
                    allCanAutoResolve = false;
                    continue;
                }
                FileItem baseConflictedFile = null;
                FileItem ourConflictedFile = null;
                FileItem theirConflictedFile = null;
                try {
                    baseConflictedFile = parseHistory0(conflictedFile, baseCommit, new ReadHistoryVisitor(baseCommit));
                    ourConflictedFile = parseHistory0(conflictedFile, ourCommit, new ReadHistoryVisitor(ourCommit));
                    theirConflictedFile = parseHistory0(conflictedFile,
                            theirCommit,
                            new ReadHistoryVisitor(theirCommit));
                    if (baseConflictedFile == null || ourConflictedFile == null || theirConflictedFile == null) {
                        allCanAutoResolve = false;
                        continue;
                    }
                    try (XlsWorkbookMerger workbookMerger = XlsWorkbookMerger.create(baseConflictedFile.getStream(),
                            ourConflictedFile.getStream(),
                            theirConflictedFile.getStream())) {
                        WorkbookDiffResult diffResult = workbookMerger.getDiffResult();
                        if (!diffResult.hasConflicts()) {
                            toAutoResolve.put(conflictedFile, diffResult);
                            diffs.remove(conflictedFile);
                        } else {
                            allCanAutoResolve = false;
                        }
                    }
                } catch (Exception e) {
                    throw new MergeConflictException(diffs, baseCommit, ourCommit, theirCommit, Collections.emptyMap());
                } finally {
                    IOUtils.closeQuietly(baseConflictedFile);
                    IOUtils.closeQuietly(ourConflictedFile);
                    IOUtils.closeQuietly(theirConflictedFile);
                }
            }

            if (!allCanAutoResolve) {
                throw new MergeConflictException(diffs, baseCommit, ourCommit, theirCommit, toAutoResolve);
            } else if (!toAutoResolve.isEmpty()) {
                String ourBranch;
                String theirBranch;
                if (theirToOur) {
                    ourBranch = branch;
                    theirBranch = branchFrom;
                } else {
                    ourBranch = branchFrom;
                    theirBranch = branch;
                }
                try {
                    ConflictResolveData conflictResolveData = autoResolveConflicts(toAutoResolve,
                            ourCommit,
                            ourBranch,
                            theirCommit,
                            theirBranch);
                    resolveConflict(mergeResult, conflictResolveData, userInfo);
                } catch (Exception e) {
                    throw new MergeConflictException(diffs, baseCommit, ourCommit, theirCommit, Collections.emptyMap());
                }
            }
        }
    }

    private ConflictResolveData autoResolveConflicts(Map<String, WorkbookDiffResult> toAutoResolve,
                                                     String ourCommit,
                                                     String ourBranch,
                                                     String theirCommit,
                                                     String theirBranch) throws IOException {
        List<FileItem> autoResolved = new ArrayList<>();
        StringBuilder sb = new StringBuilder("Merge commit with ").append(theirCommit)
                .append("\n\n Automatically resolved conflicts:");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BiConsumer<String, String> appendSheetMergeLog = (sheetName, branchName) -> {
            sb.append("\n\t\t").append(sheetName);
            if (branchName != null) {
                sb.append(" (").append(branchName).append(')');
            }
        };
        for (String conflictedFile : toAutoResolve.keySet()) {
            output.reset();
            sb.append("\n\t").append(conflictedFile);
            WorkbookDiffResult diffResult = toAutoResolve.get(conflictedFile);
            var sheetDiffResult = diffResult.getSheetDiffResult();
            FileItem ourConflictedFile = parseHistory0(conflictedFile, ourCommit, new ReadHistoryVisitor(ourCommit));
            for (String sheetName : sheetDiffResult.getDiffSheets(DiffStatus.OUR)) {
                appendSheetMergeLog.accept(sheetName, ourBranch);
            }

            FileItem theirConflictedFile = parseHistory0(conflictedFile,
                    theirCommit,
                    new ReadHistoryVisitor(theirCommit));
            for (String sheetName : sheetDiffResult.getDiffSheets(DiffStatus.THEIR)) {
                appendSheetMergeLog.accept(sheetName, theirBranch);
            }
            XlsWorkbookMerger.merge(ourConflictedFile.getStream(), theirConflictedFile.getStream(), diffResult, output);
            autoResolved.add(new FileItem(conflictedFile, new ByteArrayInputStream(output.toByteArray())));
        }
        return new ConflictResolveData(theirCommit, autoResolved, sb.toString());
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        // noinspection Duplicates
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }

    private FetchResult fetchAll() throws GitAPIException {
        FetchCommand fetchCommand = git.fetch();
        fetchCommand.setTagOpt(TagOpt.FETCH_TAGS);
        CredentialsProvider credentialsProvider = getCredentialsProvider(GitActionType.FETCH_ALL);
        if (credentialsProvider != null) {
            fetchCommand.setCredentialsProvider(credentialsProvider);
        }
        fetchCommand.setRefSpecs(new RefSpec().setSourceDestination(Constants.R_HEADS + "*",
                Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/*"));
        fetchCommand.setRemoveDeletedRefs(true);
        fetchCommand.setTimeout(connectionTimeout);
        FetchResult result = fetchCommand.call();
        successAuthentication(GitActionType.FETCH_ALL);
        return result;
    }

    private void push() throws GitAPIException, IOException {
        if (uri == null) {
            return;
        }

        try {
            remoteRepoLock.lock();
            PushCommand push;

            if (git.getRepository().findRef(branch) != null) {
                push = git.push().setPushTags().add(branch).setTimeout(connectionTimeout);
            } else if (git.getRepository().findRef(baseBranch) == null) {
                git.getRepository().updateRef(baseBranch);
                git.branchCreate().setName(baseBranch).setForce(true).call();
                push = git.push().setPushTags().add(baseBranch).setTimeout(connectionTimeout);
            } else {
                throw new IOException(String.format("Cannot find branch '%s'", branch));
            }

            CredentialsProvider credentialsProvider = getCredentialsProvider(GitActionType.PUSH);
            if (credentialsProvider != null) {
                push.setCredentialsProvider(credentialsProvider);
            }

            Iterable<PushResult> results = push.call();
            successAuthentication(GitActionType.PUSH);
            validatePushResults(results);
        } finally {
            remoteRepoLock.unlock();
        }
    }

    private void validatePushResults(Iterable<PushResult> results) throws IOException {
        for (PushResult result : results) {
            log.debug(result.getMessages());

            Collection<RemoteRefUpdate> remoteUpdates = result.getRemoteUpdates();
            for (RemoteRefUpdate remoteUpdate : remoteUpdates) {
                RemoteRefUpdate.Status status = remoteUpdate.getStatus();
                switch (status) {
                    case OK:
                    case UP_TO_DATE:
                    case NON_EXISTING:
                        // Successful operation. Continue.
                        break;
                    case REJECTED_NONFASTFORWARD:
                        throw new IOException(
                                "Remote ref update was rejected, as it would cause non fast-forward update.");
                    case REJECTED_NODELETE:
                        throw new IOException(
                                "Remote ref update was rejected, because remote side does not support/allow deleting refs.");
                    case REJECTED_REMOTE_CHANGED:
                        throw new IOException(
                                "Remote ref update was rejected, because old object id on remote repository wasn't the same as defined expected old object.");
                    case REJECTED_OTHER_REASON:
                        String message = remoteUpdate.getMessage();
                        if ("pre-receive hook declined".equals(message)) {
                            message = "Remote git server rejected your commit because of pre-receive hook. Details:\n" + result
                                    .getMessages();
                        }
                        throw new IOException(message);
                    case AWAITING_REPORT:
                        throw new IOException(
                                "Push process is awaiting update report from remote repository. This is a temporary state or state after critical error in push process.");
                    default:
                        throw new IOException(
                                "Push process returned with status " + status + " and message " + remoteUpdate
                                        .getMessage());
                }
            }
        }
    }

    private <T> T iterate(String path, WalkCommand<T> command) throws IOException {
        Lock readLock = repositoryLock.readLock();
        try {
            log.debug("iterate(): lock");
            readLock.lock();
            initLfsCredentials();

            Repository repository = git.getRepository();
            if (isEmpty()) {
                return command.apply(repository, null, path);
            }

            try (RevWalk walk = new RevWalk(repository)) {
                ObjectId branchId = resolveBranchId();
                if (branchId == null) {
                    return command.apply(repository, null, path);
                }
                RevCommit commit = walk.parseCommit(branchId);
                RevTree tree = commit.getTree();

                // Create TreeWalk for root folder
                try (TreeWalk rootWalk = buildTreeWalk(repository, path, tree)) {
                    return command.apply(repository, rootWalk, path);
                } catch (FileNotFoundException e) {
                    return command.apply(repository, null, path);
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            resetLfsCredentials();
            readLock.unlock();
            log.debug("iterate(): unlock");
        }
    }

    private <T> T iterateHistory(String name,
                                 HistoryVisitor<T> historyVisitor,
                                 String globalFilter,
                                 boolean techRevs,
                                 Pageable page) throws IOException {
        Lock readLock = repositoryLock.readLock();
        try {
            log.debug("iterateHistory(): lock");
            readLock.lock();
            initLfsCredentials();

            if (isEmpty()) {
                return historyVisitor.getResult();
            }

            // We cannot use git.log().addPath(path) because jgit has some issues for some scenarios when merging commits
            // so some history elements aren't shown. So we iterate all commits and filter them out ourselves.
            Iterator<RevCommit> iterator = git.log()
                    .add(resolveBranchId())
                    .setRevFilter(buildGlobalRevisionFilter(globalFilter))
                    .call()
                    .iterator();

            List<Ref> tags = git.tagList().call();

            Repository repository = git.getRepository();

            int totalProcessed = 0;
            int processed = 0;
            int skip = -1;
            int maxCount = Integer.MAX_VALUE;
            if (!page.isUnpaged()) {
                skip = page.getOffset();
                maxCount = page.getPageSize();
            }
            try (ObjectReader or = repository.newObjectReader()) {
                TreeWalk tw = createTreeWalk(or, name);

                while (iterator.hasNext() && processed < maxCount) {
                    RevCommit commit = iterator.next();
                    boolean hasChanges = hasChangesInPath(tw, commit, git);
                    if (!techRevs && !hasChanges) {
                        continue;
                    }
                    totalProcessed++;
                    if (totalProcessed <= skip) {
                        continue;
                    }
                    boolean stop = historyVisitor.visit(name, commit, getVersionName(repository, tags, commit));
                    historyVisitor.getLastVisited().setTechnicalRevision(!hasChanges);
                    processed++;
                    if (stop) {
                        break;
                    }
                }
            }

            return historyVisitor.getResult();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            resetLfsCredentials();
            readLock.unlock();
            log.debug("iterateHistory(): unlock");
        }
    }

    private RevFilter buildGlobalRevisionFilter(String globalFilter) {
        if (globalFilter == null || globalFilter.isBlank()) {
            return RevFilter.ALL;
        }
        globalFilter = safeEscapeFilter(globalFilter.trim());
        RevFilter idFilter;
        if (SubStringRevFilter.safe(globalFilter)) {
            idFilter = new SubStringIdRevFilter(globalFilter);
        } else {
            idFilter = new PatternIdRevFilter(globalFilter);
        }
        return OrRevFilter.create(
                Arrays.asList(AuthorRevFilter.create(globalFilter), MessageRevFilter.create(globalFilter), idFilter));
    }

    private String safeEscapeFilter(String globalFilter) {
        if (SubStringRevFilter.safe(globalFilter)) {
            // in this case JGit uses substring filter, so nothing to escape
            return globalFilter;
        }
        // in this case JGit uses Pattern filter, so let's validate if filter can be compiled, and escape it if not;
        try {
            var ignore = Pattern.compile(globalFilter).matcher("");
            return globalFilter;
        } catch (PatternSyntaxException e) {
            log.debug(e.getMessage(), e);
            return String.format("\\Q%s\\E", globalFilter);
        }
    }

    private <T> T parseHistory(String name, String version, HistoryVisitor<T> historyVisitor) throws IOException {
        Lock readLock = repositoryLock.readLock();
        try {
            log.debug("parseHistory(): lock");
            readLock.lock();
            initLfsCredentials();

            return parseHistory0(name, version, historyVisitor);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            resetLfsCredentials();
            readLock.unlock();
            log.debug("parseHistory(): unlock");
        }
    }

    private <T> T parseHistory0(String name, String version, HistoryVisitor<T> historyVisitor) throws IOException {
        try {
            List<Ref> tags = git.tagList().call();

            try (RevWalk walk = new RevWalk(git.getRepository())) {
                if (isEmpty()) {
                    return historyVisitor.getResult();
                }

                ObjectId id = getCommitByVersion(version);
                if (id != null) {
                    RevCommit commit = walk.parseCommit(id);
                    historyVisitor.visit(name, commit, getVersionName(git.getRepository(), tags, commit));
                } else {
                    log.warn("Cannot find commit for version {}", version);
                }
                return historyVisitor.getResult();
            }
        } catch (MissingObjectException e) {
            log.error("", e);
            return null;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Reset work dir and index.
     */
    private void reset() {
        reset(null);
    }

    /**
     * Reset work dir, index and discard commit. if {@code commitToDiscard} is null, then just reset work dir and index.
     *
     * @param commitToDiscard if null, commit will not be discarded. If not null, commit with that id will be discarded.
     */
    private void reset(String commitToDiscard) {
        try {
            String fullBranch = git.getRepository().getFullBranch();
            if (ObjectId.isId(fullBranch)) {
                // Detached HEAD. Just checkout to current branch and reset working dir.
                log.debug("Found detached HEAD: {}.", fullBranch);
                git.checkout().setName(branch).setForced(true).call();
            } else {
                ResetCommand resetCommand = git.reset().setMode(ResetCommand.ResetType.HARD);
                // If commit is not merged to our branch, it's detached - in this case no need to reset commit tree.
                if (commitToDiscard != null && isCommitMerged(commitToDiscard)) {
                    log.debug("Discard commit: {}.", commitToDiscard);
                    resetCommand.setRef(commitToDiscard + "^");
                }
                try {
                    resetCommand.call();
                } catch (JGitInternalException e) {
                    // check if index file is corrupted
                    File indexFile = git.getRepository().getIndexFile();
                    try {
                        DirCache dc = new DirCache(indexFile, git.getRepository().getFS());
                        dc.read();
                        log.error(e.getMessage(), e);
                    } catch (CorruptObjectException ex) {
                        log.error("git index file is corrupted and will be deleted", e);
                        if (!indexFile.delete() && indexFile.exists()) {
                            log.warn("Cannot delete corrupted index file {}.", indexFile);
                        }
                        resetCommand.call();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void resetToCommit(String refToResetTo) {
        try {
            if (refToResetTo != null) {
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(refToResetTo).call();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void checkoutForcedOrReset(String branch) throws IOException, GitAPIException {
        if (isEmpty()) {
            reset();
        } else {
            checkoutForced(branch);
        }
    }

    private void checkoutForced(String branch) throws GitAPIException {
        git.checkout().setName(branch).setForced(true).call();
    }

    private boolean isCommitMerged(String commitId) throws IOException {
        Repository repository = git.getRepository();
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit branchHead = revWalk.parseCommit(repository.resolve(Constants.R_HEADS + branch));
            RevCommit otherHead = revWalk.parseCommit(repository.resolve(commitId));
            return revWalk.isMergedInto(otherHead, branchHead);
        }
    }

    private String getNextTagId() throws GitAPIException {
        List<Ref> call = git.tagList().call();
        long maxId = 0;
        for (Ref tagRef : call) {
            String name = getLocalTagName(tagRef);
            if (name.startsWith(tagPrefix)) {
                int num;
                try {
                    num = Integer.parseInt(name.substring(tagPrefix.length()));
                } catch (NumberFormatException e) {
                    log.debug("Tag '{}' is skipped because it does not contain version number", name);
                    continue;
                }
                if (num > maxId) {
                    maxId = num;
                }
            }
        }

        return String.valueOf(maxId + 1);
    }

    static String getVersionName(Repository repository, List<Ref> tags, ObjectId commitId) throws IOException {
        Ref tagRef = getTagRefForCommit(repository, tags, commitId);

        return tagRef != null ? getLocalTagName(tagRef) : commitId.getName();
    }

    static RevCommit findFirstCommit(Git git, ObjectId startCommit, String path) throws IOException, GitAPIException {
        // We cannot use git.log().addPath(path) because jgit has some issues for some scenarios when merging commits so
        // some history elements aren't shown. So we iterate all commits and filter them out ourselves.
        Repository repository = git.getRepository();
        try (ObjectReader or = repository.newObjectReader()) {
            TreeWalk tw = createTreeWalk(or, path);
            for (RevCommit commit : git.log().add(startCommit).call()) {
                if (hasChangesInPath(tw, commit, git)) {
                    return commit;
                }
            }
        }

        return null;
    }

    private static boolean hasChangesInPath(TreeWalk tw, RevCommit commit, Git git) throws IOException,
            GitAPIException {
        Repository repository = git.getRepository();
        RevCommit[] parents = commit.getParents();
        int parentsNum = parents.length;

        tw.reset(getTreesToCompare(commit));

        Set<Integer> changes = new HashSet<>();

        while (tw.next()) {
            if (parentsNum == 0) {
                // Path is changed but there are no parents. It's a first commit.
                return true;
            }

            int currentMode = tw.getRawMode(parentsNum);
            for (int i = 0; i < parentsNum; i++) {
                int parentMode = tw.getRawMode(i);
                if (currentMode != parentMode || !tw.idEqual(i, parentsNum)) {
                    // Path configured in tw was changed
                    changes.add(i);
                }
            }
        }

        if (parentsNum == 0) {
            return false;
        } else if (parentsNum == 1) {
            return !changes.isEmpty();
        } else {
            if (changes.size() == parentsNum) {
                // Merge commit is modified comparing to both parents. Definitely we must show it in history.
                return true;
            }
            if (!changes.isEmpty()) {
                // Merge commit is same as one of the parents for inspecting path.
                // It can be in two cases:
                // 1) it's a merge commit with overwriting changes of a user (ours or theirs).
                // 2) merge commit doesn't introduce anything related to our path (merged changes are for other
                // paths not related to the path interesting to us).

                // Find a common parent for commits that were merged.
                // Then we compare it to each commit that changed the project in question.
                // If there is a difference between commits, then it should be displayed.
                try (RevWalk walk = new RevWalk(repository)) {
                    walk.setRevFilter(RevFilter.MERGE_BASE);

                    for (RevCommit parent : parents) {
                        RevCommit revCommit = walk.parseCommit(parent);
                        walk.markStart(revCommit);
                    }
                    RevCommit mergeBase = walk.next();

                    if (mergeBase != null) {
                        for (int i : changes) {
                            tw.reset(parents[i].getTree(), mergeBase.getTree());
                            if (tw.next() && !tw.idEqual(0, 1)) {
                                return true;
                            }
                        }

                        // Check if any commit from parent until merge base contains changes in the project.
                        // If contains (probably that commit was reverted eventually), then it will be shown in history,
                        // so we must show our merge commit because it contains the latest project state, and
                        // it differs from its parent.
                        for (int i : changes) {
                            Iterable<RevCommit> commits = git.log().addRange(mergeBase, parents[i]).call();
                            for (RevCommit prevParentCommit : commits) {
                                tw.reset(getTreesToCompare(prevParentCommit));
                                int prevParentCount = prevParentCommit.getParentCount();
                                int modified = 0;
                                for (int j = 0; j < prevParentCount; j++) {
                                    if (tw.next() && !tw.idEqual(j, prevParentCount)) {
                                        // Path configured in tw was changed
                                        modified++;
                                    }
                                }
                                if (modified > 0 && modified == prevParentCount) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            }
        }

        return false;
    }

    private static ObjectId[] getTreesToCompare(RevCommit commit) {
        RevCommit[] parents = commit.getParents();
        int parentsNum = parents.length;

        ObjectId[] trees = new ObjectId[parentsNum + 1];
        for (int i = 0; i < parentsNum; i++) {
            trees[i] = parents[i].getTree();
        }
        // The last tree is a tree for inspecting commit.
        trees[parentsNum] = commit.getTree();

        return trees;
    }

    private static TreeWalk createTreeWalk(ObjectReader or, String path) {
        TreeFilter t = AndTreeFilter.create(PathFilterGroup.create(Collections.singleton(PathFilter.create(path))),
                TreeFilter.ANY_DIFF);
        TreeWalk tw = new TreeWalk(or);
        tw.setFilter(t);
        tw.setRecursive(t.shouldBeRecursive());
        return tw;
    }

    private static Ref getTagRefForCommit(Repository repository, List<Ref> tags, ObjectId commitId) throws IOException {
        Ref tagRefForCommit = null;
        for (Ref tagRef : tags) {
            ObjectId objectId = repository.getRefDatabase().peel(tagRef).getPeeledObjectId();
            if (objectId == null) {
                objectId = tagRef.getObjectId();
            }

            if (objectId.equals(commitId)) {
                tagRefForCommit = tagRef;
                break;
            }
        }
        return tagRefForCommit;
    }

    private static String getLocalTagName(Ref tagRef) {
        String name = tagRef.getName();
        return name.startsWith(Constants.R_TAGS) ? name.substring(Constants.R_TAGS.length()) : name;
    }

    private void addTagToCommit(RevCommit commit, UserInfo mergeAuthor) throws GitAPIException, IOException {
        addTagToCommit(commit, commit.getId().getName(), mergeAuthor);
    }

    private void addTagToCommit(RevCommit commit, String commitToRevert, UserInfo mergeAuthor) throws GitAPIException,
            IOException {
        pull(commitToRevert, mergeAuthor);

        if (!tagPrefix.isEmpty()) {
            String tagName = tagPrefix + getNextTagId();
            git.tag().setObjectId(commit).setName(tagName).call();
        }
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        initializeGit(true);

        return iterate(path, new ListFoldersCommand());
    }

    @Override
    public FileData save(FileData folderData,
                         Iterable<FileItem> files,
                         ChangesetType changesetType) throws IOException {
        initializeGit(true);

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("save(folderData, files, changesetType): lock");
            writeLock.lock();
            initLfsCredentials();

            saveMultipleFiles(folderData, files, changesetType);
        } catch (IOException e) {
            reset();
            throw e;
        } catch (Exception e) {
            reset();
            throw new IOException(e.getMessage(), e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("save(folderData, files, changesetType): unlock");
        }

        monitor.fireOnChange();
        return check(folderData.getName());
    }

    @Override
    public void pull(UserInfo author) throws IOException {
        initializeGit(true);

        if (uri == null) {
            return;
        }

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("pull(author): lock");
            writeLock.lock();
            initLfsCredentials();

            checkoutForcedOrReset(branch);

            pull(null, author);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("pull(author): unlock");
        }

    }

    @Override
    public void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException {
        initializeGit(true);

        Lock writeLock = repositoryLock.writeLock();
        String refToResetTo = null;
        try {
            log.debug("merge(): lock");
            writeLock.lock();
            initLfsCredentials();

            checkoutForcedOrReset(branch);

            if (conflictResolveData == null) {
                pull(null, author);
            }
            refToResetTo = git.getRepository().findRef(branch).getObjectId().getName();

            Ref branchRef = git.getRepository().findRef(branchFrom);
            String mergeMessage = getMergeMessage(branchRef);
            MergeResult mergeResult = git.merge()
                    .include(branchRef)
                    .setCommit(false)
                    .setMessage(mergeMessage)
                    .setFastForward(MergeCommand.FastForwardMode.NO_FF)
                    .call();

            validateNonConflictingMerge(mergeResult);

            if (conflictResolveData != null) {
                resolveConflict(mergeResult, conflictResolveData, author);
            } else {
                validateMergeConflict(mergeResult, true, branchFrom, author);
                applyMergeCommit(mergeResult, mergeMessage, author);
            }

            pull(null, author);
            push();
        } catch (IOException e) {
            resetToCommit(refToResetTo);
            throw e;
        } catch (Exception e) {
            resetToCommit(refToResetTo);
            throw new IOException(e.getMessage(), e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("merge(): unlock");
        }

        monitor.fireOnChange();
    }

    @Override
    public boolean isMergedInto(String from, String to) throws IOException {
        initializeGit(true);

        Lock readLock = repositoryLock.readLock();
        try {
            log.debug("isMergedInto(): lock");
            readLock.lock();
            initLfsCredentials();
            Repository repository = git.getRepository();
            return isMergedInto(repository.resolve(from), repository.resolve(to), true);
        } finally {
            resetLfsCredentials();
            readLock.unlock();
            log.debug("isMergedInto(): unlock");
        }
    }

    /**
     * Checks if commit with id {@code fromId} is merged to commit with id {@code toId}.
     *
     * @param fromId           origin
     * @param toId             destination
     * @param skipEmptyChanges If false, it works as in git: Determine if a commit is reachable from another commit. If
     *                         true, commits with empty changes will be skipped. In the latter case if there are no valuable changes
     *                         in other branch (for example only merge commits gotten from our branch), this method returns true. If
     *                         you are interested in all unmerged commits (including empty ones), use "false".
     * @return true if commit {@code fromId} is merged to commit with id {@code toId} and false otherwise.
     * @throws IOException if any error is occurred during this method
     */
    private boolean isMergedInto(ObjectId fromId, ObjectId toId, boolean skipEmptyChanges) throws IOException {
        Repository repository = git.getRepository();

        try (RevWalk revWalk = new RevWalk(repository)) {
            if (fromId == null || toId == null) {
                return false;
            }
            RevCommit fromCommit = revWalk.parseCommit(fromId);
            RevCommit toCommit = revWalk.parseCommit(toId);
            boolean merged = revWalk.isMergedInto(fromCommit, toCommit);
            if (!merged && skipEmptyChanges) {
                if (fromCommit.getParentCount() == 2) {
                    // fromCommit is a merge commit
                    final RevCommit parent1 = fromCommit.getParent(0);
                    final RevCommit parent2 = fromCommit.getParent(1);

                    if (hasSameContent(parent1, fromCommit) || hasSameContent(parent2, fromCommit)) {
                        // Merge commit has same content as one of their parents
                        final boolean firstParentMerged = isMergedInto(parent1, toCommit, true);
                        final boolean secondParentMerged = isMergedInto(parent2, toCommit, true);
                        if (firstParentMerged && secondParentMerged) {
                            // If both parents are merged to our commit and one of the parents is same as child (merge
                            // commit), we can assume that the merge commit doesn't have any valuable updates.
                            // So we can assume that all valuable changes are merged.
                            return true;
                        }
                    }
                }
            }
            return merged;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private boolean hasSameContent(RevCommit commit1, RevCommit commit2) throws IOException {
        try (DiffFormatter diffFormatter = new DiffFormatter(NullOutputStream.INSTANCE)) {
            diffFormatter.setRepository(git.getRepository());
            List<DiffEntry> diffEntries = diffFormatter.scan(commit1, commit2);
            if (diffEntries.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void saveMultipleFiles(FileData folderData,
                                   Iterable<FileItem> files,
                                   ChangesetType changesetType) throws IOException {

        String commitId = null;
        try {
            String parentVersion = folderData.getVersion();
            boolean checkoutOldVersion = isCheckoutOldVersion(folderData.getName(), parentVersion);
            checkoutForcedOrReset(checkoutOldVersion ? parentVersion : branch);

            RevCommit commit = createCommit(folderData, files, changesetType);
            commitId = commit.getId().getName();

            resolveAndMerge(folderData, checkoutOldVersion, commit);
            addTagToCommit(commit, folderData.getAuthor());

            push();

            if (uri == null) {
                // GC is required in local mode. In remote mode autoGC() will be invoked on each fetch or merge.
                // autoGC() didn't solve the issue for local repository, so we use gc() instead.
                try {
                    git.gc().call();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            reset(commitId);
            throw e;
        } catch (Exception e) {
            reset(commitId);
            throw new IOException(e.getMessage(), e);
        }
    }

    private RevCommit createCommit(FileData folderData,
                                   Iterable<FileItem> files,
                                   ChangesetType changesetType) throws IOException, GitAPIException {
        String relativeFolder = folderData.getName();

        List<String> changedFiles = new ArrayList<>();

        // Add new files and update existing ones
        List<File> savedFiles = new ArrayList<>();
        for (FileItem change : files) {
            File file = new File(localRepositoryPath, change.getData().getName());
            savedFiles.add(file);
            applyChangeInWorkspace(change, changedFiles);
        }

        if (changesetType == ChangesetType.FULL) {
            // Remove absent files
            String basePath = new File(localRepositoryPath).getAbsolutePath();
            File folder = new File(localRepositoryPath, relativeFolder);
            removeAbsentFiles(basePath, folder, savedFiles);
        }

        CommitCommand commitCommand = git.commit()
                .setNoVerify(noVerify)
                .setMessage(formatComment(CommitType.SAVE, folderData))
                .setCommitter(folderData.getAuthor().getDisplayName(),
                        Optional.ofNullable(folderData.getAuthor().getEmail()).orElse(""));

        return commitChangedFiles(commitCommand);
    }

    private void applyChangeInWorkspace(FileItem change, Collection<String> changedFiles) throws IOException,
            GitAPIException {
        File file = new File(localRepositoryPath, change.getData().getName());
        createParent(file);

        InputStream stream = change.getStream();
        if (stream != null) {
            try (FileOutputStream output = new FileOutputStream(file)) {
                stream.transferTo(output);
            }
            git.add().addFilepattern(change.getData().getName()).call();
            changedFiles.add(change.getData().getName());
        } else {
            if (file.exists()) {
                git.rm().addFilepattern(change.getData().getName()).call();
                changedFiles.add(change.getData().getName());
            }
        }
    }

    private RevCommit commitChangedFiles(CommitCommand commitCommand) throws GitAPIException {
        RevCommit commit;
        if (git.status().call().getUncommittedChanges().isEmpty()) {
            // For the cases:
            // 1) User modified a project, then manually reverted, then pressed save.
            // 2) Copy project that does not have rules.xml, check "Copy old revisions". The last one commit should
            // have changed rules.xml with changed project name but the project does not have rules.xml so there are
            // no changes
            // 3) Try to deploy several times same deploy configuration. For example if we need to trigger
            // webservices redeployment without actually changing projects.
            commit = commitCommand.setAllowEmpty(true).call();
        } else {
            commit = commitCommand.call();
        }
        return commit;
    }

    private void resolveAndMerge(FileData folderData,
                                 boolean checkoutOldVersion,
                                 RevCommit commit) throws GitAPIException, IOException {
        ConflictResolveData conflictResolveData = folderData.getAdditionalData(ConflictResolveData.class);
        RevCommit lastCommit = commit;

        if (conflictResolveData != null) {
            lastCommit = resolveConflict(folderData.getAuthor(), conflictResolveData);
        }

        if (checkoutOldVersion || conflictResolveData != null) {
            // Merge detached commit to existing branch.
            checkoutForced(branch);
            ObjectId commitId = lastCommit.getId();
            ObjectIdRef.Unpeeled ref = new ObjectIdRef.Unpeeled(Ref.Storage.LOOSE, commitId.name(), commitId.copy());
            String mergeMessage = getMergeMessage(ref);
            MergeResult mergeDetached = git.merge().include(commitId).setMessage(mergeMessage).setCommit(false).call();
            validateNonConflictingMerge(mergeDetached);
            validateMergeConflict(mergeDetached, false, folderData.getBranch(), folderData.getAuthor());
            applyMergeCommit(mergeDetached, mergeMessage, folderData.getAuthor());
        }
    }

    private RevCommit resolveConflict(UserInfo author, ConflictResolveData conflictResolveData) throws GitAPIException,
            IOException {
        // Merge with a commit we have a conflict.
        MergeResult mergeResult = git.merge()
                .include(getCommitByVersion(conflictResolveData.getCommitToMerge()))
                .call();

        return resolveConflict(mergeResult, conflictResolveData, author);
    }

    private RevCommit resolveConflict(MergeResult mergeResult,
                                      ConflictResolveData conflictResolveData,
                                      UserInfo author) throws IOException, GitAPIException {
        if (mergeResult.getMergeStatus() != MergeResult.MergeStatus.CONFLICTING) {
            log.debug("Merge status: {}", mergeResult.getMergeStatus());
            throw new IOException("There is no merge conflict, nothing to resolve.");
        }

        // Resolve merge conflict.
        String mergeMessage = conflictResolveData.getMergeMessage();
        if (mergeMessage == null) {
            mergeMessage = "Merge";
        }
        CommitCommand conflictResolveCommit = git.commit()
                .setNoVerify(noVerify)
                .setMessage(mergeMessage)
                .setCommitter(author.getDisplayName(), Optional.ofNullable(author.getEmail()).orElse(""));

        Status status = git.status().call();

        Set<String> changedFiles = new HashSet<>();
        for (FileItem change : conflictResolveData.getResolvedFiles()) {
            applyChangeInWorkspace(change, changedFiles);
        }

        for (String changed : status.getChanged()) {
            if (!changedFiles.contains(changed)) {
                git.add().addFilepattern(changed).call();
                changedFiles.add(changed);
            }
        }
        for (String added : status.getAdded()) {
            if (!changedFiles.contains(added)) {
                git.add().addFilepattern(added).call();
                changedFiles.add(added);
            }
        }
        for (String removed : status.getRemoved()) {
            if (!changedFiles.contains(removed)) {
                git.rm().addFilepattern(removed).call();
                changedFiles.add(removed);
            }
        }

        return commitChangedFiles(conflictResolveCommit);
    }

    private ObjectId getCommitByVersion(String version) throws IOException {
        Ref ref = git.getRepository().findRef(version);
        if (ref == null) {
            // Version is a hash for commit
            return git.getRepository().resolve(version);
        }

        // Version is a tag.
        ObjectId objectId = git.getRepository().getRefDatabase().peel(ref).getPeeledObjectId();
        // Not annotated tags return null for getPeeledObjectId().
        return objectId == null ? ref.getObjectId() : objectId;
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setSupportsUniqueFileId(true).setSearchable(true).setFolders(true).build();
    }

    @Override
    public String getBranch() {
        return branch;
    }

    @Override
    public void createBranch(String projectPath, String branch) throws IOException {
        createBranch(projectPath, branch, null);
    }

    @Override
    public void createBranch(String projectPath, String newBranch, String startPoint) throws IOException {
        initializeGit(true);

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("createBranch(): lock");
            writeLock.lock();
            initLfsCredentials();

            reset();

            if (startPoint != null) {
                var obj = git.getRepository().resolve(startPoint);
                if (obj == null) {
                    throw new IOException("Cannot resolve " + startPoint);
                }
            }

            // If newBranch does not exist, create it.
            Ref branchRef = git.getRepository().findRef(newBranch);
            boolean branchAbsents = branchRef == null;
            if (branchAbsents) {
                // Checkout existing branch
                if (isEmpty()) {
                    throw new IOException("Cannot create a branch on the empty repository.");
                }
                checkoutForced(branch);

                // Create new branch
                var createBranchCommand = git.branchCreate().setName(newBranch);
                if (startPoint != null) {
                    createBranchCommand.setStartPoint(startPoint);
                }
                branchRef = createBranchCommand.call();
                pushBranch(new RefSpec().setSource(newBranch).setDestination(Constants.R_HEADS + newBranch));
            }

            lockSettings();
            try {
                BranchesData branches = getBranches(false);
                branches.addBranch(projectPath, branch, null);
                branches.addBranch(projectPath, newBranch, branchRef.getObjectId().getName());

                saveBranches();
            } finally {
                unlockSettings();
            }
        } catch (IOException e) {
            reset();
            try {
                git.branchDelete().setBranchNames(newBranch).call();
            } catch (Exception ignored) {
            }
            throw e;
        } catch (Exception e) {
            reset();
            try {
                git.branchDelete().setBranchNames(newBranch).call();
            } catch (Exception ignored) {
            }
            throw new IOException(e.getMessage(), e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("createBranch(): unlock");
        }
    }

    @Override
    public void deleteBranch(String projectPath, String branch) throws IOException {
        initializeGit(true);

        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("deleteBranch(): lock");
            writeLock.lock();
            initLfsCredentials();

            reset();

            if (projectPath == null) {
                lockSettings();
                try {
                    BranchesData branches = getBranches(false);
                    // Remove the branch from all mappings.
                    if (branches.removeBranch(null, branch)) {
                        saveBranches();
                    }
                } finally {
                    unlockSettings();
                }

                // Remove the branch from git itself.
                // Cannot delete checked out branch. So we check out another branch instead.
                checkoutForced(baseBranch);
                git.branchDelete().setBranchNames(branch).setForce(true).call();
                pushBranch(new RefSpec().setSource(null).setDestination(Constants.R_HEADS + branch));
            } else {
                lockSettings();
                try {
                    BranchesData branches = getBranches(false);
                    // Remove branch mapping for specific project only.
                    if (branches.removeBranch(projectPath, branch)) {
                        saveBranches();
                    }
                } finally {
                    unlockSettings();
                }
            }
        } catch (IOException e) {
            reset();
            throw e;
        } catch (Exception e) {
            reset();
            throw new IOException(e.getMessage(), e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("deleteBranch(): unlock");
        }
    }

    @Override
    public List<String> getBranches(String projectPath) throws IOException {
        initializeGit(true);

        Lock readLock = repositoryLock.readLock();
        try {
            log.debug("getBranches(): lock");
            readLock.lock();
            initLfsCredentials();

            BranchesData branches = getBranches(true);
            if (projectPath == null) {
                // Return all available branches
                TreeSet<String> branchNames = getAvailableBranches();

                // Local branches absent in repository may be needed to uncheck them in UI.
                for (List<String> projectBranches : branches.getProjectBranches().values()) {
                    branchNames.addAll(projectBranches);
                }

                return new ArrayList<>(branchNames);
            } else {
                // Return branches mapped to a specific project
                List<String> projectBranches = branches.getProjectBranches().get(projectPath);
                List<String> result;
                if (projectBranches == null) {
                    result = new ArrayList<>(Collections.singletonList(branch));
                } else {
                    result = new ArrayList<>(projectBranches);
                    result.sort(String.CASE_INSENSITIVE_ORDER);
                }
                return result;
            }
        } catch (GitAPIException e) {
            throw new IOException(e);
        } finally {
            resetLfsCredentials();
            readLock.unlock();
            log.debug("getBranches(): unlock");
        }
    }

    @Override
    public GitRepository forBranch(String branch) throws IOException {
        initializeGit(true);

        Lock readLock = repositoryLock.readLock();
        try {
            log.debug("forBranch(): read: lock");
            readLock.lock();
            initLfsCredentials();

            if (git.getRepository().findRef(branch) == null) {
                List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();

                boolean branchExist = false;
                String remoteBranchName = Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/" + branch;
                for (Ref ref : refs) {
                    String name = ref.getName();
                    if (remoteBranchName.equals(name)) {
                        branchExist = true;
                        break;
                    }
                }

                if (!branchExist) {
                    throw new IOException(String.format("Cannot find branch '%s'", branch));
                }
            }
        } catch (GitAPIException e) {
            throw new IOException(e);
        } finally {
            readLock.unlock();
            log.debug("forBranch(): read: unlock");
        }
        Lock writeLock = repositoryLock.writeLock();
        try {
            log.debug("forBranch(): write: lock");
            writeLock.lock();

            return createRepository(branch);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            resetLfsCredentials();
            writeLock.unlock();
            log.debug("forBranch(): write: unlock");
        }
    }

    private GitRepository createRepository(String branch) throws IOException, GitAPIException {
        if (git.getRepository().findRef(branch) == null) {
            createRemoteTrackingBranch(git, branch);
        }

        GitRepository repo = new GitRepository();

        repo.setId(id);
        repo.setName(name);
        repo.setUri(uri);
        repo.setLogin(login);
        repo.setPassword(password);
        repo.credentialsProvider = credentialsProvider;
        repo.setLocalRepositoryPath(localRepositoryPath);
        repo.setBranch(branch);
        repo.protectedBranchFilter = protectedBranchFilter;
        repo.baseBranch = baseBranch; // Base branch is only one
        repo.setTagPrefix(tagPrefix);
        repo.setListenerTimerPeriod(listenerTimerPeriod);
        repo.setConnectionTimeout(connectionTimeout);
        repo.setCommentTemplate(commentTemplate);
        repo.setCommentTemplateOld(commentTemplateOld);
        repo.setRepositorySettings(repositorySettings);
        repo.git = git;
        repo.repositoryLock = repositoryLock; // must be common for all instances because git
        // repository is same
        repo.remoteRepoLock = remoteRepoLock; // must be common for all instances because git
        // repository is same
        repo.branches = branches; // Can be shared between instances
        repo.monitor = monitor;
        repo.useLFS = useLFS;
        return repo;
    }

    private void createRemoteTrackingBranch(Git git, String branch) throws GitAPIException {
        git.branchCreate()
                .setName(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setStartPoint(Constants.DEFAULT_REMOTE_NAME + "/" + branch)
                .call();
    }

    TreeSet<String> getAvailableBranches() throws GitAPIException {
        return getAvailableBranches(git);
    }

    private TreeSet<String> getAvailableBranches(Git git) throws GitAPIException {
        TreeSet<String> branchNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        List<Ref> refs = git.branchList().call();
        for (Ref ref : refs) {
            String name = ref.getName();
            if (name.startsWith(Constants.R_HEADS)) {
                name = name.substring(Constants.R_HEADS.length());
                branchNames.add(name);
            }
        }
        return branchNames;
    }

    private void pushBranch(RefSpec refSpec) throws GitAPIException, IOException {
        if (uri == null) {
            return;
        }

        PushCommand push = git.push().setRefSpecs(refSpec).setTimeout(connectionTimeout);

        CredentialsProvider credentialsProvider = getCredentialsProvider(GitActionType.PUSH_BRANCH);
        if (credentialsProvider != null) {
            push.setCredentialsProvider(credentialsProvider);
        }

        Iterable<PushResult> results = push.call();
        successAuthentication(GitActionType.PUSH_BRANCH);
        validatePushResults(results);
    }

    private void readBranchesWithLock() throws IOException {
        lockSettings();
        try {
            readBranches();
        } finally {
            unlockSettings();
        }
    }

    private void readBranches() throws IOException {
        if (repositorySettings == null) {
            return;
        }

        settingsSyncDate = repositorySettings.getSyncDate();
        FileItem fileItem = repositorySettings.getRepository().read(branchesConfigFile);
        if (fileItem != null) {
            if (settingsSyncDate.before(fileItem.getData().getModifiedAt())) {
                settingsSyncDate = fileItem.getData().getModifiedAt();
            }
            try (InputStreamReader in = new InputStreamReader(fileItem.getStream(), StandardCharsets.UTF_8)) {
                branches.copyFrom(mapper.readValue(in, BranchesData.class));
            }
        }
    }

    private void saveBranches() throws IOException {
        if (repositorySettings == null) {
            return;
        }
        FileData data = new FileData();
        data.setName(branchesConfigFile);
        data.setAuthor(new UserInfo(getClass().getName()));
        data.setComment("Update branches info");
        var bytes = mapper.writeValueAsBytes(branches);
        repositorySettings.getRepository().save(data, new ByteArrayInputStream(bytes));
    }

    private void removeAbsentFiles(String baseAbsolutePath,
                                   File directory,
                                   Collection<File> toSave) throws GitAPIException {
        File[] found = directory.listFiles();

        if (found != null) {
            for (File file : found) {
                if (file.isDirectory()) {
                    removeAbsentFiles(baseAbsolutePath, file, toSave);
                } else {
                    if (!toSave.contains(file)) {
                        String relativePath = file.getAbsolutePath()
                                .substring(baseAbsolutePath.length())
                                .replace('\\', '/');
                        if (relativePath.startsWith("/")) {
                            relativePath = relativePath.substring(1);
                        }
                        git.rm().addFilepattern(relativePath).call();
                    }
                }
            }
        }
    }

    private void createParent(File file) throws FileNotFoundException {
        File parentFile = file.getParentFile();
        if (!parentFile.mkdirs() && !parentFile.exists()) {
            throw new FileNotFoundException("Cannot create the folder " + parentFile.getAbsolutePath());
        }
    }

    private URI getUri(String uriOrPath) {
        if (uriOrPath == null) {
            return null;
        }
        try {
            return new URL(uriOrPath).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            // uri can be a folder path. It's not valid URI but git accepts paths too.
            return new File(uriOrPath).toURI();
        }
    }

    private String escapeCurlyBrackets(String value) {
        String ret = value.replaceAll("\\{(?![012]})", "'{'");
        return ret.replaceAll("(?<!\\{[012])}", "'}'");
    }

    private String formatComment(CommitType commitType, FileData data) {
        String comment = StringUtils.trimToEmpty(data.getComment());
        if (escapedCommentTemplate == null) {
            return comment;
        }
        return MessageFormat.format(escapedCommentTemplate, commitType, comment);
    }

    private String getMergeMessage(Ref r) throws IOException {
        String userMessage = new MergeMessageFormatter().format(Collections.singletonList(r),
                git.getRepository().exactRef(Constants.HEAD));
        if (escapedCommentTemplate == null) {
            return userMessage;
        }
        return MessageFormat.format(escapedCommentTemplate, CommitType.MERGE, userMessage);
    }

    private void unlockSettings() {
        if (repositorySettings != null) {
            repositorySettings.unlock(branchesConfigFile);
        }
    }

    private void lockSettings() throws IOException {
        if (repositorySettings != null) {
            repositorySettings.lock(branchesConfigFile);
        }
    }

    boolean isCheckoutOldVersion(String path, String baseVersion) throws GitAPIException, IOException {
        if (baseVersion != null) {
            List<Ref> tags = git.tagList().call();

            RevCommit commit = findFirstCommit(git, resolveBranchId(), path);
            if (commit != null) {
                String lastVersion = getVersionName(git.getRepository(), tags, commit);
                return !baseVersion.equals(lastVersion);
            }
        }

        return false;
    }

    @Override
    public boolean isValidBranchName(String s) {
        return s != null && Repository.isValidRefName(Constants.R_HEADS + s);
    }

    @Override
    public boolean branchExists(String branch) throws IOException {
        for (String existedBranch : getBranches(null)) {
            if (existedBranch.equalsIgnoreCase(branch)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns Git repository that can safely be closed.<br/>
     * If our instance of GitRepository is not closed, returns Git object that reuses existing Repository instance. In
     * this case close() method does nothing: Repository object is managed by GitRepository.<br/>
     * If our instance of GitRepository is closed, returns Git object with a new Repository instance so the caller must
     * close it.<br/>
     * So we have a general rule: when you use getClosableGit(), you must always close returned object.
     */
    Git getClosableGit() throws IOException {
        if (closed) {
            return Git.open(new File(localRepositoryPath));
        } else {
            return new Git(git.getRepository());
        }
    }

    private CredentialsProvider getCredentialsProvider(GitActionType actionType) {
        if (credentialsProvider != null) {
            credentialsProvider.validateAuthorizationState(actionType);
        }
        return credentialsProvider;
    }

    private void successAuthentication(GitActionType actionType) {
        if (credentialsProvider != null) {
            credentialsProvider.successAuthentication(actionType);
        }
    }

    private void initLfsCredentials() {
        if (credentialsProvider != null && useLFS) {
            LfsFactory.setCredentialsProvider(credentialsProvider);
        }
    }

    private void resetLfsCredentials() {
        if (credentialsProvider != null && useLFS) {
            LfsFactory.removeCredentialsProvider();
        }
    }

    private ObjectLoader downloadLfs(ObjectLoader loader) throws IOException {
        if (!useLFS) {
            return loader;
        }
        return LfsBlobFilter.smudgeLfsBlob(git.getRepository(), loader);
    }

    private void configureBuiltInLFS(Repository repository) throws IOException {
        boolean lfsApplied = false;

        try (RevWalk walk = new RevWalk(repository)) {
            ObjectId branchId = null;
            if (repository.findRef(branch) != null) {
                branchId = repository.resolve(branch);
            }
            if (branchId != null) {
                RevCommit commit = walk.parseCommit(branchId);

                try (TreeWalk rootWalk = buildTreeWalk(repository, Constants.DOT_GIT_ATTRIBUTES, commit.getTree())) {
                    ObjectLoader loader = repository.open(rootWalk.getObjectId(0));
                    lfsApplied = new String(loader.getBytes(), StandardCharsets.UTF_8).contains("filter=lfs");
                } catch (FileNotFoundException ignored) {
                }
            }
        }

        useLFS = lfsApplied;

        if (useLFS) {
            log.info("LFS is enabled for repository '{}'.", name);
            try {
                boolean installed = repository.getConfig()
                        .getBoolean(ConfigConstants.CONFIG_FILTER_SECTION,
                                ConfigConstants.CONFIG_SECTION_LFS,
                                ConfigConstants.CONFIG_KEY_USEJGITBUILTIN,
                                false);
                if (!installed) {
                    LfsFactory.getInstance().getInstallCommand().setRepository(repository).call();
                }
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }

            File hookFile = repository.getFS().findHook(repository, PrePushHook.NAME);
            if (hookFile != null) {
                try (var input = new FileInputStream(hookFile)) {
                    var content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    if (content.contains("git lfs")) {
                        // Rename pre-push hook otherwise we will be spammed with warning message (if native git with
                        // LFS is found)
                        log.info(
                                "Rename pre-push hook to avoid conflict between LFS built-in hook and existing pre-push hook. Repo: {}",
                                repository);
                        String from = hookFile.getPath();
                        String to = from + ".renamed";
                        boolean renamed = hookFile.renameTo(new File(to));
                        if (!renamed) {
                            log.warn("Cannot rename '{}' to '{}'", from, to);
                        }
                    }
                }
            }
        }
    }

    private class GitRevisionGetter implements RevisionGetter {
        @Override
        public Object getRevision() {
            try {
                initializeGit(true);

                return getLastRevision();
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
                return null;
            }
        }
    }

    public interface WalkCommand<T> {
        T apply(Repository repository, TreeWalk rootWalk, String baseFolder) throws IOException,
                GitAPIException;
    }

    public interface HistoryVisitor<T> {
        /**
         * Visit commit for a file with a path {@code fullPath}
         *
         * @param fullPath      full path to the file
         * @param commit        visiting commit
         * @param commitVersion commit version. Either tag name or commit hash.
         * @return true if we should stop iterating history (we found needed information) and false if not found or
         * should iterate all commits
         */
        boolean visit(String fullPath, RevCommit commit, String commitVersion) throws IOException, GitAPIException;

        /**
         * Get accumulated result
         */
        T getResult();

        default FileData getLastVisited() {
            throw new IllegalStateException("Is not implemented!");
        }
    }

    private class ListCommand implements WalkCommand<List<FileData>> {

        private final ObjectId start;
        private final RevCommit revCommit;

        private ListCommand(ObjectId start) {
            this.start = start;
            this.revCommit = null;
        }

        private ListCommand(RevCommit revCommit) {
            this.start = revCommit.getId();
            this.revCommit = revCommit;
        }

        @Override
        public List<FileData> apply(Repository repository,
                                    TreeWalk rootWalk,
                                    String baseFolder) throws IOException {
            if (rootWalk != null) {
                // Iterate files in folder
                List<FileData> files = new ArrayList<>();
                if (rootWalk.getFilter() == TreeFilter.ALL) {
                    while (rootWalk.next()) {
                        files.add(createFileData(rootWalk, baseFolder, start));
                    }
                } else {
                    if (rootWalk.getTreeCount() > 0) {
                        try (TreeWalk dirWalk = new TreeWalk(repository)) {
                            dirWalk.addTree(rootWalk.getObjectId(0));
                            dirWalk.setRecursive(true);
                            while (dirWalk.next()) {
                                if (revCommit != null) {
                                    files.add(new LazyFileData(branch,
                                            baseFolder + dirWalk.getPathString(),
                                            GitRepository.this,
                                            revCommit,
                                            getFileId(dirWalk),
                                            commitMessageParser,
                                            commitMessageParserOld));
                                } else {
                                    files.add(createFileData(dirWalk, baseFolder, start));
                                }
                            }
                        }
                    }
                }

                return files;
            } else {
                return Collections.emptyList();
            }
        }
    }

    private class ListFoldersCommand implements WalkCommand<List<FileData>> {
        @Override
        public List<FileData> apply(Repository repository,
                                    TreeWalk rootWalk,
                                    String baseFolder) throws IOException {
            if (rootWalk != null) {
                if (rootWalk.getFilter() == TreeFilter.ALL) {
                    return collectFolderData(rootWalk, baseFolder);
                } else {
                    if (rootWalk.getTreeCount() > 0) {
                        try (TreeWalk dirWalk = new TreeWalk(repository)) {
                            dirWalk.addTree(rootWalk.getObjectId(0));
                            return collectFolderData(dirWalk, baseFolder);
                        }
                    }
                }
            }

            return Collections.emptyList();
        }

        private List<FileData> collectFolderData(TreeWalk rootWalk, String baseFolder) throws IOException {
            List<FileData> files = new ArrayList<>();
            rootWalk.setRecursive(false);
            ObjectId start = resolveBranchId();
            while (rootWalk.next()) {
                if ((rootWalk.getFileMode().getBits() & FileMode.TYPE_TREE) != 0) {
                    files.add(createFileData(rootWalk, baseFolder, start));
                }
            }

            return files;
        }
    }

    private class CheckCommand implements WalkCommand<FileData> {
        @Override
        public FileData apply(Repository repository,
                              TreeWalk rootWalk,
                              String baseFolder) throws IOException {
            if (rootWalk != null && StringUtils.isNotEmpty(baseFolder)) {
                return createFileData(rootWalk, "", resolveBranchId());
            } else {
                return null;
            }
        }
    }

    private class ReadCommand implements WalkCommand<FileItem> {
        @Override
        public FileItem apply(Repository repository,
                              TreeWalk rootWalk,
                              String baseFolder) throws IOException {
            if (rootWalk != null && StringUtils.isNotEmpty(baseFolder)) {
                FileData fileData = createFileData(rootWalk, "", resolveBranchId());
                ObjectLoader loader = downloadLfs(repository.open(rootWalk.getObjectId(0)));
                return new FileItem(fileData, loader.openStream());
            } else {
                return null;
            }
        }
    }

    private class ListHistoryVisitor implements HistoryVisitor<List<FileData>> {
        private final Repository repository;
        private final List<FileData> history = new ArrayList<>();
        private FileData last;

        private ListHistoryVisitor() {
            repository = git.getRepository();
        }

        @Override
        public boolean visit(String fullPath, RevCommit commit, String commitVersion) throws IOException {
            RevTree tree = commit.getTree();

            FileData data = null;
            try (TreeWalk rootWalk = buildTreeWalk(repository, fullPath, tree)) {
                data = createFileData(rootWalk, commit);
            } catch (FileNotFoundException e) {
                log.debug("File '{}' is absent in the commit {}", fullPath, commitVersion, e);
                data = new LazyFileData(branch,
                        fullPath,
                        GitRepository.this,
                        commit,
                        null,
                        commitMessageParser,
                        commitMessageParserOld);
                // Must mark it as deleted explicitly because the file can be erased outside OpenL Studio.
                data.setDeleted(true);
            }

            last = data;
            history.add(data);
            return false;
        }

        @Override
        public List<FileData> getResult() {
            Collections.reverse(history);
            return history;
        }

        @Override
        public FileData getLastVisited() {
            return last;
        }
    }

    private class ListFilesHistoryVisitor implements HistoryVisitor<List<FileData>> {
        private final String version;
        private final Repository repository;
        private final List<FileData> history = new ArrayList<>();

        private ListFilesHistoryVisitor(String version) {
            this.version = version;
            repository = git.getRepository();
        }

        @Override
        public boolean visit(String fullPath, RevCommit commit, String commitVersion) throws IOException {
            if (commitVersion.equals(version)) {
                RevTree tree = commit.getTree();

                try (TreeWalk rootWalk = buildTreeWalk(repository, fullPath, tree)) {
                    history.addAll(new ListCommand(commit).apply(repository, rootWalk, fullPath));
                } catch (FileNotFoundException ignored) {
                }

                return true;
            }

            return false;
        }

        @Override
        public List<FileData> getResult() {
            Collections.reverse(history);
            return history;
        }
    }

    private class CheckHistoryVisitor implements HistoryVisitor<FileData> {
        private final String version;
        private final Repository repository;
        private FileData result;

        private CheckHistoryVisitor(String version) {
            this.version = version;
            repository = git.getRepository();
        }

        @Override
        public boolean visit(String fullPath, RevCommit commit, String commitVersion) throws IOException {
            if (commitVersion.equals(version)) {
                RevTree tree = commit.getTree();

                try (TreeWalk rootWalk = buildTreeWalk(repository, fullPath, tree)) {
                    result = createFileData(rootWalk, commit);
                } catch (FileNotFoundException e) {
                    result = null;
                }

                return true;
            }

            return false;
        }

        @Override
        public FileData getResult() {
            return result;
        }
    }

    private class ReadHistoryVisitor implements HistoryVisitor<FileItem> {
        private final String version;
        private final Repository repository;
        private FileItem result;

        private ReadHistoryVisitor(String version) {
            this.version = version;
            repository = git.getRepository();
        }

        @Override
        public boolean visit(String fullPath, RevCommit commit, String commitVersion) throws IOException {
            if (commitVersion.equals(version)) {
                RevTree tree = commit.getTree();

                try (TreeWalk rootWalk = buildTreeWalk(repository, fullPath, tree)) {
                    FileData fileData = createFileData(rootWalk, commit);
                    ObjectLoader loader = downloadLfs(repository.open(rootWalk.getObjectId(0)));
                    result = new FileItem(fileData, loader.openStream());
                } catch (FileNotFoundException e) {
                    result = null;
                }

                return true;
            }

            return false;
        }

        @Override
        public FileItem getResult() {
            return result;
        }
    }

    private static class PatternIdRevFilter extends PatternMatchRevFilter {
        public PatternIdRevFilter(String pattern) {
            super(pattern, true, true, Pattern.CASE_INSENSITIVE);
        }

        @Override
        protected CharSequence text(RevCommit cmit) {
            return cmit.getId().getName();
        }

        @Override
        public RevFilter clone() {
            return new PatternIdRevFilter(pattern());
        }
    }

    private static class SubStringIdRevFilter extends SubStringRevFilter {
        public SubStringIdRevFilter(String patternText) {
            super(patternText);
        }

        @Override
        protected RawCharSequence text(RevCommit cmit) {
            String id = cmit.getId().getName();
            return new RawCharSequence(id.getBytes(StandardCharsets.UTF_8), 0, id.length());
        }
    }

    static RawCharSequence textFor(RevCommit cmit) {
        final byte[] raw = cmit.getRawBuffer();
        final int b = RawParseUtils.commitMessage(raw, 0);
        if (b < 0)
            return RawCharSequence.EMPTY;
        return new RawCharSequence(raw, b, raw.length);
    }

    static boolean isSame(URI a, URI b) {
        if (!Objects.equals(a.getRawFragment(), b.getRawFragment())) {
            return false;
        }
        Function<String, String> remLastSlash = s -> {
            int i = s.length();
            while (i > 0 && s.charAt(i - 1) == '/') {
                i--;
            }
            if (i != s.length() && s.charAt(i) == '/') {
                return s.substring(0, i);
            } else {
                return s;
            }
        };
        if (!Objects.equals(a.getRawSchemeSpecificPart(), b.getRawSchemeSpecificPart())) {
            if (!Objects.equals(remLastSlash.apply(a.getRawSchemeSpecificPart()),
                    remLastSlash.apply(b.getRawSchemeSpecificPart()))) {
                return false;
            }
        }
        if (!Objects.equals(a.getRawPath(), b.getRawPath())) {
            if (!Objects.equals(remLastSlash.apply(a.getRawPath()), remLastSlash.apply(b.getRawPath()))) {
                return false;
            }
        }
        if (!Objects.equals(a.getRawQuery(), b.getRawQuery())) {
            return false;
        }
        if (!Objects.equals(a.getRawAuthority(), b.getRawAuthority())) {
            return false;
        }
        if (a.getHost() != null) {
            if (!Objects.equals(a.getRawUserInfo(), b.getRawUserInfo())) {
                return false;
            }
            if (!a.getHost().equalsIgnoreCase(b.getHost())) {
                return false;
            }
            if (a.getPort() != b.getPort()) {
                return false;
            }
        }
        return Objects.equals(a.getRawAuthority(), b.getRawAuthority());
    }
}
