package org.openl.security.acl.repository;

import static org.openl.security.acl.permission.AclPermission.DESIGN_REPOSITORY_CREATE;
import static org.openl.security.acl.permission.AclPermission.DESIGN_REPOSITORY_DELETE;
import static org.openl.security.acl.permission.AclPermission.DESIGN_REPOSITORY_READ;
import static org.openl.security.acl.permission.AclPermission.DESIGN_REPOSITORY_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.security.acl.permission.AclPermission;

public class SecureDesignRepository implements Repository {
    private final Repository repository;
    protected final DesignRepositoryAclService designRepositoryAclService;

    public SecureDesignRepository(Repository repository, DesignRepositoryAclService designRepositoryAclService) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.designRepositoryAclService = Objects.requireNonNull(designRepositoryAclService,
            "designRepositoryAclService cannot be null");
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
            .filter(e -> designRepositoryAclService.isGranted(getId(), e.getName(), List.of(DESIGN_REPOSITORY_READ)))
            .collect(Collectors.toList());
    }

    @Override
    public FileData check(String name) throws IOException {
        if (designRepositoryAclService.isGranted(getId(), name, List.of(DESIGN_REPOSITORY_READ))) {
            return repository.check(name);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public FileItem read(String name) throws IOException {
        if (designRepositoryAclService.isGranted(getId(), name, List.of(DESIGN_REPOSITORY_READ))) {
            return repository.read(name);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        if (designRepositoryAclService
            .isGranted(getId(), data.getName(), List.of(writeOrCreatePermissionIsRequired(data.getName())))) {
            return repository.save(data, stream);
        }
        throw new AccessDeniedException("Access denied");
    }

    private AclPermission writeOrCreatePermissionIsRequired(String name) {
        try {
            return repository.check(name) != null ? DESIGN_REPOSITORY_WRITE : DESIGN_REPOSITORY_CREATE;
        } catch (IOException e) {
            return DESIGN_REPOSITORY_WRITE;
        }
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        if (fileItems.stream()
            .allMatch(e -> designRepositoryAclService.isGranted(getId(),
                e.getData().getName(),
                List.of(writeOrCreatePermissionIsRequired(e.getData().getName()))))) {
            return repository.save(fileItems);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        if (designRepositoryAclService.isGranted(getId(), data.getName(), List.of(DESIGN_REPOSITORY_DELETE))) {
            return repository.delete(data);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        if (data.stream()
            .allMatch(
                e -> designRepositoryAclService.isGranted(getId(), e.getName(), List.of(DESIGN_REPOSITORY_DELETE)))) {
            return repository.delete(data);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public void setListener(Listener callback) {
        repository.setListener(callback);
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        if (designRepositoryAclService.isGranted(getId(), name, List.of(DESIGN_REPOSITORY_READ))) {
            return repository.listHistory(name);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        if (designRepositoryAclService.isGranted(getId(), name, List.of(DESIGN_REPOSITORY_READ))) {
            return repository.checkHistory(name, version);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        if (designRepositoryAclService.isGranted(getId(), name, List.of(DESIGN_REPOSITORY_READ))) {
            return repository.readHistory(name, version);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        return repository.deleteHistory(data);
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        if (designRepositoryAclService
            .isGranted(getId(), srcName, List.of(DESIGN_REPOSITORY_READ)) && designRepositoryAclService
                .isGranted(getId(), destData.getName(), List.of(DESIGN_REPOSITORY_WRITE))) {
            return repository.copyHistory(srcName, destData, version);
        }
        throw new AccessDeniedException("Access denied");
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
