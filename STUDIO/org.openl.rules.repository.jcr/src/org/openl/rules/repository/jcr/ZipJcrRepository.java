package org.openl.rules.repository.jcr;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonUserImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RRepositoryListener;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class ZipJcrRepository implements Repository, Closeable {
    private final Logger log = LoggerFactory.getLogger(ZipJcrRepository.class);

    private RRepository rulesRepository;
    private String designPath;
    private Node defRulesLocation;
    private String deployConfigPath;
    private Node defDeploymentConfigLocation;
    private String deployPath;
    private Node deployLocation;
    // In this case there is no need to store a strong reference to the listener: current field is used only to remove
    // old instance. If it's GC-ed, no need to remove it.
    private WeakReference<Object> listenerReference = new WeakReference<Object>(null);

    protected void init(Session session, boolean designRepositoryMode) throws RRepositoryException, RepositoryException {
        if (designRepositoryMode) {
            designPath = "DESIGN/rules";
            defRulesLocation = getNode(session, designPath);
            deployConfigPath = "DESIGN/deployments";
            defDeploymentConfigLocation = getNode(session, deployConfigPath);
            rulesRepository = new JcrRepository(session, defRulesLocation, defDeploymentConfigLocation);
        } else {
            deployPath = "deploy";
            deployLocation = getNode(session, deployPath);
            rulesRepository = new JcrProductionRepository(session, deployLocation);
        }
    }

    private Node getNode(Session session, String aPath) throws RepositoryException {
        Node node = session.getRootNode();
        String[] paths = aPath.split("/");
        for (String path : paths) {
            if (path.length() == 0) {
                continue; // first element (root folder) or illegal path
            }

            if (node.hasNode(path)) {
                // go deeper
                node = node.getNode(path);
            } else {
                // create new
                node = node.addNode(path);
            }
        }
        if (node.isNew()) {
            session.save();
        }

        return node;
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            List<FileData> result = new ArrayList<FileData>();
            List<FolderAPI> projects;
            if (designPath != null && designPath.equals(path)) {
                projects = getRulesProjects();
            } else if (deployConfigPath != null && deployConfigPath.equals(path)) {
                projects = getDeployConfigs();
            } else if (deployPath != null && deployPath.equals(path)) {
                List<FolderAPI> deployments = getDeploys();
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
                } else if (deployPath != null && path.startsWith(deployPath)) {
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
            throw new IOException(e);
        }
    }

    private List<FolderAPI> getRulesProjects() throws RRepositoryException {
        NodeIterator ni;
        try {
            ni = defRulesLocation.getNodes();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot get any rules project", e);
        }

        LinkedList<FolderAPI> result = new LinkedList<FolderAPI>();
        while (ni.hasNext()) {
            Node n = ni.nextNode();
            try {
                if (!n.isNodeType(JcrNT.NT_LOCK)) {
                    result.add(new JcrFolderAPI(n, new ArtefactPathImpl(new String[]{n.getName()})));
                }
            } catch (RepositoryException e) {
                log.debug("Failed to add rules project.");
            }
        }

        return result;
    }

    private List<FolderAPI> getDeployConfigs() throws RRepositoryException {
        NodeIterator ni;
        try {
            ni = defDeploymentConfigLocation.getNodes();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot get any deployment project", e);
        }

        LinkedList<FolderAPI> result = new LinkedList<FolderAPI>();
        while (ni.hasNext()) {
            Node n = ni.nextNode();
            try {
                if (!n.isNodeType(JcrNT.NT_LOCK)) {
                    result.add(new JcrFolderAPI(n, new ArtefactPathImpl(new String[]{n.getName()})));
                }
            } catch (RepositoryException e) {
                log.debug("Failed to add deployment project.");
            }
        }

        return result;
    }

    private List<FolderAPI> getDeploys() throws RRepositoryException {
        List<FolderAPI> result = new ArrayList<FolderAPI>();
        try {
            NodeIterator iterator = deployLocation.getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                if (node.getPrimaryNodeType().getName().equals(JcrNT.NT_APROJECT)) {
                    result.add(new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{node.getName()})));
                }
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to enumerate deployments", e);
        }
        return result;
    }


    @Override
    public FileData check(String name) throws IOException {
        FileItem fileItem = read(name);
        if (fileItem == null) {
            return null;
        }
        IOUtils.closeQuietly(fileItem.getStream());
        return fileItem.getData();
    }

    @Override
    public FileItem read(String name) throws IOException {
        try {
            FolderAPI project;
            if (designPath != null && name.startsWith(designPath)) {
                String projectName = name.substring(designPath.length() + 1);
                if (!(defRulesLocation.hasNode(projectName) && !defRulesLocation.getNode(projectName).isNodeType(JcrNT.NT_LOCK))) {
                    return null;
                }
                project = getRulesProject(projectName);
            } else if (deployConfigPath != null && name.startsWith(deployConfigPath)) {
                String projectName = name.substring(deployConfigPath.length() + 1);
                if (!defDeploymentConfigLocation.hasNode(projectName)) {
                    return null;
                }
                project = getDeployConfig(projectName);
            } else if (deployPath != null && name.startsWith(deployPath)) {
                String projectName = name.substring(deployPath.length() + 1);
                if (!deployLocation.hasNode(projectName)) {
                    return null;
                }
                project = getDeploy(projectName);
            } else {
                return null;
            }
            return createFileItem(project, createFileData(name, project));
        } catch (CommonException e) {
            throw new IOException(e);
        } catch (RepositoryException e) {
            throw new IOException(e);
        }
    }

    private FolderAPI getRulesProject(String name) throws RRepositoryException {
        try {
            if (!defRulesLocation.hasNode(name)) {
                throw new RRepositoryException("Cannot find project ''{0}''", null, name);
            }

            Node n = defRulesLocation.getNode(name);
            return new JcrFolderAPI(n, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get project ''{0}''", e, name);
        }
    }

    private FolderAPI getDeployConfig(String name) throws RRepositoryException {
        try {
            if (!defDeploymentConfigLocation.hasNode(name)) {
                throw new RRepositoryException("Cannot find Project ''{0}''.", null, name);
            }

            Node n = defDeploymentConfigLocation.getNode(name);
            return new JcrFolderAPI(n, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get DDProject ''{0}''.", e, name);
        }
    }

    private FolderAPI getDeploy(String name) throws RRepositoryException {
        Node node;
        try {
            node = deployLocation.getNode(name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to get node", e);
        }

        try {
            return new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to wrap JCR node", e);
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
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
            TreeSet<String> folderPaths = new TreeSet<String>();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    newFiles.add(entry.getName());

                    String resourceName = name + "/" + entry.getName();
                    String path = entry.getName();
                    addFolderPaths(folderPaths, path);

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
                        } else {
                            artefact.delete(user);
                            artefact = rulesRepository.createResource(resourceName, in);
                        }
                    } else {
                        artefact = rulesRepository.createResource(resourceName, in);
                    }
                    artefact.commit(user, Integer.parseInt(artefact.getVersion().getRevision()) + 1);
                }

                entry = zipInputStream.getNextEntry();
            }

            deleteAbsentFiles(newFiles, project, "");

            Iterator<String> foldersIterator = folderPaths.descendingIterator();
            while (foldersIterator.hasNext()) {
                String folder = foldersIterator.next();
                ArtefactAPI artefact = rulesRepository.getArtefact(name + "/" + folder);
                artefact.commit(user, Integer.parseInt(artefact.getVersion().getRevision()) + 1);
            }

            project.commit(user, Integer.parseInt(project.getVersion().getRevision()) + 1);

            return createFileData(data.getName(), project);
        } catch (CommonException e) {
            throw new IOException(e);
        }
    }

    private void addFolderPaths(TreeSet<String> folderPaths, String path) {
        int slashIndex = path.lastIndexOf('/');
        if (slashIndex > -1) {
            String folderPath = path.substring(0, slashIndex);
            folderPaths.add(folderPath);
            addFolderPaths(folderPaths, folderPath);
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
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public FileData copy(String srcPath, FileData destData) throws IOException {
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
            throw new IOException(e);
        }
    }

    @Override
    public FileData rename(String path, FileData destData) throws IOException {
        try {
            String name = destData.getName();
            return createFileData(name, rulesRepository.rename(path, name));
        } catch (CommonException e) {
            throw new IOException(e);
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
            throw new IOException(e);
        }
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        FileItem fileItem = readHistory(name, version);
        if (fileItem == null) {
            return null;
        }
        IOUtils.closeQuietly(fileItem.getStream());
        return fileItem.getData();
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        if (version == null) {
            return read(name);
        }
        try {
            ArtefactAPI artefact = rulesRepository.getArtefact(name);
            if (artefact == null || artefact instanceof ResourceAPI) {
                return null;
            }

            FolderAPI project = (FolderAPI) artefact;

            FolderAPI history = project.getVersion(new CommonVersionImpl(Integer.parseInt(version)));
            return createFileItem(history, createFileData(name, history));
        } catch (CommonException e) {
            throw new IOException(e);
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
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
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
            throw new IOException(e);
        }
    }

    private CommonUser getUser() {
        // TODO Get current user
        return new CommonUserImpl("system");
    }

    private FolderAPI getOrCreateProject(String name) throws RRepositoryException {
        FolderAPI project;
        try {
        if (designPath != null && name.startsWith(designPath)) {
            String projectName = name.substring(designPath.length() + 1);
            if (defRulesLocation.hasNode(projectName) && !defRulesLocation.getNode(projectName).isNodeType(JcrNT.NT_LOCK)) {
                project = getRulesProject(projectName);
            } else {
                project = createRulesProject(projectName);
            }
        } else if (deployConfigPath != null && name.startsWith(deployConfigPath)) {
            String projectName = name.substring(deployConfigPath.length() + 1);
            if (defDeploymentConfigLocation.hasNode(projectName)) {
                project = getDeployConfig(projectName);
            } else {
                project = rulesRepository.createDeploymentProject(projectName);
            }
        } else if (deployPath != null && name.startsWith(deployPath)) {
            String projectName = name.substring(deployPath.length() + 1);
            if (deployLocation.hasNode(projectName)) {
                project = getDeploy(projectName);
            } else {
                project = rulesRepository.createDeploymentProject(projectName);
            }
        } else {
            project = null;
        }
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to check project {0}", e, name);
        }
        return project;
    }

    private FolderAPI createRulesProject(String name) throws RRepositoryException {
        try {
            Node node = NodeUtil.createNode(defRulesLocation, name,
                    JcrNT.NT_APROJECT, true);
            defRulesLocation.save();
            node.checkin();
            return new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to create rules project.", e);
        }
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
        boolean hasEntries = writeFolderToZip(project, zipOutputStream, "");
        // In java 6 zip must have at least one entry
        if (hasEntries) {
            zipOutputStream.close();
        }

        return new FileItem(fileData, new ByteArrayInputStream(out.toByteArray()));
    }

    private boolean writeFolderToZip(FolderAPI folder, ZipOutputStream zipOutputStream, String pathPrefix) throws
                                                                                                        IOException,
                                                                                                        ProjectException {
        Collection<? extends ArtefactAPI> artefacts = folder.getArtefacts();
        boolean hasEntries = false;
        for (ArtefactAPI artefact : artefacts) {
            if (artefact instanceof ResourceAPI) {
                ZipEntry entry = new ZipEntry(pathPrefix + artefact.getName());
                zipOutputStream.putNextEntry(entry);

                InputStream content = ((ResourceAPI) artefact).getContent();
                IOUtils.copy(content, zipOutputStream);

                content.close();
                zipOutputStream.closeEntry();
                hasEntries = true;
            } else {
                boolean hasFolderEntries = writeFolderToZip((FolderAPI) artefact, zipOutputStream, pathPrefix + artefact.getName() + "/");
                hasEntries = hasEntries || hasFolderEntries;
            }
        }

        return hasEntries;
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
