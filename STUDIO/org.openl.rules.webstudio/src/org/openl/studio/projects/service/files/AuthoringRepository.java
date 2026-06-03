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
import org.openl.util.StringUtils;

/**
 * A {@link BranchRepository} that stamps every committed change with a fixed author and a non-empty
 * commit comment.
 *
 * <p>The repository files mount writes straight to a design repository, where each save is a git
 * commit that requires a committer and benefits from a meaningful message. The artefact write helpers
 * build {@code FileData} without an author or comment, so this wrapper supplies the current user and,
 * when no comment is set, a short operation-derived default. Reads, history and branch queries are
 * delegated unchanged.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public class AuthoringRepository implements BranchRepository {

    /**
     * Write operations intercepted to stamp the author and comment. Excluded from delegation so the
     * methods below are used instead of generated pass-throughs.
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

    /**
     * Stamps the author on the data and, when no comment is set, a non-empty default.
     */
    private FileData stamp(FileData data, String defaultComment) {
        if (data != null) {
            data.setAuthor(author);
            if (StringUtils.isBlank(data.getComment())) {
                data.setComment(defaultComment);
            }
        }
        return data;
    }

    private static String nameOf(FileData data) {
        String name = data == null ? null : data.getName();
        return StringUtils.isBlank(name) ? "files" : FilePaths.name(name);
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        return delegate.save(stamp(data, "Save " + nameOf(data)), stream);
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        fileItems.forEach(item -> stamp(item.getData(), "Save " + nameOf(item.getData())));
        return delegate.save(fileItems);
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) throws IOException {
        files.forEach(item -> stamp(item.getData(), "Save " + nameOf(item.getData())));
        return delegate.save(stamp(folderData, "Update files"), files, changesetType);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        return delegate.delete(stamp(data, "Delete " + nameOf(data)));
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        data.forEach(item -> stamp(item, "Delete " + nameOf(item)));
        return delegate.delete(data);
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        return delegate.deleteHistory(stamp(data, "Delete " + nameOf(data)));
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        return delegate.copyHistory(srcName, stamp(destData, "Copy " + nameOf(destData)), version);
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        return new AuthoringRepository(delegate.forBranch(branch), author);
    }
}
