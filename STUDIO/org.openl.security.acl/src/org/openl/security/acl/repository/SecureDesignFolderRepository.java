package org.openl.security.acl.repository;

import static org.openl.security.acl.permission.AclPermission.DESIGN_REPOSITORY_READ;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.security.acl.permission.AclPermission;

public class SecureDesignFolderRepository extends SecureDesignRepository implements FolderRepository {

    private final FolderRepository folderRepository;

    public SecureDesignFolderRepository(FolderRepository repository,
            DesignRepositoryAclService designRepositoryAclService) {
        super(repository, designRepositoryAclService);
        this.folderRepository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        return folderRepository.listFolders(path)
            .stream()
            .filter(e -> designRepositoryAclService.isGranted(getId(), e.getName(), List.of(DESIGN_REPOSITORY_READ)))
            .collect(Collectors.toList());
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        return folderRepository.listFiles(path, version)
            .stream()
            .filter(e -> designRepositoryAclService.isGranted(getId(), e.getName(), List.of(DESIGN_REPOSITORY_READ)))
            .collect(Collectors.toList());
    }

    @Override
    public FileData save(FileData folderData,
            Iterable<FileItem> files,
            ChangesetType changesetType) throws IOException {
        if (changesetType == ChangesetType.FULL && !designRepositoryAclService.isGranted(folderRepository.getId(),
            folderData.getName(),
            List.of(AclPermission.DESIGN_REPOSITORY_WRITE))) {
            throw new AccessDeniedException("Access denied");
        }
        List<FileItem> fileItems = new ArrayList<>();
        for (FileItem fileItem : files) {
            if (!designRepositoryAclService.isGranted(folderRepository.getId(),
                fileItem.getData().getName(),
                List.of(AclPermission.DESIGN_REPOSITORY_WRITE))) {
                throw new AccessDeniedException("Access denied");
            }
            fileItems.add(fileItem);
        }
        return folderRepository.save(folderData, fileItems, changesetType);
    }
}
