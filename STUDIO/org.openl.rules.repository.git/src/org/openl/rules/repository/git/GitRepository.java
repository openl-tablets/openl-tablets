package org.openl.rules.repository.git;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FS;
import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.rules.repository.common.RevisionGetter;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitRepository implements FolderRepository, BranchRepository, Closeable, RRepositoryFactory {
    private static final String DELETED_MARKER_FILE = ".archived";

    private final Logger log = LoggerFactory.getLogger(GitRepository.class);

    private String uri;
    private String login;
    private String password;
    private String localRepositoryPath;
    private String branch = Constants.MASTER;
    private String baseBranch = branch;
    private String tagPrefix = "";
    private int listenerTimerPeriod = 10;
    private String commentPattern;
    private String gitSettingsPath;

    private ChangesMonitor monitor;
    private Git git;

    private ReadWriteLock repositoryLock = new ReentrantReadWriteLock();

    private Map<String, List<String>> branches = new HashMap<>();

    @Override
    public List<FileData> list(String path) throws IOException {
        return iterate(path, new ListCommand(resolveBranchId()));
    }

    @Override
    public FileData check(String name) throws IOException {
        return iterate(name, new CheckCommand());
    }

    @Override
    public FileItem read(String name) throws IOException {
        return iterate(name, new ReadCommand());
    }

    @Override
    @SuppressWarnings("squid:S2095") // resources are closed by IOUtils
    public FileData save(FileData data, InputStream stream) throws IOException {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            git.checkout().setName(branch).call();

            String fileInRepository = data.getName();
            File file = new File(localRepositoryPath, fileInRepository);
            createParent(file);
            IOUtils.copyAndClose(stream, new FileOutputStream(file));

            git.add().addFilepattern(fileInRepository).call();
            // TODO: Add possibility to set committer email
            RevCommit commit = git.commit()
                    .setMessage(StringUtils.trimToEmpty(data.getComment()))
                    .setCommitter(data.getAuthor(), "")
                    .setOnly(fileInRepository)
                    .call();

            addTagToCommit(commit);

            push();
        } catch (Exception e) {
            reset();
            throw new IOException(e);
        } finally {
            writeLock.unlock();
        }

        return check(data.getName());
    }

    @Override
    public boolean delete(FileData data) {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            git.checkout().setName(branch).call();

            String name = data.getName();
            File file = new File(localRepositoryPath, name);
            if (!file.exists()) {
                return false;
            }

            if (file.isDirectory()) {
                String comment = StringUtils.trimToEmpty(data.getComment());
                String commitMessage = MessageFormat.format(commentPattern, CommitType.ARCHIVE, comment);

                // Create marker file if it absents and write current time
                try (DataOutputStream os = new DataOutputStream(new FileOutputStream(new File(file, DELETED_MARKER_FILE)))) {
                    os.writeLong(System.currentTimeMillis());
                }

                String markerFile = name + "/" + DELETED_MARKER_FILE;
                git.add().addFilepattern(markerFile).call();
                RevCommit commit = git.commit()
                        .setMessage(commitMessage)
                        .setCommitter(data.getAuthor(), "")
                        .setOnly(markerFile)
                        .call();

                addTagToCommit(commit);
            } else {
                // Files can't be archived. Only folders.
                git.rm().addFilepattern(name).call();
                RevCommit commit = git.commit()
                        .setMessage(StringUtils.trimToEmpty(data.getComment()))
                        .setCommitter(data.getAuthor(), "")
                        .call();
                addTagToCommit(commit);
            }

            push();

            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            reset();
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("squid:S2095") // resources are closed by IOUtils
    public FileData copy(String srcName, FileData destData) throws IOException {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            git.checkout().setName(branch).call();

            File src = new File(localRepositoryPath, srcName);
            File dest = new File(localRepositoryPath, destData.getName());
            IOUtils.copyAndClose(new FileInputStream(src), new FileOutputStream(dest));

            git.add().addFilepattern(destData.getName()).call();
            RevCommit commit = git.commit()
                    .setMessage(StringUtils.trimToEmpty(destData.getComment()))
                    .setCommitter(destData.getAuthor(), "")
                    .call();
            addTagToCommit(commit);

            push();
        } catch (Exception e) {
            reset();
            throw new IOException(e);
        } finally {
            writeLock.unlock();
        }

        return check(destData.getName());
    }

    @Override
    public FileData rename(String srcName, FileData destData) throws IOException {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            git.checkout().setName(branch).call();

            File src = new File(localRepositoryPath, srcName);
            File dest = new File(localRepositoryPath, destData.getName());
            FileUtils.move(src, dest);

            git.rm().addFilepattern(srcName).call();
            git.add().addFilepattern(destData.getName()).call();
            RevCommit commit = git.commit()
                    .setMessage(StringUtils.trimToEmpty(destData.getComment()))
                    .setCommitter(destData.getAuthor(), "")
                    .setOnly(srcName)
                    .setOnly(destData.getName())
                    .call();
            addTagToCommit(commit);

            push();
        } catch (Exception e) {
            reset();
            throw new IOException(e);
        } finally {
            writeLock.unlock();
        }

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
        return iterateHistory(name, new ListHistoryVisitor());
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        return iterateHistory(path, new ListFilesHistoryVisitor(version));
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        return iterateHistory(name, new CheckHistoryVisitor(version));
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        return iterateHistory(name, new ReadHistoryVisitor(version));
    }

    @Override
    public boolean deleteHistory(FileData data) {
        String name = data.getName();
        String version = data.getVersion();
        String author = StringUtils.trimToEmpty(data.getAuthor());
        String comment = StringUtils.trimToEmpty(data.getComment());

        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            git.checkout().setName(branch).call();

            RevCommit commit;
            if (version == null) {
                git.rm().addFilepattern(name).call();
                String commitMessage = MessageFormat.format(commentPattern, CommitType.ERASE, comment);
                commit = git.commit()
                        .setCommitter(author, "")
                        .setMessage(commitMessage)
                        .setOnly(name)
                        .call();

                addTagToCommit(commit);
            } else {
                FileData fileData = checkHistory(name, version);
                if (fileData == null) {
                    return false;
                }

                if (!fileData.isDeleted()) {
                    // We can "delete" only archived versions. Other version can't be deleted.
                    return false;
                }

                String markerFile = name + "/" + DELETED_MARKER_FILE;
                git.rm().addFilepattern(markerFile).call();
                String commitMessage = MessageFormat.format(commentPattern, CommitType.RESTORE, comment);
                commit = git.commit()
                        .setCommitter(author, "")
                        .setMessage(commitMessage)
                        .setOnly(markerFile)
                        .call();

                addTagToCommit(commit);
            }

            push();
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        if (version == null) {
            return copy(srcName, destData);
        }

        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            git.checkout().setName(branch).call();

            File src = new File(localRepositoryPath, srcName);
            if (src.isDirectory()) {
                List<FileChange> files = new ArrayList<>();
                try {
                    List<FileData> fileData = listFiles(srcName + "/", version);
                    for (FileData data : fileData) {
                        String fileFrom = data.getName();
                        FileItem fileItem = readHistory(fileFrom, data.getVersion());
                        String fileTo = destData.getName() + fileFrom.substring(srcName.length());
                        files.add(new FileChange(fileTo, fileItem.getStream()));
                    }
                    return save(destData, files);
                } finally {
                    for (FileChange file : files) {
                        IOUtils.closeQuietly(file.getStream());
                    }
                }
            } else {
                FileItem fileItem = null;
                try {
                    fileItem = readHistory(srcName, version);

                    destData.setSize(fileItem.getData().getSize());

                    return save(destData, fileItem.getStream());
                } finally {
                    if (fileItem != null) {
                        IOUtils.closeQuietly(fileItem.getStream());
                    }
                }
            }
        } catch (Exception e) {
            reset();
            throw new IOException(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void initialize() throws RRepositoryException {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            File local = new File(localRepositoryPath);

            boolean shouldClone;
            if (!local.exists()) {
                shouldClone = true;
            } else {
                File[] files = local.listFiles();
                if (files == null) {
                    throw new IOException("Folder '" + local + "' is not directory");
                }

                if (files.length > 0) {
                    if (RepositoryCache.FileKey.resolve(local, FS.DETECTED) != null) {
                        log.debug("Reuse existing local repository {}", local);
                        try (Repository repository = Git.open(local).getRepository()) {
                            String remoteUrl = repository.getConfig()
                                    .getString(ConfigConstants.CONFIG_REMOTE_SECTION,
                                            Constants.DEFAULT_REMOTE_NAME,
                                            ConfigConstants.CONFIG_KEY_URL);
                            if (!new URI(uri).equals(new URI(remoteUrl))) {
                                throw new IOException("Folder '" + local + "' already contains local git repository but is configured for different URI (" + remoteUrl + ").\nDelete it or choose another local path or set correct URL for repository.");
                            }
                        }
                        shouldClone = false;
                    } else {
                        // Can't overwrite existing files that is definitely not git repository
                        throw new IOException("Folder '" + local + "' already exists and is not empty. Delete it or choose another local path.");
                    }
                } else {
                    shouldClone = true;
                }
            }

            if (shouldClone) {
                try {
                    CloneCommand cloneCommand = Git.cloneRepository()
                            .setURI(uri)
                            .setDirectory(local)
                            .setBranch(branch)
                            .setBranchesToClone(Collections.singletonList(Constants.R_HEADS + branch));

                    if (StringUtils.isNotBlank(login)) {
                        cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(login, password));
                    }

                    Git cloned = cloneCommand.call();
                    cloned.close();
                } catch (Exception e) {
                    FileUtils.deleteQuietly(local);
                    throw e;
                }
            }

            git = Git.open(local);

            if (!shouldClone) {
                FetchCommand fetchCommand = git.fetch();
                if (StringUtils.isNotBlank(login)) {
                    fetchCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(login, password));
                }
                fetchCommand.call();

                boolean branchExists = git.getRepository().findRef(branch) != null;
                if (!branchExists) {
                    git.branchCreate()
                            .setName(branch)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                            .setStartPoint(Constants.DEFAULT_REMOTE_NAME + "/" + branch)
                            .call();
                }
            }

            readBranches();

            monitor = new ChangesMonitor(new GitRevisionGetter(), listenerTimerPeriod);
        } catch (Exception e) {
            throw new RRepositoryException(e.getMessage(), e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() {
        if (monitor != null) {
            monitor.release();
            monitor = null;
        }
        if (git != null) {
            git.close();
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

    public void setCommentPattern(String commentPattern) {
        this.commentPattern = commentPattern;
    }

    public void setGitSettingsPath(String gitSettingsPath) {
        this.gitSettingsPath = gitSettingsPath;
    }

    private static TreeWalk buildTreeWalk(org.eclipse.jgit.lib.Repository repository, String path, RevTree tree) throws
                                                                                                          IOException {
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
            throw new FileNotFoundException("Did not find expected path '" + path + "' in tree '" + tree.getName() + "'");
        }
        return treeWalk;
    }

    private FileData createFileData(TreeWalk dirWalk, String baseFolder, ObjectId start) {
        String fullPath = baseFolder + dirWalk.getPathString();
        return new LazyFileData(branch, fullPath, this, start, getFileId(dirWalk));
    }

    private ObjectId resolveBranchId() throws IOException {
        ObjectId branchId = git.getRepository().resolve(branch);
        if (branchId == null) {
            throw new IOException("Can't find branch '" + branch + "'");
        }
        return branchId;
    }

    private FileData createFileData(TreeWalk dirWalk, RevCommit fileCommit) {
        String fullPath = dirWalk.getPathString();

        return new LazyFileData(branch, fullPath, this, fileCommit, getFileId(dirWalk));
    }

    private ObjectId getFileId(TreeWalk dirWalk) {
        int fileModeBits = dirWalk.getFileMode().getBits();
        ObjectId fileId = null;
        if ((fileModeBits & FileMode.TYPE_FILE) != 0) {
            fileId = dirWalk.getObjectId(0);
        }
        return fileId;
    }

    private ObjectId getLastRevision() throws GitAPIException, IOException {
        pull();

        Lock readLock = repositoryLock.readLock();
        try {
            readLock.lock();
            return git.getRepository().resolve("HEAD^{tree}");
        } finally {
            readLock.unlock();
        }
    }

    private void pull() throws GitAPIException {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            // TODO: Consider changing merge strategy
            PullCommand pullCommand = git.pull().setStrategy(MergeStrategy.RECURSIVE);
            if (StringUtils.isNotBlank(login)) {
                pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(login, password));
            }

            PullResult pullResult = pullCommand.call();
            if (!pullResult.isSuccessful()) {
                throw new IllegalStateException("Can't pull: " + pullResult.toString());
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void push() throws GitAPIException {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            PushCommand push = git.push().setPushTags().add(branch);

            if (StringUtils.isNotBlank(login)) {
                push.setCredentialsProvider(new UsernamePasswordCredentialsProvider(login, password));
            }

            push.call();

            monitor.fireOnChange();
        } finally {
            writeLock.unlock();
        }
    }

    private <T> T iterate(String path, WalkCommand<T> command) throws IOException {
        Lock readLock = repositoryLock.readLock();
        try {
            readLock.lock();

            org.eclipse.jgit.lib.Repository repository = git.getRepository();
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit commit = walk.parseCommit(resolveBranchId());
                RevTree tree = commit.getTree();

                // Create TreeWalk for root folder
                try (TreeWalk rootWalk = buildTreeWalk(repository, path, tree)) {
                    return command.apply(repository, rootWalk, path);
                } catch (FileNotFoundException e) {
                    return command.apply(repository, null, path);
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            readLock.unlock();
        }
    }

    private <T> T iterateHistory(String name, HistoryVisitor<T> historyVisitor) throws IOException {
        Lock readLock = repositoryLock.readLock();
        try {
            readLock.lock();
            Iterator<RevCommit> iterator = git.log()
                    .add(resolveBranchId())
                    .addPath(name)
                    .call()
                    .iterator();

            List<Ref> tags = git.tagList().call();

            while (iterator.hasNext()) {
                RevCommit commit = iterator.next();

                boolean stop = historyVisitor.visit(name, commit, getVersionName(tags, commit));
                if (stop) {
                    break;
                }
            }

            return historyVisitor.getResult();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            readLock.unlock();
        }
    }

    private void reset() {
        try {
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
                    log.debug("Tag {} is skipped because it doesn't contain version number", name);
                    continue;
                }
                if (num > maxId) {
                    maxId = num;
                }
            }
        }

        return String.valueOf(maxId + 1);
    }

    String getVersionName(ObjectId commitId) throws GitAPIException {
        return getVersionName(git.tagList().call(), commitId);
    }

    private String getVersionName(List<Ref> tags, ObjectId commitId) {
        Ref tagRef = getTagRefForCommit(tags, commitId);

        return tagRef != null ? getLocalTagName(tagRef) : commitId.getName();
    }

    private Ref getTagRefForCommit(List<Ref> tags, ObjectId commitId) {
        Ref tagRefForCommit = null;
        for (Ref tagRef : tags) {
            ObjectId objectId = git.getRepository().peel(tagRef).getPeeledObjectId();
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

    private String getLocalTagName(Ref tagRef) {
        String name = tagRef.getName();
        return name.startsWith(Constants.R_TAGS) ? name.substring(Constants.R_TAGS.length()) : name;
    }

    String getCommentPattern() {
        return commentPattern;
    }

    Git getGit() {
        return git;
    }

    private void addTagToCommit(RevCommit commit) throws GitAPIException {
        pull();

        if (!tagPrefix.isEmpty()) {
            String tagName = tagPrefix + getNextTagId();
            git.tag().setObjectId(commit).setName(tagName).call();
        }
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        return iterate(path, new ListFoldersCommand());
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileChange> files) throws IOException {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            git.checkout().setName(branch).call();

            String relativeFolder = folderData.getName();

            // Add new files and update existing ones
            List<File> savedFiles = new ArrayList<>();
            for (FileChange change : files) {
                File file = new File(localRepositoryPath, change.getName());
                savedFiles.add(file);
                createParent(file);

                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(file);
                    IOUtils.copy(change.getStream(), output);
                } finally {
                    // Close only output stream. This class isn't responsible for input stream: stream must be closed in the
                    // place where it was created.
                    IOUtils.closeQuietly(output);
                }

                git.add().addFilepattern(change.getName()).call();
            }

            // Remove absent files
            String basePath = new File(localRepositoryPath).getAbsolutePath();
            File folder = new File(localRepositoryPath, relativeFolder);
            removeAbsentFiles(basePath, folder, savedFiles);

            // TODO: Add possibility to set committer email
            CommitCommand commitCommand = git.commit()
                    .setMessage(StringUtils.trimToEmpty(folderData.getComment()))
                    .setCommitter(folderData.getAuthor(), "");

            RevCommit commit;

            if (git.status().call().getUncommittedChanges().isEmpty()) {
                // For the cases:
                // 1) User modified a project, then manually reverted, then pressed save.
                // 2) Copy project that doesn't have rules.xml, check "Copy old revisions". The last one commit should
                // have changed rules.xml with changed project name but the project doesn't have rules.xml so there are no changes
                // 3) Try to deploy several times same deploy configuration. For example if we need to trigger
                // webservices redeployment without actually changing projects.
                commit = commitCommand.setAllowEmpty(true).call();
            } else {
                commit = commitCommand.setOnly(relativeFolder).call();
            }

            addTagToCommit(commit);

            push();
        } catch (Exception e) {
            reset();
            throw new IOException(e);
        } finally {
            writeLock.unlock();
        }

        return check(folderData.getName());
    }

    @Override
    public Features supports() {
        return new Features(this);
    }

    @Override
    public String getBranch() {
        return branch;
    }

    @Override
    public void createBranch(String projectName, String newBranch) throws IOException {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            // Checkout existing branch
            git.checkout().setName(branch).call();

            // Create new branch
            git.branchCreate().setName(newBranch).call();
            List<String> projectBranches = branches.get(projectName);
            if (projectBranches == null) {
                projectBranches = new ArrayList<>();
                projectBranches.add(branch); // Add main branch
                branches.put(projectName, projectBranches);
            }
            if (!projectBranches.contains(newBranch)) {
                projectBranches.add(newBranch);
            }

            pushBranch(new RefSpec().setSource(newBranch).setDestination(newBranch));

            saveBranches();
        } catch (Exception e) {
            reset();
            try {
                git.branchDelete().setBranchNames(newBranch).call();
            } catch (GitAPIException ignored) {
            }
            throw new IOException(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void deleteBranch(String projectName, String branch) throws IOException {
        Lock writeLock = repositoryLock.writeLock();
        try {
            writeLock.lock();

            // Can't delete checked out branch. So we check out another branch instead.
            git.checkout().setName(baseBranch).call();

            git.branchDelete().setBranchNames(branch).setForce(true).call();
            Collection<String> projectBranches = branches.get(projectName);
            if (projectBranches != null) {
                projectBranches.remove(branch);
                pushBranch(new RefSpec().setSource(null).setDestination(Constants.R_HEADS + branch));

                saveBranches();
            }
        } catch (Exception e) {
            reset();
            throw new IOException(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<String> getBranches(String projectName) {
        Lock readLock = repositoryLock.readLock();
        try {
            readLock.lock();
            List<String> projectBranches = branches.get(projectName);
            return projectBranches == null ? Collections.singletonList(branch) : projectBranches;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BranchRepository cloneFor(String branch) throws IOException {
        GitRepository repository = new GitRepository();

        repository.setUri(uri);
        repository.setLogin(login);
        repository.setPassword(password);
        repository.setLocalRepositoryPath(localRepositoryPath);
        repository.setBranch(branch);
        repository.baseBranch = baseBranch; // Base branch is only one
        repository.setTagPrefix(tagPrefix);
        repository.setListenerTimerPeriod(listenerTimerPeriod);
        repository.setCommentPattern(commentPattern);
        repository.setGitSettingsPath(gitSettingsPath);
        repository.git = Git.open(new File(localRepositoryPath));
        repository.repositoryLock = repositoryLock; // must be common for all instances because git repository is same
        repository.branches = branches; // Can be shared between instances
        repository.monitor = monitor;

        return repository;
    }

    private void pushBranch(RefSpec refSpec) throws GitAPIException {
        PushCommand push = git.push().setRefSpecs(refSpec);

        if (StringUtils.isNotBlank(login)) {
            push.setCredentialsProvider(new UsernamePasswordCredentialsProvider(login, password));
        }

        push.call();
    }

    private void readBranches() throws IOException {
        branches.clear();

        if (StringUtils.isBlank(gitSettingsPath)) {
            return;
        }

        File settings = new File(new File(gitSettingsPath), "branches.properties");
        if (settings.isFile()) {
            try (InputStreamReader in = new InputStreamReader(new FileInputStream(settings), StandardCharsets.UTF_8)) {
                Properties properties = new Properties();
                properties.load(in);
                String numStr = properties.getProperty("projects.number");
                if (numStr == null) {
                    return;
                }

                int num = Integer.parseInt(numStr);
                for (int i = 1; i <= num; i++) {
                    String name = properties.getProperty("project." + i + ".name");
                    String branchesStr = properties.getProperty("project." + i + ".branches");
                    if (StringUtils.isBlank(name) || StringUtils.isBlank(branchesStr)) {
                        continue;
                    }

                    branches.put(name, new ArrayList<>(Arrays.asList(branchesStr.split(","))));
                }
            }
        }

    }

    private void saveBranches() throws IOException {
        File parent = new File(gitSettingsPath);
        if (!parent.mkdirs() && !parent.exists()) {
            throw new FileNotFoundException("Can't create folder " + gitSettingsPath);
        }
        File settings = new File(parent, "branches.properties");
        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(settings), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.setProperty("projects.number", String.valueOf(branches.size()));

            int i = 1;
            for (Map.Entry<String, List<String>> entry : branches.entrySet()) {
                properties.setProperty("project." + i + ".name", entry.getKey());
                properties.setProperty("project." + i + ".branches", StringUtils.join(entry.getValue(), ","));

                i++;
            }
            properties.store(out, null);
        }
    }

    private void removeAbsentFiles(String baseAbsolutePath, File directory, Collection<File> toSave) throws GitAPIException {
        File[] found = directory.listFiles();

        if (found != null) {
            for (File file : found) {
                if (file.isDirectory()) {
                    removeAbsentFiles(baseAbsolutePath, file, toSave);
                } else {
                    if (!toSave.contains(file)) {
                        String relativePath = file.getAbsolutePath().substring(baseAbsolutePath.length()).replace('\\', '/');
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
            throw new FileNotFoundException("Can't create the folder " + parentFile.getAbsolutePath());
        }
    }

    private class GitRevisionGetter implements RevisionGetter {
        @Override
        public Object getRevision() {
            try {
                return getLastRevision();
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
                return null;
            }
        }
    }

    public interface WalkCommand<T> {
        T apply(org.eclipse.jgit.lib.Repository repository, TreeWalk rootWalk, String baseFolder) throws
                                                                                                  IOException,
                                                                                                  GitAPIException;
    }

    public interface HistoryVisitor<T> {
        /**
         * Visit commit for a file with a path {@code fullPath}
         *
         * @param fullPath full path to the file
         * @param commit visiting commit
         * @param commitVersion commit version. Either tag name or commit hash.
         * @return true if we should stop iterating history (we found needed information) and false if not found or
         * should iterate all commits
         */
        boolean visit(String fullPath, RevCommit commit, String commitVersion) throws IOException, GitAPIException;

        /**
         * Get accumulated result
         */
        T getResult();
    }

    private class ListCommand implements WalkCommand<List<FileData>> {
        private final ObjectId start;

        public ListCommand(ObjectId start) {
            this.start = start;
        }

        @Override
        public List<FileData> apply(org.eclipse.jgit.lib.Repository repository,
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
                                files.add(createFileData(dirWalk, baseFolder, start));
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
        public List<FileData> apply(org.eclipse.jgit.lib.Repository repository,
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
        public FileData apply(org.eclipse.jgit.lib.Repository repository, TreeWalk rootWalk, String baseFolder) throws
                                                                                                                IOException {
            if (rootWalk != null) {
                return createFileData(rootWalk, "", resolveBranchId());
            } else {
                return null;
            }
        }
    }

    private class ReadCommand implements WalkCommand<FileItem> {
        @Override
        public FileItem apply(org.eclipse.jgit.lib.Repository repository, TreeWalk rootWalk, String baseFolder) throws
                                                                                                                IOException {
            if (rootWalk != null) {
                FileData fileData = createFileData(rootWalk, "", resolveBranchId());
                ObjectLoader loader = repository.open(rootWalk.getObjectId(0));
                return new FileItem(fileData, loader.openStream());
            } else {
                return null;
            }
        }
    }

    private class ListHistoryVisitor implements HistoryVisitor<List<FileData>> {
        private final org.eclipse.jgit.lib.Repository repository;
        private final List<FileData> history = new ArrayList<>();

        private ListHistoryVisitor() {
            repository = git.getRepository();
        }

        @Override
        public boolean visit(String fullPath, RevCommit commit, String commitVersion) throws IOException {
            RevTree tree = commit.getTree();

            try (TreeWalk rootWalk = buildTreeWalk(repository, fullPath, tree)) {
                // Skip technical commits
                if (!isTechnicalCommit(commit)) {
                    history.add(createFileData(rootWalk, commit));
                }
            } catch (FileNotFoundException e) {
                log.debug("File '{}' is absent in the commit {}", fullPath, commitVersion, e);
            }

            return false;
        }

        private boolean isTechnicalCommit(RevCommit commit) {
            boolean technicalCommit = false;
            try {
                Object[] parse = new MessageFormat(commentPattern).parse(commit.getFullMessage());
                if (parse.length == 2) {
                    CommitType commitType = CommitType.valueOf(String.valueOf(parse[0]));
                    if (commitType != CommitType.NORMAL) {
                        technicalCommit = true;
                    }
                }
            } catch (ParseException | IllegalArgumentException ignored) {
                // If message doesn't conform format then it's typical commit message, not technical
            }
            return technicalCommit;
        }

        @Override
        public List<FileData> getResult() {
            Collections.reverse(history);
            return history;
        }
    }

    private class ListFilesHistoryVisitor implements HistoryVisitor<List<FileData>> {
        private final String version;
        private final org.eclipse.jgit.lib.Repository repository;
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
                    history.addAll(new ListCommand(commit.getId()).apply(repository, rootWalk, fullPath));
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
        private final org.eclipse.jgit.lib.Repository repository;
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
                    return true;
                }
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
        private final org.eclipse.jgit.lib.Repository repository;
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
                    ObjectLoader loader = repository.open(rootWalk.getObjectId(0));
                    result = new FileItem(fileData, loader.openStream());
                    return true;
                }
            }

            return false;
        }

        @Override
        public FileItem getResult() {
            return result;
        }
    }
}
