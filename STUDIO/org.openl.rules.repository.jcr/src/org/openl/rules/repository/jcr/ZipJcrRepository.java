package org.openl.rules.repository.jcr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.openl.rules.common.CommonException;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.ValueType;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonUserImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipJcrRepository implements Repository, Closeable, EventListener {
    private final Logger log = LoggerFactory.getLogger(ZipJcrRepository.class);

    private Session session;
    private Listener listener;
    // In this case there is no need to store a strong reference to the listener: current field is used only to remove
    // old instance. If it's GC-ed, no need to remove it.

    protected void init(Session session) throws RepositoryException {
        this.session = session;
        int eventTypes = Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED | Event.NODE_REMOVED;
        String[] nodeTypeName = { JcrNT.NT_COMMON_ENTITY };
        session.getWorkspace()
            .getObservationManager()
            .addEventListener(this, eventTypes, "/", true, null, nodeTypeName, false);
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            Node node = checkFolder(session.getRootNode(), path, false);
            List<FileData> result = new ArrayList<>();
            if (node == null) {
                return result;
            }
            List<FolderAPI> projects = getChildren(node);

            if (path.equals("deploy")) {
                for (FolderAPI deployment : projects) {
                    for (ArtefactAPI artefactAPI : deployment.getArtefacts()) {
                        if (artefactAPI instanceof FolderAPI) {
                            result.add(createFileData(path + "/" + deployment.getName() + "/" + artefactAPI.getName(),
                                artefactAPI));
                        }
                    }
                }
                return result;
            } else {
                for (FolderAPI project : projects) {
                    result.add(createFileData(path + "/" + project.getName(), project));
                }
            }

            return result;
        } catch (CommonException | RepositoryException e) {
            throw new IOException(e);
        }
    }

    private List<FolderAPI> getChildren(Node root) throws RRepositoryException {
        NodeIterator ni;
        try {
            ni = root.getNodes();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot get children nodes", e);
        }

        LinkedList<FolderAPI> result = new LinkedList<>();
        while (ni.hasNext()) {
            Node n = ni.nextNode();
            try {
                if (!n.isNodeType(JcrNT.NT_LOCK)) {
                    result.add(new JcrFolderAPI(n, new ArtefactPathImpl(new String[] { n.getName() })));
                }
            } catch (RepositoryException e) {
                log.debug("Failed to get a child node.", e);
            }
        }

        return result;
    }

    @Override
    public FileData check(String name) throws IOException {
        try {
            FolderAPI project = getOrCreateProject(name, false);
            if (project == null) {
                return null;
            }
            return createFileData(name, project);
        } catch (CommonException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileItem read(String name) throws IOException {
        try {
            FolderAPI project = getOrCreateProject(name, false);
            if (project == null) {
                return null;
            }
            return createFileItem(project, createFileData(name, project));
        } catch (CommonException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        try {
            String name = data.getName();
            FolderAPI project = getOrCreateProject(name, true);

            String comment = data.getComment();
            if (comment == null) {
                comment = "";
            }
            Map<String, Object> projectProps = project.getProps();
            projectProps.put(ArtefactProperties.VERSION_COMMENT, comment);
            project.setProps(projectProps);

            List<String> newFiles = new ArrayList<>();
            ZipInputStream zipInputStream = new ZipInputStream(stream);
            ZipEntry entry = zipInputStream.getNextEntry();
            CommonUser user = data.getAuthor() == null ? getUser() : new CommonUserImpl(data.getAuthor());
            TreeSet<String> folderPaths = new TreeSet<>();
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

                    ArtefactAPI artefact = getArtefact(resourceName);
                    if (artefact != null) {
                        if (artefact instanceof ResourceAPI) {
                            Map<String, Object> artefactProps = artefact.getProps();
                            artefactProps.put(ArtefactProperties.VERSION_COMMENT, comment);
                            artefact.setProps(artefactProps);
                            ((ResourceAPI) artefact).setContent(in);
                        } else {
                            artefact.delete(user);
                            artefact = createResource(resourceName, in);
                        }
                    } else {
                        artefact = createResource(resourceName, in);
                    }
                    artefact.commit(user, Integer.parseInt(project.getVersion().getRevision()) + 1);
                }

                entry = zipInputStream.getNextEntry();
            }

            deleteAbsentFiles(newFiles, project, "", user);

            Iterator<String> foldersIterator = folderPaths.descendingIterator();
            while (foldersIterator.hasNext()) {
                String folder = foldersIterator.next();
                ArtefactAPI artefact = getArtefact(name + "/" + folder);
                artefact.commit(user, Integer.parseInt(project.getVersion().getRevision()) + 1);
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

    @Override
    public boolean delete(FileData data) {
        try {
            String path = data.getName();
            ArtefactAPI artefact = getArtefact(path);
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
    public void setListener(final Listener callback) {
        this.listener = callback;
    }

    @Override
    public void onEvent(EventIterator events) {
        while (listener != null && events.hasNext()) {
            try {
                listener.onChange();
                break;
            } catch (Exception e) {
                log.error("onEvent", e);
            }
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        try {
            ArtefactAPI artefact = getArtefact(name);
            if (artefact == null || artefact instanceof ResourceAPI) {
                return Collections.emptyList();
            }

            FolderAPI project = (FolderAPI) artefact;
            List<FileData> result = new ArrayList<>();
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
        if (version == null) {
            return check(name);
        }
        try {
            ArtefactAPI artefact = getArtefact(name);
            if (artefact == null || artefact instanceof ResourceAPI) {
                return null;
            }

            FolderAPI project = (FolderAPI) artefact;

            FolderAPI history = project.getVersion(new CommonVersionImpl(Integer.parseInt(version)));
            return createFileData(name, history);
        } catch (CommonException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        if (version == null) {
            return read(name);
        }
        try {
            ArtefactAPI artefact = getArtefact(name);
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
    public boolean deleteHistory(FileData data) {
        String name = data.getName();
        String version = data.getVersion();

        try {
            ArtefactAPI artefact = getArtefact(name);
            if (artefact == null) {
                return false;
            }
            if (version == null) {
                CommonUser user = data.getAuthor() == null ? getUser() : new CommonUserImpl(data.getAuthor());
                artefact.delete(user);

                return true;
            } else {
                if (!artefact.hasProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION)) {
                    throw new ProjectException(
                        "Project ''{0}'' isn't marked for deletion. The method deleteHistory() in JCR Repository is supported only for undelete and erase.",
                        null,
                        name);
                }
                artefact.removeProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION);
                return true;
            }
        } catch (CommonException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        FileItem fileItem = null;
        try {
            fileItem = readHistory(srcName, version);

            FileData copy = new FileData();
            copy.setName(destData.getName());
            copy.setComment(destData.getComment());
            copy.setAuthor(destData.getAuthor());
            copy.setSize(fileItem.getData().getSize());

            return save(copy, fileItem.getStream());
        } finally {
            if (fileItem != null) {
                IOUtils.closeQuietly(fileItem.getStream());
            }
        }
    }

    @Override
    public Features supports() {
        return new Features(this);
    }

    private CommonUser getUser() {
        // TODO Get current user
        return new CommonUserImpl("system");
    }

    private FolderAPI getOrCreateProject(String name, boolean create) throws RRepositoryException,
                                                                      FileNotFoundException {
        FolderAPI project;
        try {
            Node root = session.getRootNode();
            Node n = checkFolder(root, name, false);
            if (n != null) {
                project = new JcrFolderAPI(n,
                    new ArtefactPathImpl(new String[] { name.substring(name.lastIndexOf("/") + 1) }));
            } else if (create) {
                project = createArtifact(root, name);
            } else {
                project = null;
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get an artifact ''{0}''", e, name);
        }
        return project;
    }

    private FolderAPI createArtifact(Node root, String path) throws RRepositoryException {
        try {
            int lastSeparator = path.lastIndexOf("/");
            Node parent;
            String name;
            if (lastSeparator >= 0) {
                String folder = path.substring(0, lastSeparator);
                name = path.substring(lastSeparator + 1);
                parent = checkFolder(root, folder, true);
            } else {
                name = path;
                parent = root;
            }
            Node node = NodeUtil.createNode(parent, name, JcrNT.NT_APROJECT, true);
            root.save();
            node.checkin();
            return new JcrFolderAPI(node, new ArtefactPathImpl(new String[] { name }));
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to create an artifact ''{0}''", e, path);
        }
    }

    private void deleteAbsentFiles(List<String> newFiles,
            FolderAPI folder,
            String prefix,
            CommonUser user) throws ProjectException {
        for (ArtefactAPI artefact : folder.getArtefacts()) {
            if (artefact instanceof ResourceAPI) {
                if (!newFiles.contains(prefix + artefact.getName())) {
                    artefact.delete(user);
                }
            } else {
                deleteAbsentFiles(newFiles, (FolderAPI) artefact, prefix + artefact.getName() + "/", user);
            }
        }
    }

    private FileData createFileData(String name, ArtefactAPI project) throws PropertyException {
        FileData fileData = new FileData();
        fileData.setName(name);

        // It's impossible to calculate zip size if project contains artefacts
        if (((FolderAPI) project).getArtefacts().isEmpty()) {
            fileData.setSize(0);
        }

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

    private void writeFolderToZip(FolderAPI folder,
            ZipOutputStream zipOutputStream,
            String pathPrefix) throws IOException, ProjectException {
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

    private Node checkFolder(Node root, String aPath, boolean create) throws RepositoryException {
        Node node = root;
        String[] paths = aPath.split("/");
        for (String path : paths) {
            if (path.length() == 0) {
                continue; // first element (root folder) or illegal path
            }

            if (node.hasNode(path)) {
                // go deeper
                node = node.getNode(path);
            } else if (create) {
                // create new
                Node n = NodeUtil.createNode(node, path, JcrNT.NT_FOLDER, true);
                node.save();
                n.save();
                node = n;
            } else {
                return null;
            }
        }

        return node;
    }

    protected Session getSession() {
        return session;
    }

    private ArtefactAPI getArtefact(String name) {
        try {
            Node node = checkFolder(session.getRootNode(), name, false);
            if (node == null) {
                return null;
            }

            return createArtefactAPI(node, name);
        } catch (RepositoryException e) {
            log.debug("Cannot get artefact " + name, e);
            return null;
        }
    }

    private ResourceAPI createResource(String name, InputStream inputStream) throws RRepositoryException {
        try {
            String folderPath = name.substring(0, name.lastIndexOf("/"));
            Node node = checkFolder(session.getRootNode(), folderPath, true);
            ArtefactAPI artefact = createArtefactAPI(node, folderPath);
            if (!(artefact instanceof FolderAPI)) {
                throw new RepositoryException("Incorrect node type");
            }

            FolderAPI folder = (FolderAPI) artefact;
            return folder.addResource(name.substring(name.lastIndexOf("/") + 1), inputStream);
        } catch (RepositoryException | ProjectException e) {
            throw new RRepositoryException("Cannot add resource " + name, e);
        }
    }

    private ArtefactAPI createArtefactAPI(Node node, String name) throws RepositoryException {
        if (node.isNodeType(JcrNT.NT_LOCK)) {
            log.error("Incorrect node type " + JcrNT.NT_LOCK);
            return null;
        } else {
            ArtefactPathImpl path = new ArtefactPathImpl(name.split("/"));
            if (node.isNodeType(JcrNT.NT_FILE)) {
                return new JcrFileAPI(node, path);
            } else {
                return new JcrFolderAPI(node, path);
            }
        }
    }

    @Override
    public void close() {
        setListener(null);
        // Session can be null if failed to initialize repository. But this method can still be called during
        // finalization.
        if (session != null) {
            try {
                Workspace workspace = session.getWorkspace();
                ObservationManager manager = workspace.getObservationManager();
                manager.removeEventListener(this);
            } catch (RepositoryException e) {
                log.debug("release", e);
            }

            if (session.isLive()) {
                session.logout();
            }
        }
    }
}
