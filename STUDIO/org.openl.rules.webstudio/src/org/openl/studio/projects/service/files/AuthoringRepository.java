package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.util.StringUtils;

/**
 * A {@link BranchRepository} that stamps every committed change with the current user as the author and a
 * non-empty commit comment.
 *
 * <p>The repository files mount and a closed project write straight to a design repository, where each save is
 * a git commit. A commit requires a committer and benefits from a meaningful message. The artefact write helpers
 * build the {@code FileData} of a new file without an author or comment. They write an existing file with the
 * {@code FileData} it was read with, which names the author and the message of its last commit.
 *
 * <p>So every single-file change gets the current user and a short operation-derived message, whoever changed
 * the file before. A folder save keeps the message its caller sets, and gets a default only when none is set.
 *
 * <p>The author is looked up only when a change is written, so a mount that is only read never looks it up.
 * Reads, history and branch queries are delegated unchanged.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public class AuthoringRepository implements BranchRepository {

    private static final String SAVE_PREFIX = "Save ";
    private static final String DELETE_PREFIX = "Delete ";

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

        BranchRepository forBranch(@NonNull String branch) throws IOException;
    }

    @Delegate(excludes = Writes.class)
    private final BranchRepository delegate;
    private final Supplier<UserInfo> author;

    /**
     * The current user in the form the repository commits expect as the author.
     *
     * <p>The user is looked up on the first call and remembered, so a mount built for one request looks it up
     * at most once, however many files it writes.
     */
    static Supplier<UserInfo> currentAuthor(UserManagementService userManagementService) {
        return new Supplier<>() {
            private UserInfo author;

            @Override
            public synchronized UserInfo get() {
                if (author == null) {
                    var username = SecurityContextHolder.getContext().getAuthentication().getName();
                    author = Optional.ofNullable(userManagementService.getUser(username))
                            .map(user -> new UserInfo(user.getUsername(), user.getEmail(), user.getDisplayName()))
                            .orElseGet(() -> new UserInfo(username));
                }
                return author;
            }
        };
    }

    /**
     * Stamps the author and the comment on the data, replacing the ones it was read with.
     */
    private static FileData stamp(FileData data, UserInfo author, String comment) {
        if (data != null) {
            data.setAuthor(author);
            data.setComment(comment);
        }
        return data;
    }

    private static String nameOf(FileData data) {
        String name = data == null ? null : data.getName();
        return StringUtils.isBlank(name) ? "files" : FilePaths.name(name);
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        return delegate.save(stamp(data, author.get(), SAVE_PREFIX + nameOf(data)), stream);
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        var user = author.get();
        fileItems.forEach(item -> stamp(item.getData(), user, SAVE_PREFIX + nameOf(item.getData())));
        return delegate.save(fileItems);
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) throws IOException {
        var user = author.get();
        files.forEach(item -> stamp(item.getData(), user, SAVE_PREFIX + nameOf(item.getData())));
        var comment = folderData == null || StringUtils.isBlank(folderData.getComment())
                ? "Update files"
                : folderData.getComment();
        return delegate.save(stamp(folderData, user, comment), files, changesetType);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        return delegate.delete(stamp(data, author.get(), DELETE_PREFIX + nameOf(data)));
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        var user = author.get();
        data.forEach(item -> stamp(item, user, DELETE_PREFIX + nameOf(item)));
        return delegate.delete(data);
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        return delegate.deleteHistory(stamp(data, author.get(), DELETE_PREFIX + nameOf(data)));
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        return delegate.copyHistory(srcName, stamp(destData, author.get(), "Copy " + nameOf(destData)), version);
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        return new AuthoringRepository(delegate.forBranch(branch), author);
    }
}
