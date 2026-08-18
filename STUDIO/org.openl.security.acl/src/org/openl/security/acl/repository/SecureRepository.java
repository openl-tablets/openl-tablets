package org.openl.security.acl.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.workspace.dtr.impl.FileMappingData;

public class SecureRepository implements Repository, RepositoryDelegate {
    private final Repository repository;
    protected final SimpleRepositoryAclService simpleRepositoryAclService;

    public SecureRepository(Repository repository, SimpleRepositoryAclService simpleRepositoryAclService) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.simpleRepositoryAclService = Objects.requireNonNull(simpleRepositoryAclService,
                "simpleRepositoryAclService cannot be null");
    }

    @Override
    public Repository getOriginal() {
        return repository;
    }

    @Override
    public String getId() {
        return repository.getId();
    }

    @Override
    public String getName() {
        return repository.getName();
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        return repository.list(path).stream().filter(this::isReadable).toList();
    }

    @Override
    public FileData check(String name) throws IOException {
        // Checking permissions here is a problem for
        // org.openl.rules.workspace.uw.impl.UserWorkspaceImpl.uploadLocalProject method
        // checkReadPermission(name);
        return repository.check(name);
    }

    @Override
    public FileItem read(String name) throws IOException {
        checkReadPermission(name);
        return repository.read(name);
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        checkSavePermissions(data.getName());
        return repository.save(data, stream);
    }

    /** The path the permissions for the content at that repository path are kept under. */
    protected String aclPath(String path) {
        return path;
    }

    /** The same, for content a listing reported: the listing itself answers with that path. */
    private String aclPath(FileData fileData) {
        var fileMappingData = fileData.getAdditionalData(FileMappingData.class);
        return fileMappingData != null ? fileMappingData.getInternalPath() : aclPath(fileData.getName());
    }

    private boolean isReadable(FileData fileData) {
        return simpleRepositoryAclService.isGranted(getId(), aclPath(fileData), List.of(BasePermission.READ));
    }

    protected void checkSavePermissions(String name) throws IOException {
        if (repository.check(name) != null) {
            if (!simpleRepositoryAclService.isGranted(getId(), aclPath(name), List.of(BasePermission.WRITE))) {
                throw new AccessDeniedException(
                        "There is no permission for modifying '%s' in '%s' repository.".formatted(name, getName()));
            }
        } else {
            checkCreatePermissions(name);
        }
    }

    protected void checkCreatePermissions(String name) throws IOException {
        if (!simpleRepositoryAclService.isGranted(getId(), aclPath(name), List.of(BasePermission.CREATE))) {
            throw new AccessDeniedException(
                    "There is no permission for creating '%s' in '%s' repository.".formatted(name, getName()));
        }
    }

    protected void checkReadPermission(String name) throws IOException {
        if (!simpleRepositoryAclService.isGranted(getId(), aclPath(name), List.of(BasePermission.READ))) {
            throw new AccessDeniedException(
                    "There is no permission for reading '%s' from '%s' repository.".formatted(name, getName()));
        }
    }

    private void checkDeletePermission(FileData fileData) throws IOException {
        if (!simpleRepositoryAclService.isGranted(getId(), aclPath(fileData), true, BasePermission.DELETE)) {
            throw new AccessDeniedException("There is no permission for deleting '%s' from '%s' repository."
                    .formatted(fileData.getName(), getName()));
        }
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        for (FileItem fileItem : fileItems) {
            checkSavePermissions(fileItem.getData().getName());
        }
        return repository.save(fileItems);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        checkDeletePermission(data);
        return repository.delete(data);
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        for (FileData data1 : data) {
            checkDeletePermission(data1);
        }
        return repository.delete(data);
    }

    @Override
    public void setListener(Listener callback) {
        repository.setListener(callback);
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        checkReadPermission(name);
        return repository.listHistory(name);
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        checkReadPermission(name);
        return repository.checkHistory(name, version);
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        checkReadPermission(name);
        return repository.readHistory(name, version);
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        checkDeletePermission(data);
        return repository.deleteHistory(data);
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        checkReadPermission(srcName);
        checkCreatePermissions(destData.getName());
        return repository.copyHistory(srcName, destData, version);
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        return repository.listFolders(path).stream().filter(this::isReadable).toList();
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        return repository.listFiles(path, version).stream().filter(this::isReadable).toList();
    }

    /**
     * Saves the folder, with the permission of every change checked.
     *
     * <p>A changeset at hand is checked as a whole before the repository is given any of it, so a refusal
     * leaves the folder as it was. A changeset that arrives as one open stream is checked change by change
     * as the repository takes it, and a refusal then leaves behind whatever a repository that cannot undo a
     * save has already written.
     */
    @Override
    public FileData save(FileData folderData,
                         Iterable<FileItem> files,
                         ChangesetType changesetType) throws IOException {
        try {
            if (files instanceof Collection) {
                // A changeset at hand can be checked as a whole, so a refusal still leaves the folder as it
                // was: nothing of it is written before all of it is allowed.
                var checked = new ArrayList<FileItem>();
                checkedChanges(folderData, files, changesetType).forEachRemaining(checked::add);
                return repository.save(folderData, checked, changesetType);
            }
            // The rest is one pass over one open stream - a deployment, a project read out of an archive - and
            // a file of it is readable only while it is the change at hand. Each is therefore checked as the
            // repository takes it: collecting them to check them upfront leaves every change but the last
            // behind an exhausted stream, and the project is written empty.
            return repository.save(folderData, () -> checkedChanges(folderData, files, changesetType), changesetType);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (IOException e) {
            throw refusalIn(e);
        }
    }

    /**
     * The changes with their permissions checked, in the order the repository takes them.
     *
     * <p>A full changeset holds everything the folder is to keep, so whatever it does not carry the
     * repository removes on its own. The permission to remove those is checked once the whole changeset is
     * known, which is still before the repository removes anything: it writes the changes first.
     */
    private Iterator<FileItem> checkedChanges(FileData folderData,
                                              Iterable<FileItem> files,
                                              ChangesetType changesetType) {
        var source = files.iterator();
        var keptNames = new HashSet<String>();
        return new Iterator<>() {
            private FileItem change;
            private boolean removalsChecked;

            @Override
            public boolean hasNext() {
                // The repository asks for the changes, so a refusal reaches it as the failure of a change it
                // cannot take: it is marked to be told apart from the failures the repository has of its own.
                try {
                    while (change == null && source.hasNext()) {
                        change = checked(source.next());
                    }
                    if (change == null && !removalsChecked) {
                        removalsChecked = true;
                        checkRemovals();
                    }
                } catch (AccessDeniedException e) {
                    throw new Refused(e);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return change != null;
            }

            @Override
            public FileItem next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                var checked = change;
                change = null;
                return checked;
            }

            /** The change to pass on, or {@code null} for a removal a full changeset does not carry itself. */
            private FileItem checked(FileItem fileItem) throws IOException {
                if (fileItem.getStream() == null) {
                    // A full changeset says what the folder holds, and what it does not hold is removed by
                    // that alone. Only a changeset of the changes themselves carries a removal of its own.
                    if (changesetType != ChangesetType.DIFF) {
                        return null;
                    }
                    if (exists(fileItem.getData().getName())) {
                        checkDeletePermission(fileItem.getData());
                    }
                } else {
                    checkSavePermissions(fileItem.getData().getName());
                }
                keptNames.add(fileItem.getData().getName());
                return fileItem;
            }

            private void checkRemovals() throws IOException {
                if (changesetType != ChangesetType.FULL) {
                    return;
                }
                for (FileData fileData : repository.list(asFolder(folderData.getName()))) {
                    if (!keptNames.contains(fileData.getName())) {
                        checkDeletePermission(fileData);
                    }
                }
            }
        };
    }

    /**
     * The refusal that ended a save, when the repository reported it as a failure of its own.
     *
     * <p>A permission is refused while the repository is already writing, so a repository that answers a
     * failed save with a failure of its own hides what actually happened. Only a refusal raised here counts:
     * a file the file system itself refuses is a failure of the save, not a matter of permissions.
     */
    private static IOException refusalIn(IOException failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof Refused refused) {
                return refused.getCause();
            }
        }
        return failure;
    }

    /** A permission refused while the repository was already taking the changes. */
    private static final class Refused extends UncheckedIOException {
        private Refused(AccessDeniedException refusal) {
            super(refusal);
        }

        @Override
        public AccessDeniedException getCause() {
            return (AccessDeniedException) super.getCause();
        }
    }

    /** Whether the repository holds anything at that path: one file, or a folder with content. */
    private boolean exists(String path) throws IOException {
        return repository.check(path) != null || !repository.list(asFolder(path)).isEmpty();
    }

    /** The path as a folder is named, which is what a listing answers about. */
    private static String asFolder(String path) {
        return path.isEmpty() || path.endsWith("/") ? path : path + "/";
    }

    @Override
    public Features supports() {
        return repository.supports();
    }

    @Override
    public void close() throws Exception {
        repository.close();
    }

    @Override
    public void validateConnection() throws IOException {
        repository.validateConnection();
    }

}
