package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderMapper;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.repository.folder.FileChangesFromZip;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AProject extends AProjectFolder implements IProject {
    private static final Logger LOG = LoggerFactory.getLogger(AProject.class);

    /**
     * true if the project has a folder structure and false if the project is stored as a zip
     */
    private Boolean folderStructure;
    protected List<FileData> historyFileDatas;
    private String lastHistoryVersion;

    public AProject(Repository repository, String folderPath) {
        this(repository, folderPath, null);
    }

    public AProject(Repository repository, String folderPath, String historyVersion) {
        super(null, repository, folderPath, historyVersion);
    }

    public AProject(Repository repository, FileData fileData) {
        super(null, repository, fileData.getName(), fileData.getVersion());
        setFileData(fileData);
    }

    @Override
    public FileData getFileData() {
        FileData fileData = super.getFileData();
        if (fileData == null) {
            Repository repository = getRepository();
            if (isRepositoryVersionable()) {
                fileData = getFileDataForVersionableRepo(repository);
            } else {
                fileData = getFileDataForUnversionableRepo(repository);
            }
            setFileData(fileData);
        }
        return fileData;
    }

    private FileData getFileDataForVersionableRepo(Repository repository) {
        FileData fileData;// In the case of FolderRepository we can retrieve FileData using check()/checkHistory() for a
                          // folder.
        try {
            if (!isHistoric() || isLastVersion()) {
                fileData = repository.check(getFolderPath());
                if (fileData == null) {
                    // A project or deploy configuration doesn't exist yet. Probably we are creating it now.
                    fileData = new FileData();
                    fileData.setName(getFolderPath());
                }
            } else {
                fileData = repository.checkHistory(getFolderPath(), getHistoryVersion());
            }
            if (fileData != null && repository.supports().branches()) {
                fileData.setBranch(((BranchRepository) repository).getBranch());
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
        return fileData;
    }

    protected FileData getFileDataForUnversionableRepo(Repository repository) {
        try {
            FileData fileData = repository.check(getFolderPath());
            if (fileData == null) {
                // A project doesn't exist yet. Probably we are creating it now.
                fileData = new FileData();
                fileData.setName(getFolderPath());
                fileData.setVersion(getHistoryVersion());
            }
            return fileData;
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public String getLastHistoryVersion() {
        if (lastHistoryVersion == null) {
            // Retrieving all history is expensive. If it's retrieved already, use it, otherwise detect last version
            // from the repository directly.
            List<FileData> fileDatas = historyFileDatas;
            if (fileDatas != null) {
                lastHistoryVersion = fileDatas.isEmpty() ? null : fileDatas.get(fileDatas.size() - 1).getVersion();
            } else {
                lastHistoryVersion = findLastHistoryVersion();
            }
        }
        return lastHistoryVersion;
    }

    protected String findLastHistoryVersion() {
        String folderPath = getFolderPath();
        if (folderPath != null && isRepositoryVersionable()) {
            try {
                FileData fileData = getRepository().check(folderPath);
                if (fileData != null) {
                    return fileData.getVersion();
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return null;
    }

    protected final void setLastHistoryVersion(String lastHistoryVersion) {
        this.lastHistoryVersion = lastHistoryVersion;
    }

    public boolean isLastVersion() {
        String historyVersion = getHistoryVersion();
        if (historyVersion == null) {
            return true;
        }
        String lastVersion = getLastHistoryVersion();
        return lastVersion == null || historyVersion.equals(lastVersion);
    }

    @Override
    public List<ProjectVersion> getVersions() {
        Collection<FileData> fileDatas = getHistoryFileDatas();
        List<ProjectVersion> versions = new ArrayList<>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    public String getBusinessName() {
        String folderPath = getFolderPath();
        Repository repository = getRepository();
        if (repository.supports().mappedFolders()) {
            folderPath = ((FolderMapper) repository).getBusinessName(folderPath);
        }
        return folderPath.substring(folderPath.lastIndexOf('/') + 1);
    }

    @Override
    public int getVersionsCount() {
        return getHistoryFileDatas().size();
    }

    protected List<FileData> getHistoryFileDatas() {
        if (historyFileDatas == null) {
            try {
                String folderPath = getFolderPath();
                if (folderPath != null && isRepositoryVersionable()) {
                    historyFileDatas = getRepository().listHistory(folderPath);
                } else {
                    // File repository does not have versions
                    historyFileDatas = Collections.emptyList();
                }
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
                return Collections.emptyList();
            }
        }
        return historyFileDatas;
    }

    @Override
    public void setFileData(FileData fileData) {
        super.setFileData(fileData);
        historyFileDatas = null;
        lastHistoryVersion = null;
    }

    @Override
    public AProject getProject() {
        return this;
    }

    @Override
    public void delete() throws ProjectException {
        unlock();
        close(null);
        FileData fileData = getFileData();
        try {
            getRepository().delete(fileData);
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        }
        setFileData(null);
        setHistoryVersion(null); // In some repository types new version is created, so we must change version to latest
    }

    public void delete(CommonUser user, String comment) throws ProjectException {
        if (isDeleted()) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion.", null, getBusinessName());
        }

        unlock();
        close(user);
        FileData fileData = getFileData();

        FileData data = new FileData();
        data.setName(fileData.getName());
        data.setVersion(fileData.getVersion());
        data.setAuthor(user.getUserName());
        data.setComment(comment);
        try {
            getRepository().delete(data);
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        }
        setFileData(null);
        setHistoryVersion(null); // In some repository types new version is created, so we must change version to latest
    }

    public void close(CommonUser user) throws ProjectException {
        refresh();
    }

    public void erase(CommonUser user, String comment) throws ProjectException {
        FileData fileData = getFileData();
        FileData data = new FileData();
        data.setName(fileData.getName());
        data.setVersion(null);
        data.setAuthor(user.getUserName());
        data.setComment(comment);
        try {
            getRepository().deleteHistory(data);
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        }
    }

    public boolean isDeleted() {
        try {
            FileData fileData = getFileData();
            return fileData == null || fileData.isDeleted();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    public void undelete(CommonUser user, String comment) throws ProjectException {
        try {
            if (!isDeleted()) {
                throw new ProjectException("Cannot undelete non-marked project ''{0}''.", null, getBusinessName());
            }

            Repository repository = getRepository();
            FileData fileData = repository.check(getFileData().getName());
            if (fileData != null && fileData.isDeleted()) {
                FileData data = new FileData();
                data.setName(fileData.getName());
                data.setVersion(fileData.getVersion());
                data.setAuthor(user.getUserName());
                data.setComment(comment);
                repository.deleteHistory(data);
                FileData actual = repository.check(fileData.getName());
                setFileData(actual);
                String version = actual.getVersion();
                setLastHistoryVersion(version);
                setHistoryVersion(version);
            }
        } catch (IOException ex) {
            throw new ProjectException(ex.getMessage(), ex);
        }

    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String path = artefactPath.getStringValue();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        AProjectArtefact artefact = getArtefactsInternal().get(path);
        if (artefact == null) {
            // For backward compatibility throw exception if artefact is not found
            throw new ProjectException("Cannot find project artefact ''{0}''", null, path);
        }
        return artefact;
    }

    @Override
    protected Map<String, AProjectArtefact> createInternalArtefacts() {
        if (isFolder()) {
            return super.createInternalArtefacts();
        }

        final HashMap<String, AProjectArtefact> internalArtefacts = new HashMap<>();

        final String folderPath = getFolderPath();
        final Repository repository = getRepository();
        FileItem fileItem;
        try {
            if (isHistoric()) {
                fileItem = repository.readHistory(folderPath, getFileData().getVersion());
            } else {
                fileItem = repository.read(folderPath);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        if (fileItem == null) {
            return internalArtefacts;
        }
        try (InputStream stream = fileItem.getStream();
             ZipInputStream zipInputStream = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                FileData fileData = new FileData();
                final String artefactName = entry.getName();
                fileData.setName(folderPath + "/" + artefactName);
                String version = isHistoric() ? getFileData().getVersion() : null;
                ZipFolderRepository zipFolderRepository = new ZipFolderRepository(repository, folderPath, version);
                AProjectResource resource = new AProjectResource(getProject(), zipFolderRepository, fileData);
                internalArtefacts.put(artefactName, resource);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return internalArtefacts;
    }

    public boolean isOpenedForEditing() {
        // TODO Remove this workaround
        return false;
    }

    @Override
    public void update(AProjectArtefact newFolder, CommonUser user) throws ProjectException {
        if (!(newFolder instanceof AProject)) {
            throw new IllegalArgumentException("Cannot update not from AProject");
        }

        AProject projectFrom = (AProject) newFolder;

        Repository repositoryTo = getRepository();

        if (isFolder()) {
            if (projectFrom.isFolder()) {
                super.update(projectFrom, user);
            } else {
                setFileData(unpack(projectFrom, repositoryTo, getFolderPath(), user));
            }
        } else {
            if (!projectFrom.isFolder()) {
                if (getResourceTransformer() != null) {
                    // projectFrom will be unarchived, transformed and then archived

                    File tempFolder = null;
                    try {
                        // Unpack to temp folder
                        tempFolder = Files.createTempDirectory("openl").toFile();
                        try (FileSystemRepository tempRepository = new FileSystemRepository()) {
                            tempRepository.setRoot(tempFolder);
                            tempRepository.initialize();
                            unpack(projectFrom, tempRepository, projectFrom.getBusinessName(), user);
                            AProject tempProject = new AProject(tempRepository, projectFrom.getBusinessName());

                            transformAndArchive(tempProject, user);
                        }
                    } catch (IOException e) {
                        throw new ProjectException(e.getMessage(), e);
                    } finally {
                        FileUtils.deleteQuietly(tempFolder);
                    }
                } else {
                    // Just copy a single file
                    FileData fileData = getFileData();

                    InputStream stream = null;
                    try {
                        FileItem fileItem;
                        if (projectFrom.isHistoric()) {
                            fileItem = projectFrom.getRepository()
                                .readHistory(projectFrom.getFolderPath(), projectFrom.getFileData().getVersion());
                        } else {
                            fileItem = projectFrom.getRepository().read(projectFrom.getFolderPath());
                        }
                        fileData.setSize(fileItem.getData().getSize());
                        stream = fileItem.getStream();
                        fileData.setAuthor(user == null ? null : user.getUserName());
                        setFileData(repositoryTo.save(fileData, stream));
                    } catch (IOException ex) {
                        throw new ProjectException(ex.getMessage(), ex);
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
            } else {
                transformAndArchive(projectFrom, user);
            }
        }
    }

    private void transformAndArchive(AProject projectFrom, CommonUser user) throws ProjectException {
        // Archive the folder using zip
        FileData fileData = getFileData();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(out);

            for (AProjectArtefact artefact : projectFrom.getArtefacts()) {
                writeArtefact(zipOutputStream, artefact);
            }
            zipOutputStream.finish();

            fileData.setAuthor(user == null ? null : user.getUserName());
            fileData.setSize(out.size());
            setFileData(getRepository().save(fileData, new ByteArrayInputStream(out.toByteArray())));
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    private FileData unpack(AProject projectFrom,
            Repository repositoryTo,
            String folderTo,
            CommonUser user) throws ProjectException {
        ZipInputStream stream = null;
        try {
            FileItem fileItem;
            if (projectFrom.isHistoric()) {
                fileItem = projectFrom.getRepository()
                    .readHistory(projectFrom.getFolderPath(), projectFrom.getFileData().getVersion());
            } else {
                fileItem = projectFrom.getRepository().read(projectFrom.getFolderPath());
            }
            if (fileItem == null) {
                return getFileData();
            }
            stream = new ZipInputStream(fileItem.getStream());
            FileData fileData = getFileData();
            fileData.setAuthor(user == null ? null : user.getUserName());
            return ((FolderRepository) repositoryTo)
                .save(fileData, new FileChangesFromZip(stream, folderTo), ChangesetType.FULL);
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    private void writeArtefact(ZipOutputStream zipOutputStream, AProjectArtefact artefact) throws IOException,
                                                                                           ProjectException {
        if (artefact instanceof AProjectResource) {
            AProjectResource resource = (AProjectResource) artefact;
            zipOutputStream.putNextEntry(new ZipEntry(resource.getInternalPath()));

            ResourceTransformer transformer = getResourceTransformer();
            try (InputStream content = transformer != null ? transformer.transform(resource) : resource.getContent()) {
                IOUtils.copy(content, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        } else {
            AProjectFolder folder = (AProjectFolder) artefact;
            for (AProjectArtefact a : folder.getArtefacts()) {
                writeArtefact(zipOutputStream, a);
            }
        }
    }

    @Override
    public boolean isFolder() {
        return folderStructure != null ? folderStructure : getRepository().supports().folders();
    }

    /**
     * Override folder structure. For example FileSystemRepository by default contains projects as folders. But
     * sometimes it can contain projects as zips (See an example in FileSystemDataSource).
     */
    public void overrideFolderStructure(Boolean folderStructure) {
        this.folderStructure = folderStructure;
    }

    @Override
    public String getInternalPath() {
        // The root of the project
        return "";
    }

    @Override
    public boolean hasArtefacts() {
        return isFolder() ? super.hasArtefacts() : (getFileData().getSize() != 0);
    }

}
