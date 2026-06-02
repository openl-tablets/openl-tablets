package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;

/**
 * A {@link BranchRepository} that stamps every committed change with a fixed author.
 *
 * <p>The repository files mount writes straight to a design repository, where each save is a git
 * commit that requires a committer. The artefact write helpers build {@code FileData} without an
 * author, so this wrapper supplies the current user. Reads, history and branch queries are delegated
 * unchanged.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public class AuthoringRepository implements BranchRepository {

    /**
     * Write operations intercepted to stamp the author. Excluded from delegation so the methods
     * below are used instead of generated pass-throughs.
     */
    private interface Writes {
        FileData save(FileData data, InputStream stream) throws IOException;

        List<FileData> save(List<FileItem> fileItems) throws IOException;

        FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) throws IOException;

        boolean delete(FileData data) throws IOException;

        boolean delete(List<FileData> data) throws IOException;

        boolean deleteHistory(FileData data) throws IOException;

        FileData copyHistory(String srcName, FileData destData, String version) throws IOException;

        BranchRepository forBranch(String branch) throws IOException;
    }

    @Delegate(excludes = Writes.class)
    private final BranchRepository delegate;
    private final UserInfo author;

    private FileData withAuthor(FileData data) {
        if (data != null) {
            data.setAuthor(author);
        }
        return data;
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        return delegate.save(withAuthor(data), stream);
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        fileItems.forEach(item -> withAuthor(item.getData()));
        return delegate.save(fileItems);
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) throws IOException {
        files.forEach(item -> withAuthor(item.getData()));
        return delegate.save(withAuthor(folderData), files, changesetType);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        return delegate.delete(withAuthor(data));
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        data.forEach(this::withAuthor);
        return delegate.delete(data);
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        return delegate.deleteHistory(withAuthor(data));
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        return delegate.copyHistory(srcName, withAuthor(destData), version);
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        return new AuthoringRepository(delegate.forBranch(branch), author);
    }
}
