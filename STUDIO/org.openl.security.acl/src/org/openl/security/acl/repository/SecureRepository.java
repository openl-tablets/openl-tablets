package org.openl.security.acl.repository;

import static org.openl.security.acl.permission.AclPermission.DESIGN_REPOSITORY_READ;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.security.acl.permission.AclPermission;

public class SecureRepository implements Repository, RepositoryDelegate {
    private final Repository repository;
    protected final SimpleRepositoryAclService simpleRepositoryAclService;

    public SecureRepository(Repository repository, SimpleRepositoryAclService simpleRepositoryAclService) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.simpleRepositoryAclService = Objects.requireNonNull(simpleRepositoryAclService,
                "simpleRepositoryAclService cannot be null");
    }

    @Override
    public Repository getDelegate() {
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
        return repository.list(path)
                .stream()
                .filter(e -> simpleRepositoryAclService.isGranted(getId(), e.getName(), List.of(DESIGN_REPOSITORY_READ)))
                .collect(Collectors.toList());
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

    protected void checkSavePermissions(String name) throws IOException {
        if (repository.check(name) != null) {
            if (!simpleRepositoryAclService.isGranted(getId(), name, List.of(AclPermission.DESIGN_REPOSITORY_WRITE))) {
                throw new AccessDeniedException(
                        String.format("There is no permission for modifying '%s' in '%s' repository.", name, getName()));
            }
        } else {
            checkCreatePermissions(name);
        }
    }

    protected void checkCreatePermissions(String name) throws IOException {
        if (!simpleRepositoryAclService.isGranted(getId(), name, List.of(AclPermission.DESIGN_REPOSITORY_CREATE))) {
            throw new AccessDeniedException(
                    String.format("There is no permission for creating '%s' in '%s' repository.", name, getName()));
        }
    }

    protected void checkReadPermission(String name) throws IOException {
        if (!simpleRepositoryAclService.isGranted(getId(), name, List.of(DESIGN_REPOSITORY_READ))) {
            throw new AccessDeniedException(
                    String.format("There is no permission for reading '%s' from '%s' repository.", name, getName()));
        }
    }

    protected void checkDeletePermission(String name) throws IOException {
        if (!simpleRepositoryAclService.isGranted(getId(), name, List.of(AclPermission.DESIGN_REPOSITORY_DELETE))) {
            throw new AccessDeniedException(
                    String.format("There is no permission for deleting '%s' from '%s' repository.", name, getName()));
        }
    }

    protected void checkDeleteHistoryPermission(String name) throws IOException {
        if (!simpleRepositoryAclService
                .isGranted(getId(), name, List.of(AclPermission.DESIGN_REPOSITORY_DELETE_HISTORY))) {
            throw new AccessDeniedException(
                    String.format("There is no permission for deleting '%s' from '%s' repository.", name, getName()));
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
        checkDeletePermission(data.getName());
        return repository.delete(data);
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        for (FileData data1 : data) {
            checkDeletePermission(data1.getName());
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
        checkDeleteHistoryPermission(data.getName());
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
        return repository.listFolders(path)
                .stream()
                .filter(e -> simpleRepositoryAclService.isGranted(getId(), e.getName(), List.of(DESIGN_REPOSITORY_READ)))
                .collect(Collectors.toList());
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        return repository.listFiles(path, version)
                .stream()
                .filter(e -> simpleRepositoryAclService.isGranted(getId(), e.getName(), List.of(DESIGN_REPOSITORY_READ)))
                .collect(Collectors.toList());
    }

    @Override
    public FileData save(FileData folderData,
                         Iterable<FileItem> files,
                         ChangesetType changesetType) throws IOException {
        List<FileItem> newContentFileItems = new ArrayList<>();
        for (FileItem fileItem : files) {
            if (fileItem.getStream() != null) {
                checkSavePermissions(fileItem.getData().getName());
                newContentFileItems.add(fileItem);
            } else {
                if (!repository.list(fileItem.getData().getName()).isEmpty()) {
                    checkDeletePermission(fileItem.getData().getName());
                }
            }
        }
        if (changesetType == ChangesetType.FULL) {
            List<FileData> existingContentFileData = repository.list(folderData.getName());
            for (FileData fileData : existingContentFileData) {
                if (newContentFileItems.stream()
                        .noneMatch(e -> Objects.equals(e.getData().getName(), fileData.getName()))) {
                    checkDeletePermission(fileData.getName());
                }
            }
        }
        return repository.save(folderData, newContentFileItems, changesetType);
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
