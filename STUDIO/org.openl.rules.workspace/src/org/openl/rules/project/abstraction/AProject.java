package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;
import org.openl.util.RuntimeExceptionWrapper;

public class AProject extends AProjectFolder {
    /**
     * true if the project has a folder structure and false if the project is stored as a zip
     */
    private boolean folderStructure;

    public AProject(Repository repository, String folderPath, boolean folderStructure) {
        this(repository, folderPath, null, folderStructure);
    }

    public AProject(Repository repository, String folderPath, String historyVersion, boolean folderStructure) {
        super(null, repository, folderPath, historyVersion);
        this.folderStructure = folderStructure;
    }

    public AProject(Repository repository, FileData fileData, boolean folderStructure) {
        super(null, repository, fileData.getName(), fileData.getVersion());
        this.folderStructure = folderStructure;
        setFileData(fileData);
    }

    @Override
    public FileData getFileData() {
        FileData fileData = super.getFileData();
        if (fileData == null) {
            if (!isFolder()) {
                try {
                if (!isHistoric() || isLastVersion()) {
                    FileItem fileItem = getRepository().read(getFolderPath());
                    if (fileItem != null) {
                        IOUtils.closeQuietly(fileItem.getStream());
                        fileData = fileItem.getData();
                    } else {
                        fileData = new FileData();
                        fileData.setName(getFolderPath());
                        fileData.setVersion(getHistoryVersion());
                    }
                } else {
                    FileItem fileItem = getRepository().readHistory(getFolderPath(), getHistoryVersion());
                    IOUtils.closeQuietly(fileItem.getStream());
                    fileData = fileItem.getData();
                }
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            } else {
                fileData = new FileData();
                fileData.setName(getFolderPath());
                fileData.setVersion(getHistoryVersion());
            }
            setFileData(fileData);
        }
        return fileData;
    }

    private boolean isLastVersion() {
        if (getHistoryVersion() == null) {
            return true;
        }

        List<FileData> fileDatas;
        try {
            fileDatas = getRepository().listHistory(getFolderPath());
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        return fileDatas.isEmpty() || getHistoryVersion().equals(fileDatas.get(fileDatas.size() - 1).getVersion());
    }

    @Override
    public AProject getProject() {
        return this;
    }

    @Override
    public void delete() throws ProjectException {
        if (isFolder()) {
            for (AProjectArtefact artefact : getArtefacts()) {
                artefact.delete();
            }
        } else {
            close(null);
            super.delete();
        }
        setFileData(null);
    }

    public void delete(CommonUser user) throws ProjectException {
        if (isDeleted()) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion!", null, getName());
        }

        close(null);
        super.delete();
        setFileData(null);
    }

    public void save(CommonUser user) throws ProjectException {
        if (!isFolder()) {
            FileData fileData = getFileData();
            FileItem read;
            InputStream stream = null;
            try {
            if (isHistoric()) {
                read = getRepository().readHistory(fileData.getName(), fileData.getVersion());
            } else {
                read = getRepository().read(fileData.getName());
            }
            stream = read.getStream();
            setFileData(getRepository().save(fileData, stream));
            } catch (IOException ex) {
                throw new ProjectException("Project cannot be saved", ex);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        refresh();
    }

    public void close(CommonUser user) throws ProjectException {
        refresh();
    }

    public void erase() throws ProjectException {
        if (isFolder()) {
            for (AProjectArtefact artefact : getArtefacts()) {
                if (artefact instanceof AProjectResource) {
                    if (!getRepository().deleteHistory(artefact.getFileData().getName(), null)) {
                        throw new ProjectException("Can't erase project because it is absent or can't be deleted");
                    }
                }
            }
        } else {
            if (!getRepository().deleteHistory(getFileData().getName(), null)) {
                throw new ProjectException("Can't erase project because it is absent or can't be deleted");
            }
        }
    }

    public boolean isDeleted() {
        if (isFolder()) {
            for (AProjectArtefact artefact : getArtefacts()) {
                if (artefact instanceof AProjectResource) {
                    if (!artefact.getFileData().isDeleted()) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return getFileData().isDeleted();
        }
    }

    public void undelete() throws ProjectException {
        try {
        if (!isDeleted()) {
            throw new ProjectException("Cannot undelete non-marked project ''{0}''!", null, getName());
        }

        if (isFolder()) {
            for (AProjectArtefact artefact : getArtefacts()) {
                if (artefact instanceof AProjectResource) {
                    FileData fileData = artefact.getFileData();
                    fileData.setDeleted(false);

                    FileItem read = getRepository().read(fileData.getName());
                    InputStream stream = read.getStream();
                    getRepository().save(fileData, stream);
                    IOUtils.closeQuietly(stream);
                }
            }
        } else {
            FileData fileData = getFileData();
            fileData.setDeleted(false);
            InputStream stream = getRepository().read(fileData.getName()).getStream();
            setFileData(getRepository().save(fileData, stream));
            setHistoryVersion(getFileData().getVersion());
            IOUtils.closeQuietly(stream);
        }
        } catch (IOException ex) {
            throw new ProjectException("Cannot undelete a project", ex);
        }

    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        return getArtefactsInternal().get(artefactPath.getStringValue());
    }

    @Override
    protected Map<String, AProjectArtefact> createInternalArtefacts() {
        if (isFolder()) {
            return super.createInternalArtefacts();
        }

        final HashMap<String, AProjectArtefact> internalArtefacts = new HashMap<String, AProjectArtefact>();

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
        InputStream stream = fileItem.getStream();
        if (stream == null) {
            return internalArtefacts;
        }
        ZipInputStream zipInputStream = new ZipInputStream(stream);
        try {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                FileData fileData = new FileData();
                final String artefactName = entry.getName();
                fileData.setName(folderPath + "/" + artefactName);
                String version = isHistoric() ? getFileData().getVersion() : null;
                ContentHandler contentHandler = new LazyZipContentHandler(repository, folderPath, artefactName, version);
                AProjectResource resource = new AProjectResource(getProject(), repository, fileData, contentHandler);
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
            throw new IllegalArgumentException("Can't update not from AProject");
        }

        AProject projectFrom = (AProject) newFolder;

        if (isFolder()) {
            if (newFolder.isFolder()) {
                super.update(newFolder, user);
            } else {
                // TODO Optimize copying to reduce using of LazyZipContentHandler
                super.update(newFolder, user);
            }
        } else {
            if (!projectFrom.isFolder()) {
                // Just copy a single file
                FileData fileData = getFileData();

                InputStream stream = null;
                try {
                if (isHistoric()) {
                    stream = projectFrom.getRepository().readHistory(projectFrom.getFolderPath(), projectFrom.getFileData().getVersion()).getStream();
                } else {
                    stream = projectFrom.getRepository().read(projectFrom.getFolderPath()).getStream();
                }
                fileData.setAuthor(user.getUserName());
                setFileData(getRepository().save(fileData, stream));
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Can't update not from AProject", ex);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            } else {
                // Archive the folder using zip
                FileData fileData = getFileData();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = null;
                try {
                    zipOutputStream = new ZipOutputStream(out);

                    for (AProjectArtefact artefact : projectFrom.getArtefacts()) {
                        writeArtefact(zipOutputStream, artefact);
                    }

                    fileData.setAuthor(user.getUserName());
                    setFileData(getRepository().save(fileData, new ByteArrayInputStream(out.toByteArray())));
                } catch (IOException e) {
                    throw new ProjectException(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(zipOutputStream);
                }

            }
        }
    }

    private void writeArtefact(ZipOutputStream zipOutputStream, AProjectArtefact artefact) throws
                                                                                           IOException,
                                                                                           ProjectException {
        if ((artefact instanceof AProjectResource)) {
            AProjectResource resource = (AProjectResource) artefact;
            String name = resource.getArtefactPath().withoutFirstSegment().getStringValue();
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            ZipEntry entry = new ZipEntry(name);
            zipOutputStream.putNextEntry(entry);

            InputStream content = resource.getContent();
            IOUtils.copy(content, zipOutputStream);

            content.close();
            zipOutputStream.closeEntry();
        } else {
            AProjectFolder folder = (AProjectFolder) artefact;
            for (AProjectArtefact a : folder.getArtefacts()) {
                writeArtefact(zipOutputStream, a);
            }
        }
    }

    @Override
    public boolean isFolder() {
        return folderStructure;
    }

    protected void setFolderStructure(boolean folderStructure) {
        this.folderStructure = folderStructure;
    }
}
