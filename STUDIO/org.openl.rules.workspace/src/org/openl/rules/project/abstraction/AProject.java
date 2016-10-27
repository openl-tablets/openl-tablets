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
import org.openl.rules.project.impl.local.FolderRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;

public class AProject extends AProjectFolder {
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
            if (!isFolder()) {
                List<FileData> list;

                if (!isHistoric() || isLastVersion()) {
                    list = getRepository().list(getFolderPath());
                    if (list.size() > 1) {
                        throw new IllegalArgumentException("Found several projects for a single project path");
                    }
                    if (!list.isEmpty()) {
                        fileData = list.get(0);
                    } else {
                        fileData = new FileData();
                        fileData.setName(getFolderPath());
                        fileData.setVersion(getHistoryVersion());
                    }
                } else {
                    // TODO Don't query fileItem. Only fileData instead
                    FileItem fileItem = getRepository().readHistory(getFolderPath(), getHistoryVersion());
                    IOUtils.closeQuietly(fileItem.getStream());
                    fileData = fileItem.getData();
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

        List<FileData> fileDatas = getRepository().listHistory(getFolderPath());
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
            if (isHistoric()) {
                read = getRepository().readHistory(fileData.getName(), fileData.getVersion());
            } else {
                read = getRepository().read(fileData.getName());
            }
            InputStream stream = read.getStream();
            setFileData(getRepository().save(fileData, stream));
            IOUtils.closeQuietly(stream);
        }
        refresh();
    }

    public void edit(CommonUser user) throws ProjectException {
    }

    public void close(CommonUser user) throws ProjectException {
        refresh();
    }

    public void erase() throws ProjectException {
        if (isFolder()) {
            for (AProjectArtefact artefact : getArtefacts()) {
                if (artefact instanceof AProjectResource) {
                    getRepository().deleteHistory(artefact.getFileData().getName(), null);
                }
            }
        } else {
            getRepository().deleteHistory(getFileData().getName(), null);
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
            IOUtils.closeQuietly(stream);
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
        if (isHistoric()) {
            fileItem = repository.readHistory(folderPath, getFileData().getVersion());
        } else {
            fileItem = repository.read(folderPath);
        }
        if (fileItem == null) {
            return internalArtefacts;
        }
        ZipInputStream zipInputStream = new ZipInputStream(fileItem.getStream());
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

                InputStream stream;
                if (isHistoric()) {
                    stream = projectFrom.getRepository().readHistory(projectFrom.getFolderPath(), projectFrom.getFileData().getVersion()).getStream();
                } else {
                    stream = projectFrom.getRepository().read(projectFrom.getFolderPath()).getStream();
                }
                setFileData(getRepository().save(fileData, stream));
                IOUtils.closeQuietly(stream);
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

                    zipOutputStream.close();
                } catch (IOException e) {
                    throw new ProjectException(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(zipOutputStream);
                }

                setFileData(getRepository().save(fileData, new ByteArrayInputStream(out.toByteArray())));
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
        return getRepository() instanceof FolderRepository;
    }
}
