package org.openl.rules.repository.jcr;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.CommonUserImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RRepositoryListener;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.IOUtils;

public class ZipJcrRepository implements Repository, Closeable {

    private RRepository rulesRepository;
    private String projectsPath;
    private String deploymentConfigPath;
    private String deploymentsPath;
    // In this case there is no need to store a strong reference to the listener: current field is used only to remove
    // old instance. If it's GC-ed, no need to remove it.
    private WeakReference<Object> listenerReference = new WeakReference<Object>(null);

    protected void init(RRepository rulesRepository) {
        this.rulesRepository = rulesRepository;

        try {
            projectsPath = rulesRepository.getRulesProjectsRootPath();
        } catch (RRepositoryException e) {
            throw new IllegalStateException(e);
        }

        try {
            deploymentConfigPath = rulesRepository.getDeploymentConfigRootPath();
        } catch (RRepositoryException e) {
            throw new IllegalStateException(e);
        }

        try {
            deploymentsPath = rulesRepository.getDeploymentsRootPath();
        } catch (RRepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            List<FileData> result = new ArrayList<FileData>();
            List<FolderAPI> projects;
            if (projectsPath != null && projectsPath.equals(path)) {
                projects = rulesRepository.getRulesProjects();
            } else if (deploymentConfigPath != null && deploymentConfigPath.equals(path)) {
                projects = rulesRepository.getDeploymentProjects();
            } else if (deploymentsPath != null && deploymentsPath.equals(path)) {
                List<FolderAPI> deployments = rulesRepository.getDeploymentProjects();
                for (FolderAPI deployment : deployments) {
                    for (ArtefactAPI artefactAPI : deployment.getArtefacts()) {
                        if (artefactAPI instanceof FolderAPI) {
                            result.add(createFileData(path + "/" + deployment.getName() + "/" + artefactAPI.getName(), artefactAPI));
                        }
                    }
                }
                return result;
            } else {
                ArtefactAPI artefact = rulesRepository.getArtefact(path);
                if (artefact == null) {
                    return result;
                } else if (deploymentsPath != null && path.startsWith(deploymentsPath)) {
                    projects = new ArrayList<FolderAPI>();
                    FolderAPI deploymentProject = (FolderAPI) artefact;
                    for (ArtefactAPI artefactAPI : deploymentProject.getArtefacts()) {
                        if (artefactAPI instanceof FolderAPI) {
                            projects.add((FolderAPI) artefactAPI);
                        }
                    }
                } else {
                    result.add(createFileData(path, artefact));
                    return result;
                }

            }

            for (FolderAPI project : projects) {
                result.add(createFileData(path + "/" + project.getName(), project));
            }

            return result;
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public FileData check(String name) throws IOException {
        return read(name).getData();
    }

    @Override
    public FileItem read(String name) {
        try {
            FolderAPI project;
            if (projectsPath != null && name.startsWith(projectsPath)) {
                String projectName = name.substring(projectsPath.length() + 1);
                if (!rulesRepository.hasProject(projectName)) {
                    return null;
                }
                project = rulesRepository.getRulesProject(projectName);
            } else if (deploymentConfigPath != null && name.startsWith(deploymentConfigPath)) {
                String projectName = name.substring(deploymentConfigPath.length() + 1);
                if (!rulesRepository.hasDeploymentProject(projectName)) {
                    return null;
                }
                project = rulesRepository.getDeploymentProject(projectName);
            } else if (deploymentsPath != null && name.startsWith(deploymentsPath)) {
                String projectName = name.substring(deploymentsPath.length() + 1);
                if (!rulesRepository.hasDeploymentProject(projectName)) {
                    return null;
                }
                project = rulesRepository.getDeploymentProject(projectName);
            } else {
                return null;
            }
            return createFileItem(project, createFileData(name, project));
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) {
        try {

            String name = data.getName();
            FolderAPI project = getOrCreateProject(name);

            if (undeleteIfNeeded(data, project)) {
                return createFileData(name, project);
            }

            String comment = data.getComment();
            if (comment == null) {
                comment = "";
            }
            Map<String, Object> projectProps = project.getProps();
            projectProps.put(ArtefactProperties.VERSION_COMMENT, comment);
            project.setProps(projectProps);

            List<String> newFiles = new ArrayList<String>();
            ZipInputStream zipInputStream = new ZipInputStream(stream);
            ZipEntry entry = zipInputStream.getNextEntry();
            CommonUser user = data.getAuthor() == null ? getUser() : new CommonUserImpl(data.getAuthor());
            while (entry != null) {
                if (!entry.isDirectory()) {
                    newFiles.add(entry.getName());

                    String resourceName = name + "/" + entry.getName();

                    // Workaround with byte array because jcr closes input stream
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    IOUtils.copy(zipInputStream, out);
                    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

                    ArtefactAPI artefact = rulesRepository.getArtefact(resourceName);
                    if (artefact != null) {
                        if (artefact instanceof ResourceAPI) {
                            Map<String, Object> artefactProps = artefact.getProps();
                            artefactProps.put(ArtefactProperties.VERSION_COMMENT, comment);
                            artefact.setProps(artefactProps);
                            ((ResourceAPI) artefact).setContent(in);
                            artefact.commit(user, Integer.parseInt(artefact.getVersion().getRevision()) + 1);
                        } else {
                            artefact.delete(user);
                            ResourceAPI resource = rulesRepository.createResource(resourceName, in);
                            resource.commit(user, Integer.parseInt(resource.getVersion().getRevision()) + 1);
                        }
                    } else {
                        ResourceAPI resource = rulesRepository.createResource(resourceName, in);
                        resource.commit(user, Integer.parseInt(resource.getVersion().getRevision()) + 1);
                    }
                }

                entry = zipInputStream.getNextEntry();
            }

            deleteAbsentFiles(newFiles, project, "");

            project.commit(user, Integer.parseInt(project.getVersion().getRevision()) + 1);

            return createFileData(data.getName(), project);
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean undeleteIfNeeded(FileData data, FolderAPI project) throws IOException, PropertyException {
        FileItem existingFileItem = read(data.getName());
        if (existingFileItem == null) {
            return false;
        }
        existingFileItem.getStream().close();
        FileData existingData = existingFileItem.getData();
        if (existingData.isDeleted() && !data.isDeleted()) {
            project.removeProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(String path) {
        try {
            ArtefactAPI artefact = rulesRepository.getArtefact(path);
            if (artefact == null) {
                return false;
            }
            if (artefact.hasProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION)) {
                throw new ProjectException("Project ''{0}'' is already marked for deletion!", null, path);
            }
            artefact.addProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION, ValueType.BOOLEAN, true);

            return true;
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public FileData copy(String srcPath, FileData destData) {
        try {
            if (rulesRepository.getArtefact(srcPath) == null) {
                throw new ProjectException("Project ''{0}'' is absent in the repository!", null, srcPath);
            }
            String name = destData.getName();
            if (rulesRepository.getArtefact(name) != null) {
                throw new ProjectException("Project ''{0}'' is already exist in the repository!", null, destData);
            }

            // TODO Only create
            FolderAPI srcProject = getOrCreateProject(name);
            FolderAPI destProject = getOrCreateProject(name);
            copy(srcProject, destProject);

            return createFileData(name, destProject);
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public FileData rename(String path, FileData destData) {
        try {
            String name = destData.getName();
            return createFileData(name, rulesRepository.rename(path, name));
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setListener(final Listener callback) {
        Object listener = listenerReference.get();
        if (listener != null) {
            // Remove previous listener
            if (rulesRepository instanceof JcrRepository) {
                rulesRepository.removeRepositoryListener((RRepositoryListener) listener);
                listenerReference.clear();
            } else if (rulesRepository instanceof RProductionRepository) {
                ((RProductionRepository) rulesRepository).removeListener((RDeploymentListener) listener);
                listenerReference.clear();
            }
        }

        if (callback != null) {
            if (rulesRepository instanceof JcrRepository) {
                RRepositoryListener repositoryListener = new RRepositoryListener() {
                    @Override
                    public void onEventInRulesProjects(RRepositoryEvent event) {
                        callback.onChange();
                    }

                    @Override
                    public void onEventInDeploymentProjects(RRepositoryEvent event) {
                        callback.onChange();
                    }
                };
                listenerReference = new WeakReference<Object>(repositoryListener);
                rulesRepository.addRepositoryListener(repositoryListener);
            } else if (rulesRepository instanceof RProductionRepository) {
                RDeploymentListener deploymentListener = new RDeploymentListener() {
                    @Override
                    public void onEvent() {
                        callback.onChange();
                    }
                };
                listenerReference = new WeakReference<Object>(deploymentListener);
                ((RProductionRepository) rulesRepository).addListener(deploymentListener);
            }
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        try {
            ArtefactAPI artefact = rulesRepository.getArtefact(name);
            if (artefact == null || artefact instanceof ResourceAPI) {
                return Collections.emptyList();
            }

            FolderAPI project = (FolderAPI) artefact;
            List<FileData> result = new ArrayList<FileData>();
            if (project.getVersionsCount() > 0) {
                for (ProjectVersion version : project.getVersions()) {
                    FolderAPI history = project.getVersion(version);
                    result.add(createFileData(name, history));
                }
            }
            return result;
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        return readHistory(name, version).getData();
    }

    @Override
    public FileItem readHistory(String name, String version) {
        try {
            ArtefactAPI artefact = rulesRepository.getArtefact(name);
            if (artefact == null || artefact instanceof ResourceAPI) {
                return null;
            }

            FolderAPI project = (FolderAPI) artefact;

            FolderAPI history = project.getVersion(new CommonVersionImpl(Integer.parseInt(version)));
            return createFileItem(history, createFileData(name, history));
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean deleteHistory(String name, String version) {
        try {
            ArtefactAPI artefact = rulesRepository.getArtefact(name);
            if (artefact == null) {
                return false;
            }
            if (version == null) {
                artefact.delete(getUser());

                return true;
            } else {
                // TODO implement
                return false;
            }
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) {
        try {
            if (rulesRepository.getArtefact(srcName) == null) {
                throw new ProjectException("Project ''{0}'' is absent in the repository!", null, srcName);
            }
            String name = destData.getName();
            if (rulesRepository.getArtefact(name) != null) {
                throw new ProjectException("Project ''{0}'' is already exist in the repository!", null, destData);
            }

            FolderAPI sourceProject = getOrCreateProject(srcName).getVersion(new CommonVersionImpl(Integer.parseInt(version)));
            FolderAPI destProject = getOrCreateProject(name);// TODO Only create
            copy(sourceProject, destProject);

            return createFileData(name, destProject);
        } catch (CommonException e) {
            throw new IllegalStateException(e);
        }
    }

    private CommonUser getUser() {
        // TODO Get current user
        return new CommonUserImpl("system");
    }

    private FolderAPI getOrCreateProject(String name) throws RRepositoryException {
        FolderAPI project;
        if (projectsPath != null && name.startsWith(projectsPath)) {
            String projectName = name.substring(projectsPath.length() + 1);
            if (rulesRepository.hasProject(projectName)) {
                project = rulesRepository.getRulesProject(projectName);
            } else {
                project = rulesRepository.createRulesProject(projectName);
            }
        } else if (deploymentConfigPath != null && name.startsWith(deploymentConfigPath)) {
            String projectName = name.substring(deploymentConfigPath.length() + 1);
            if (rulesRepository.hasDeploymentProject(projectName)) {
                project = rulesRepository.getDeploymentProject(projectName);
            } else {
                project = rulesRepository.createDeploymentProject(projectName);
            }
        } else if (deploymentsPath != null && name.startsWith(deploymentsPath)) {
            String projectName = name.substring(deploymentsPath.length() + 1);
            if (rulesRepository.hasDeploymentProject(projectName)) {
                project = rulesRepository.getDeploymentProject(projectName);
            } else {
                project = rulesRepository.createDeploymentProject(projectName);
            }
        } else {
            project = null;
        }
        return project;
    }

    private void deleteAbsentFiles(List<String> newFiles, FolderAPI folder, String prefix) throws ProjectException {
        for (ArtefactAPI artefact : folder.getArtefacts()) {
            if (artefact instanceof ResourceAPI) {
                if (!newFiles.contains(prefix + artefact.getName())) {
                    artefact.delete(getUser());
                }
            } else {
                deleteAbsentFiles(newFiles, (FolderAPI) artefact, prefix + artefact.getName() + "/");
            }
        }
    }

    private FileData createFileData(String name, ArtefactAPI project) throws PropertyException {
        FileData fileData = new FileData();
        fileData.setName(name);

        // TODO size
        //        if (resource instanceof JcrFileAPI) {
        //            fileData.setSize(((JcrFileAPI) resource).getSize());
        //        }

        fileData.setDeleted(project.hasProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION));

        if (project.hasProperty(ArtefactProperties.VERSION_COMMENT)) {
            Property property = project.getProperty(ArtefactProperties.VERSION_COMMENT);
            fileData.setComment(property.getString());
        }

        ProjectVersion version = project.getVersion();
        fileData.setAuthor(version.getVersionInfo().getCreatedBy());
        fileData.setModifiedAt(version.getVersionInfo().getCreatedAt());
        fileData.setVersion(String.valueOf(version.getRevision()));
        return fileData;
    }

    private FileItem createFileItem(FolderAPI project, FileData fileData) throws IOException, ProjectException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(out);
        writeFolderToZip(project, zipOutputStream, "");
        zipOutputStream.close();

        return new FileItem(fileData, new ByteArrayInputStream(out.toByteArray()));
    }

    private void writeFolderToZip(FolderAPI folder, ZipOutputStream zipOutputStream, String pathPrefix) throws
                                                                                                        IOException,
                                                                                                        ProjectException {
        Collection<? extends ArtefactAPI> artefacts = folder.getArtefacts();
        for (ArtefactAPI artefact : artefacts) {
            if (artefact instanceof ResourceAPI) {
                ZipEntry entry = new ZipEntry(pathPrefix + artefact.getName());
                zipOutputStream.putNextEntry(entry);

                InputStream content = ((ResourceAPI) artefact).getContent();
                IOUtils.copy(content, zipOutputStream);

                content.close();
                zipOutputStream.closeEntry();
            } else {
                writeFolderToZip((FolderAPI) artefact, zipOutputStream, pathPrefix + artefact.getName() + "/");
            }
        }
    }

    private void copy(FolderAPI source, FolderAPI destination) throws ProjectException {
        for (ArtefactAPI artefact : source.getArtefacts()) {
            String name = artefact.getName();
            if (artefact.isFolder()) {
                copy((FolderAPI) artefact, destination.addFolder(name));
            } else {
                destination.addResource(name, ((ResourceAPI) artefact).getContent());
            }
        }
    }

    @Override
    public void close() throws IOException {
        setListener(null);
        // If rulesRepository is not created, we don't need to create it and then release it
        if (rulesRepository != null) {
            rulesRepository.release();
        }
    }
}
