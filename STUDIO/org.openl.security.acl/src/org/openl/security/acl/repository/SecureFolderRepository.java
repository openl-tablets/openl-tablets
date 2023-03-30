package org.openl.security.acl.repository;

import static org.openl.security.acl.permission.AclPermission.DESIGN_REPOSITORY_READ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;

public class SecureFolderRepository extends SecureRepository implements FolderRepository {

    private final FolderRepository folderRepository;

    public SecureFolderRepository(FolderRepository repository, SimpleRepositoryAclService simpleRepositoryAclService) {
        super(repository, simpleRepositoryAclService);
        this.folderRepository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        return folderRepository.listFolders(path)
            .stream()
            .filter(e -> simpleRepositoryAclService.isGranted(getId(), e.getName(), List.of(DESIGN_REPOSITORY_READ)))
            .collect(Collectors.toList());
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        return folderRepository.listFiles(path, version)
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
                if (!folderRepository.list(fileItem.getData().getName()).isEmpty()) {
                    checkDeletePermission(fileItem.getData().getName());
                }
            }
        }
        if (changesetType == ChangesetType.FULL) {
            List<FileData> existingContentFileData = folderRepository.list(folderData.getName());
            for (FileData fileData : existingContentFileData) {
                if (newContentFileItems.stream()
                    .noneMatch(e -> Objects.equals(e.getData().getName(), fileData.getName()))) {
                    checkDeletePermission(fileData.getName());
                }
            }
        }
        return folderRepository.save(folderData, newContentFileItems, changesetType);
    }
}
