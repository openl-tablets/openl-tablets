package org.openl.rules.repository.git;

import java.io.IOException;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;

@Slf4j
class LazyFileData extends FileData {

    private final String fullPath;
    private final GitRepository gitRepo;
    private ObjectId fromCommit;
    private RevCommit fileCommit;
    private ObjectId fileId;

    private boolean loaded;
    private boolean deleteStatusLoaded;

    LazyFileData(String branch,
                 String fullPath,
                 GitRepository gitRepo,
                 ObjectId fromCommit,
                 ObjectId fileId) {
        setBranch(branch);
        setName(fullPath);
        if (fileId != null) {
            setUniqueId(fileId.getName());
        }

        this.fullPath = fullPath;
        this.gitRepo = gitRepo;
        this.fromCommit = fromCommit;
        this.fileId = fileId;
    }

    LazyFileData(String branch,
                 String fullPath,
                 GitRepository gitRepo,
                 RevCommit fileCommit,
                 ObjectId fileId) {
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
    public long getSize() {
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
    public void setSize(long size) {
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
        verifyDeleteStatusLoaded();
        return super.isDeleted();
    }

    @Override
    public void setDeleted(boolean deleted) {
        super.setDeleted(deleted);
        deleteStatusLoaded = true;
    }

    private void verifyLoaded() {
        if (loaded) {
            return;
        }

        try (var git = gitRepo.getClosableGit()) {
            if (fileCommit == null) {
                try {
                    fileCommit = GitRepository.findFirstCommit(git, fromCommit, fullPath);
                } catch (GitAPIException | MissingObjectException | IncorrectObjectTypeException e) {
                    log.error(e.getMessage(), e);
                }
                if (fileCommit == null) {
                    throw new IllegalStateException("Cannot find revision for the file " + fullPath);
                }

                fromCommit = null;
            }

            var message = fileCommit.getFullMessage();
            var committerIdent = fileCommit.getCommitterIdent();

            var userDisplayName = committerIdent.getName();
            super.setComment(message);
            super.setAuthor(new UserInfo(null, committerIdent.getEmailAddress(), userDisplayName));
            super.setModifiedAt(committerIdent.getWhen());

            String version;
            try {
                version = GitRepository.getVersionName(git.getRepository(), git.tagList().call(), fileCommit.getId());
            } catch (GitAPIException e) {
                throw new IllegalStateException("Cannot get tags list: " + e.getMessage(), e);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot get version name: " + e.getMessage(), e);
            }
            super.setVersion(version);

            if (isTechnicalRevision()) {
                var data = gitRepo.checkHistory(fullPath, version);
                super.setDeleted(data == null || data.isDeleted());
                deleteStatusLoaded = true;
            }

            loaded = true;
            fromCommit = null;
            fileCommit = null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void verifyDeleteStatusLoaded() {
        verifyLoaded();
        if (deleteStatusLoaded) {
            return;
        }

        // A data item built from the selected branch tree exists in that branch. Historical deletions are marked
        // explicitly by the history visitor.
        super.setDeleted(false);
        deleteStatusLoaded = true;
    }

}
