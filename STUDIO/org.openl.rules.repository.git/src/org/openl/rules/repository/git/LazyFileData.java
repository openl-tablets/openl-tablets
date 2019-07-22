package org.openl.rules.repository.git;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.openl.rules.repository.api.FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LazyFileData extends FileData {
    private final Logger log = LoggerFactory.getLogger(GitRepository.class);

    private final String fullPath;
    private final File repoFolder;
    private ObjectId fromCommit;
    private RevCommit fileCommit;
    private ObjectId fileId;
    private final String commentTemplate;

    private boolean loaded = false;

    LazyFileData(String branch,
            String fullPath,
            File repoFolder,
            ObjectId fromCommit,
            ObjectId fileId,
            String commentTemplate) {
        setBranch(branch);
        setName(fullPath);
        if (fileId != null) {
            setUniqueId(fileId.getName());
        }

        this.fullPath = fullPath;
        this.repoFolder = repoFolder;
        this.commentTemplate = commentTemplate;
        this.fromCommit = fromCommit;
        this.fileId = fileId;
    }

    LazyFileData(String branch,
            String fullPath,
            File repoFolder,
            RevCommit fileCommit,
            ObjectId fileId,
            String commentTemplate) {
        setBranch(branch);
        setName(fullPath);
        if (fileId != null) {
            setUniqueId(fileId.getName());
        }

        this.fullPath = fullPath;
        this.repoFolder = repoFolder;
        this.commentTemplate = commentTemplate;
        this.fileCommit = fileCommit;
        this.fileId = fileId;
    }

    @Override
    public long getSize() {
        if (fileId != null) {
            try (Git git = Git.open(repoFolder)) {
                ObjectLoader loader = git.getRepository().open(fileId);
                super.setSize(loader.getSize());
                fileId = null;
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
                throw new IllegalStateException("Can't get file size: " + e.getMessage(), e);
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
    public String getAuthor() {
        verifyLoaded();
        return super.getAuthor();
    }

    @Override
    public void setAuthor(String author) {
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
        verifyLoaded();
        return super.isDeleted();
    }

    @Override
    public void setDeleted(boolean deleted) {
        verifyLoaded();
        super.setDeleted(deleted);
    }

    private void verifyLoaded() {
        if (loaded) {
            return;
        }

        try (Git git = Git.open(repoFolder)) {
            if (fileCommit == null) {
                Iterator<RevCommit> iterator = null;
                try {
                    iterator = git.log().add(fromCommit).addPath(fullPath).setMaxCount(1).call().iterator();
                } catch (GitAPIException | MissingObjectException | IncorrectObjectTypeException e) {
                    log.error(e.getMessage(), e);
                }
                if (iterator == null || !iterator.hasNext()) {
                    throw new IllegalStateException("Can't find revision for the file " + fullPath);
                }

                fileCommit = iterator.next();
                fromCommit = null;
            }

            PersonIdent committerIdent = fileCommit.getCommitterIdent();

            super.setAuthor(committerIdent.getName());
            super.setModifiedAt(committerIdent.getWhen());
            String message = fileCommit.getFullMessage();
            try {
                Object[] parse = new MessageFormat(commentTemplate).parse(message);
                if (parse.length >= 2) {
                    CommitType commitType = CommitType.valueOf(String.valueOf(parse[0]));
                    if (commitType == CommitType.ARCHIVE) {
                        super.setDeleted(true);
                    }
                    message = String.valueOf(parse[1]);
                    if (parse.length > 2) {
                        super.setAuthor(String.valueOf(parse[2]));
                    }
                }
            } catch (ParseException | IllegalArgumentException ignored) {
            }
            super.setComment(message);

            String version;
            try {
                version = GitRepository.getVersionName(git.getRepository(), git.tagList().call(), fileCommit.getId());
            } catch (GitAPIException e) {
                throw new IllegalStateException("Can't get tags list: " + e.getMessage(), e);
            }
            super.setVersion(version);

            loaded = true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
