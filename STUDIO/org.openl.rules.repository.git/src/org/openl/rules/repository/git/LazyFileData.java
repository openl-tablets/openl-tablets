package org.openl.rules.repository.git;

import java.io.IOException;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.FirstCommits.Revision;

/**
 * A file of a Git revision whose size and history are read on first use.
 *
 * <p>The latest commit that changes the file is found by a walk it may share with other files: the folders read from
 * one commit find theirs together, so a project listing walks the history once (see {@link FirstCommits}).
 *
 * <p>The same data may be read by several requests at once, as when a revision of a project is listed twice. What is
 * read lazily is published whole, so no request sees it half read. The history is read without holding the data:
 * reading it may wait for the lock of the repository, which a request holding that lock may need the data under.
 */
@Slf4j
class LazyFileData extends FileData {

    private final String fullPath;
    private final GitRepository gitRepo;
    private @Nullable FirstCommits firstCommits;
    private @Nullable RevCommit fileCommit;
    private @Nullable ObjectId fileId;

    private boolean loaded;
    private boolean deleteStatusLoaded;

    /**
     * @param firstCommits the walk that finds the latest commit changing the file; the path must have joined it
     */
    LazyFileData(String branch,
                 String fullPath,
                 GitRepository gitRepo,
                 FirstCommits firstCommits,
                 @Nullable ObjectId fileId) {
        setBranch(branch);
        setName(fullPath);
        if (fileId != null) {
            setUniqueId(fileId.getName());
        }

        this.fullPath = fullPath;
        this.gitRepo = gitRepo;
        this.firstCommits = firstCommits;
        this.fileId = fileId;
    }

    LazyFileData(String branch,
                 String fullPath,
                 GitRepository gitRepo,
                 RevCommit fileCommit,
                 @Nullable ObjectId fileId) {
        setBranch(branch);
        setName(fullPath);
        if (fileId != null) {
            setUniqueId(fileId.getName());
        }

        this.fullPath = fullPath;
        this.gitRepo = gitRepo;
        this.fileCommit = fileCommit;
        this.fileId = fileId;
    }

    @Override
    public synchronized long getSize() {
        if (fileId != null) {
            try (var git = gitRepo.getClosableGit()) {
                var loader = git.getRepository().open(fileId);
                super.setSize(loader.getSize());
                fileId = null;
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
                throw new IllegalStateException("Cannot get file size: " + e.getMessage(), e);
            }
        }

        return super.getSize();
    }

    @Override
    public synchronized void setSize(long size) {
        fileId = null;
        super.setSize(size);
    }

    @Override
    public UserInfo getAuthor() {
        verifyLoaded();
        return super.getAuthor();
    }

    @Override
    public void setAuthor(UserInfo author) {
        verifyLoaded();
        super.setAuthor(author);
    }

    @Override
    public String getComment() {
        verifyLoaded();
        return super.getComment();
    }

    @Override
    public void setComment(String comment) {
        verifyLoaded();
        super.setComment(comment);
    }

    @Override
    public Date getModifiedAt() {
        verifyLoaded();
        return super.getModifiedAt();
    }

    @Override
    public void setModifiedAt(Date modifiedAt) {
        verifyLoaded();
        super.setModifiedAt(modifiedAt);
    }

    @Override
    public String getVersion() {
        verifyLoaded();
        return super.getVersion();
    }

    @Override
    public void setVersion(String version) {
        verifyLoaded();
        super.setVersion(version);
    }

    @Override
    public boolean isDeleted() {
        // The history is read without holding the data, and the status read under it, as it is written
        verifyLoaded();
        return deleteStatus();
    }

    @Override
    public void setDeleted(boolean deleted) {
        markDeleted(deleted);
    }

    /**
     * Whether the file is deleted. A data item built from the selected branch tree exists in that branch. Historical
     * deletions are marked explicitly by the history visitor.
     */
    private synchronized boolean deleteStatus() {
        if (!deleteStatusLoaded) {
            super.setDeleted(false);
            deleteStatusLoaded = true;
        }
        return super.isDeleted();
    }

    private synchronized void markDeleted(boolean deleted) {
        super.setDeleted(deleted);
        deleteStatusLoaded = true;
    }

    private void verifyLoaded() {
        @Nullable RevCommit known;
        @Nullable FirstCommits commits;
        synchronized (this) {
            if (loaded) {
                return;
            }
            known = fileCommit;
            commits = firstCommits;
        }

        try (var git = gitRepo.getClosableGit()) {
            var revision = known != null ? Revision.of(known) : findRevision(git, commits);
            if (revision == null) {
                throw new IllegalStateException("Cannot find revision for the file " + fullPath);
            }
            var committerIdent = revision.committer();
            var version = resolveVersionName(git, revision.id());
            var history = isTechnicalRevision() ? gitRepo.checkHistory(fullPath, version) : null;
            synchronized (this) {
                if (loaded) {
                    return;
                }
                super.setComment(revision.message());
                super.setAuthor(new UserInfo(null, committerIdent.getEmailAddress(), committerIdent.getName()));
                super.setModifiedAt(committerIdent.getWhen());
                super.setVersion(version);
                if (isTechnicalRevision()) {
                    super.setDeleted(history == null || history.isDeleted());
                    deleteStatusLoaded = true;
                }
                loaded = true;
                firstCommits = null;
                fileCommit = null;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private @Nullable Revision findRevision(Git git, FirstCommits commits) throws IOException {
        try {
            return commits.of(git, fullPath);
        } catch (GitAPIException | MissingObjectException | IncorrectObjectTypeException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private String resolveVersionName(Git git, ObjectId commit) {
        try {
            return GitRepository.getVersionName(git.getRepository(), git.tagList().call(), commit);
        } catch (GitAPIException e) {
            throw new IllegalStateException("Cannot get tags list: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot get version name: " + e.getMessage(), e);
        }
    }

}
