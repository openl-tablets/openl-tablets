package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderMapper;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AProjectFolder extends AProjectArtefact implements IProjectFolder {
    private static final Logger LOG = LoggerFactory.getLogger(AProject.class);

    private Map<String, AProjectArtefact> artefacts;
    private ResourceTransformer resourceTransformer;
    private String folderPath;
    private String historyVersion;

    public AProjectFolder(AProject project, Repository repository, String folderPath, String historyVersion) {
        super(project, repository, null);
        if (folderPath.startsWith("/")) {
            folderPath = folderPath.substring(1);
        }
        this.folderPath = folderPath;
        this.historyVersion = historyVersion;
    }

    /**
     * Create a folder with pre-initialised content
     *
     * @param artefacts pre-initialized artefact collection
     */
    public AProjectFolder(Map<String, AProjectArtefact> artefacts,
            AProject project,
            Repository repository,
            String folderPath) {
        this(project, repository, folderPath, null);
        this.artefacts = artefacts;
    }

    @Override
    public String getName() {
        return folderPath.substring(folderPath.lastIndexOf('/') + 1);
    }

    public AProjectArtefact getArtefact(String name) throws ProjectException {
        AProjectArtefact artefact = getArtefactsInternal().get(name);
        if (artefact == null) {
            throw new ProjectException("Cannot find project artefact ''{0}''", null, name);
        }

        return artefact;
    }

    public void deleteArtefact(String name) throws ProjectException {
        getProject().tryLockOrThrow();

        getArtefact(name).delete();
        getArtefactsInternal().remove(name);
    }

    public void deleteArtefactsInFolder(String folderName) throws ProjectException {
        getProject().tryLockOrThrow();
        Set<AProjectArtefact> artefactsToDelete = getArtefactsInternal().entrySet()
            .stream()
            .filter(entry -> entry.getKey().startsWith(folderName))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());
        for (AProjectArtefact artefact : artefactsToDelete) {
            artefact.delete();
            getArtefactsInternal().remove(artefact.getName());
        }
    }

    public boolean hasArtefact(String name) {
        return getArtefactsInternal().containsKey(name);
    }

    public AProjectFolder addFolder(String name) throws ProjectException {
        getProject().tryLockOrThrow();

        AProjectFolder createdFolder = new AProjectFolder(getProject(), getRepository(), folderPath + "/" + name, null);
        getArtefactsInternal().put(name, createdFolder);
        createdFolder.setResourceTransformer(resourceTransformer);
        return createdFolder;
    }

    public AProjectResource addResource(String name, InputStream content) throws ProjectException {
        try {
            getProject().tryLockOrThrow();

            FileData fileData = new FileData();
            String fullName = folderPath + "/" + name;
            fileData.setName(fullName);
            Repository repository = getRepository();
            if (repository.check(fullName) != null) {
                throw new ProjectException(String.format("The file '%s' exists in the folder.", name),
                    new IOException());
            }
            fileData = repository.save(fileData, content);
            AProjectResource createdResource = new AProjectResource(getProject(), repository, fileData);
            getArtefactsInternal().put(name, createdResource);
            return createdResource;
        } catch (IOException ex) {
            throw new ProjectException("Cannot add a resource", ex);
        } finally {
            IOUtils.closeQuietly(content);
        }
    }

    /**
     * Adds artefact to the folder creating needed subfolders (parent folders for artefact)
     *
     * @param artefact artefact to add
     */
    public void addArtefact(AProjectArtefact artefact) {
        Map<String, AProjectArtefact> artefactsInternal = getArtefactsInternal();
        if (artefact instanceof AProjectFolder) {
            // Add folders as is. They are split to sub-folders already.
            artefactsInternal.put(artefact.getName(), artefact);
            return;
        }

        String path = getFolderPath();
        String artefactPath = artefact.getFileData().getName();

        int subFolderNameStart = path.length() + 1;
        int subFolderNameEnd = artefactPath.indexOf('/', subFolderNameStart);
        if (subFolderNameEnd > -1) {
            // Has subfolder
            String name = artefactPath.substring(subFolderNameStart, subFolderNameEnd);
            AProjectFolder folder = (AProjectFolder) artefactsInternal.get(name);
            if (folder == null) {
                folder = new AProjectFolder(new HashMap<>(),
                    artefact.getProject(),
                    artefact.getRepository(),
                    path + "/" + name);
                artefactsInternal.put(name, folder);
            }
            folder.addArtefact(artefact);
        } else {
            artefactsInternal.put(artefact.getName(), artefact);
        }
    }

    public synchronized Collection<AProjectArtefact> getArtefacts() {
        return getArtefactsInternal().values();
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public void update(AProjectArtefact newFolder, CommonUser user) throws ProjectException {
        super.update(newFolder, user);
        if (this.isFolder()) {
            AProjectFolder from = (AProjectFolder) newFolder;

            List<FileItem> changes = new ArrayList<>();
            try {
                ChangesetType changesetType;

                FolderRepository fromRepository = (FolderRepository) from.getRepository();
                FolderRepository toRepository = (FolderRepository) getRepository();
                if (fromRepository.supports().uniqueFileId() && toRepository.supports().uniqueFileId()) {
                    changesetType = ChangesetType.DIFF;

                    String fromFilePath = from.getFolderPath() + "/";
                    List<FileData> fromList;
                    String fromProjectVersion;
                    if (fromRepository.supports().versions()) {
                        if (from.isHistoric()) {
                            fromProjectVersion = from.getHistoryVersion();
                            fromList = fromRepository.listFiles(fromFilePath, fromProjectVersion);
                        } else {
                            FileData fileData = fromRepository.check(from.getFolderPath());
                            if (fileData == null) {
                                fromProjectVersion = null;
                                fromList = Collections.emptyList();
                            } else {
                                fromProjectVersion = fileData.getVersion();
                                fromList = fromRepository.listFiles(fromFilePath, fromProjectVersion);
                            }
                        }
                    } else {
                        fromProjectVersion = null;
                        fromList = fromRepository.list(fromFilePath);
                    }

                    String toFilePath = getFolderPath() + "/";
                    List<FileData> toList = isHistoric() ? toRepository.listFiles(toFilePath, getHistoryVersion())
                                                         : toRepository.list(toFilePath);

                    ResourceTransformer transformer = getResourceTransformer();

                    // Search added and modified files
                    for (FileData fromData : fromList) {
                        String nameFrom = fromData.getName();
                        String nameTo = getFolderPath() + nameFrom.substring(from.getFolderPath().length());

                        String fromUniqueId = fromData.getUniqueId();
                        if (fromUniqueId == null) {
                            // The file was modified or added
                            FileItem read = fromRepository.supports().versions()
                                                                                 ? fromRepository.readHistory(nameFrom,
                                                                                     fromProjectVersion)
                                                                                 : fromRepository.read(nameFrom);
                            changes.add(new FileItem(nameTo, read.getStream()));
                        } else {
                            FileData toData = find(toList, nameTo);
                            if (toData == null || !fromUniqueId.equals(toData.getUniqueId())) {
                                // The file is absent in destination. Add it.
                                // Or different revision of a file.
                                FileData data = copyAndChangeName(fromData, nameTo);
                                InputStream content;
                                if (transformer != null) {
                                    FileData fileData = fromRepository.supports().versions() ? fromRepository
                                        .checkHistory(nameFrom, fromProjectVersion) : fromRepository.check(nameFrom);
                                    content = transformer
                                        .transform(new AProjectResource(from.getProject(), fromRepository, fileData));
                                } else {
                                    FileItem read = fromRepository.supports().versions() ? fromRepository
                                        .readHistory(nameFrom, fromProjectVersion) : fromRepository.read(nameFrom);
                                    content = read.getStream();
                                }
                                changes.add(new FileItem(data, content));
                            }
                            // Otherwise the file is same, no need to save it
                        }
                    }

                    // Search deleted files
                    for (FileData toData : toList) {
                        String nameTo = toData.getName();
                        String nameFrom = from.getFolderPath() + nameTo.substring(getFolderPath().length());

                        FileData fromData = find(fromList, nameFrom);
                        if (fromData == null) {
                            // File was deleted
                            changes.add(new FileItem(toData, null));
                        }
                    }
                } else {
                    changesetType = ChangesetType.FULL;
                    findChanges(from, changes);
                }

                FileData fileData = getFileData();
                fileData.setAuthor(user == null ? null : user.getUserName());
                setFileData(((FolderRepository) getRepository()).save(fileData, changes, changesetType));
            } catch (IOException e) {
                throw new ProjectException(e.getMessage(), e);
            } finally {
                for (FileItem change : changes) {
                    IOUtils.closeQuietly(change.getStream());
                }
            }
        }
    }

    private FileData copyAndChangeName(FileData data, String newName) {
        // Keep only required fields. The fields uniqueId and name are required. Fields like author, modifiedAt
        // aren't required for file saving, but retrieving that fields can be slow for a big amount of files.
        FileData copy = new FileData();
        copy.setName(newName);
        copy.setUniqueId(data.getUniqueId());

        return copy;
    }

    private static FileData find(List<FileData> list, String name) {
        for (FileData fileData : list) {
            if (fileData.getName().equals(name)) {
                return fileData;
            }
        }

        return null;
    }

    private void findChanges(AProjectFolder from, List<FileItem> files) throws ProjectException {
        ResourceTransformer transformer = getResourceTransformer();
        String path = getFolderPath();

        for (AProjectArtefact artefact : from.getArtefacts()) {
            if (artefact instanceof AProjectResource) {
                AProjectResource resource = (AProjectResource) artefact;
                InputStream content = transformer != null ? transformer.transform(resource) : resource.getContent();
                files.add(new FileItem(path + "/" + artefact.getInternalPath(), content));
            } else {
                findChanges((AProjectFolder) artefact, files);
            }
        }
    }

    private final Object lock = new Object();

    protected Map<String, AProjectArtefact> getArtefactsInternal() {
        synchronized (lock) {
            if (artefacts == null) {
                this.artefacts = createInternalArtefacts();
            }
        }
        return artefacts;
    }

    protected Map<String, AProjectArtefact> createInternalArtefacts() {
        HashMap<String, AProjectArtefact> internalArtefacts = new HashMap<>();
        Collection<FileData> fileDatas;
        String path = getFolderPath();
        if (!path.isEmpty() && !path.endsWith("/")) {
            path += "/";
        }
        try {
            if (isHistoric()) {
                if (getRepository().supports().folders()) {
                    fileDatas = ((FolderRepository) getRepository()).listFiles(path, getFileData().getVersion());
                } else {
                    throw new UnsupportedOperationException(
                        "Cannot get internal artifacts for historic project version");
                }
            } else {
                fileDatas = getRepository().list(path);
            }
            for (FileData fileData : fileDatas) {
                if (!fileData.getName().equals(path) && !fileData.isDeleted()) {
                    String artefactName = fileData.getName().substring(path.length());
                    internalArtefacts.put(artefactName, new AProjectResource(getProject(), getRepository(), fileData));
                }
            }
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return internalArtefacts;
    }

    @Override
    public void refresh() {
        super.refresh();
        synchronized (lock) {
            artefacts = null;
        }
    }

    public void setResourceTransformer(ResourceTransformer resourceTransformer) {
        this.resourceTransformer = resourceTransformer;

        if (artefacts != null) {
            for (AProjectArtefact artefact : artefacts.values()) {
                if (artefact instanceof AProjectFolder) {
                    ((AProjectFolder) artefact).setResourceTransformer(resourceTransformer);
                } else if (artefact instanceof AProjectResource) {
                    ((AProjectResource) artefact).setResourceTransformer(resourceTransformer);
                }
            }
        }
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getRealPath() {
        String path = getFolderPath();
        Repository repository = getRepository();
        if (repository.supports().mappedFolders()) {
            final FileData fileData = getFileData();
            if (fileData != null) {
                FileMappingData mappingData = fileData.getAdditionalData(FileMappingData.class);

                if (mappingData != null && path.equals(mappingData.getExternalPath())) {
                    return mappingData.getInternalPath();
                }
            }

            return ((FolderMapper) repository).getRealPath(path);
        } else {
            return path;
        }
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    @Override
    public boolean isHistoric() {
        return historyVersion != null && isRepositoryVersionable();
    }

    protected boolean isRepositoryVersionable() {
        return getRepository().supports().versions();
    }

    public String getHistoryVersion() {
        return historyVersion;
    }

    public void setHistoryVersion(String historyVersion) {
        this.historyVersion = historyVersion;
    }

    @Override
    public void setFileData(FileData fileData) {
        super.setFileData(fileData);
        if (fileData != null) {
            setFolderPath(fileData.getName());
            setHistoryVersion(fileData.getVersion());
        }
    }

    @Override
    public ArtefactPath getArtefactPath() {
        return new ArtefactPathImpl(getFolderPath());
    }

    @Override
    public String getInternalPath() {
        String projectPath = getProject().getFileData().getName();
        return folderPath.startsWith(projectPath + "/") ? folderPath.substring(projectPath.length() + 1) : folderPath;
    }

    @Override
    public void delete() throws ProjectException {
        for (AProjectArtefact artefact : getArtefacts()) {
            artefact.delete();
        }
        refresh();
    }

    public boolean hasArtefacts() {
        return !getArtefacts().isEmpty();
    }

    public ResourceTransformer getResourceTransformer() {
        return resourceTransformer;
    }
}
