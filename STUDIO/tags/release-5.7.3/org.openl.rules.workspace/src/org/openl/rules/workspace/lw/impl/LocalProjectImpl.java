package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.util.MsgHelper;

public class LocalProjectImpl extends LocalProjectFolderImpl implements LocalProject {
    private static class ProjectStateHolder implements StateHolder {
        private static final long serialVersionUID = 1659670527173898554L;

        StateHolder parent;

        ProjectVersion version;
        Collection<ProjectDependency> dependencies;
    }

    private static final Log log = LogFactory.getLog(LocalProjectImpl.class);

    private static final LinkedList<ProjectDependency> EMPTY_LIST = new LinkedList<ProjectDependency>();
    private ProjectVersion version;

    private Collection<ProjectDependency> dependencies;

    private LocalWorkspaceImpl localWorkspace;

    public LocalProjectImpl(String name, ArtefactPath path, File location, LocalWorkspaceImpl localWorkspace,
            FileFilter localWorkspaceFileFilter) {
        super(name, path, location, localWorkspaceFileFilter);

        version = new RepositoryProjectVersionImpl(0, 0, 0, null);
        this.localWorkspace = localWorkspace;
    }

    public void checkedIn(ProjectVersion newVersion) {
        // update new version
        version = newVersion;
        // reset all isNew & isChanged
        resetNewAndChanged();

        try {
            save();
        } catch (ProjectException e) {
            log.error("Failed to save local project state!", e);
        }
    }

    protected void downloadArtefact(Project project) throws ProjectException {
        super.downloadArtefact(project);

        version = project.getVersion();
        dependencies = project.getDependencies();

        // reset all isNew & isChanged
        resetNewAndChanged();
    }

    public LocalProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        LocalProjectArtefact lpa = this;

        for (String s : artefactPath.getSegments()) {
            lpa = lpa.getArtefact(s);
        }

        return lpa;
    }

    public Collection<ProjectDependency> getDependencies() {
        return (dependencies == null) ? EMPTY_LIST : Collections.unmodifiableCollection(dependencies);
    }

    @Override
    public StateHolder getState() {
        ProjectStateHolder state = new ProjectStateHolder();

        state.parent = super.getState();

        state.version = version;
        state.dependencies = new ArrayList<ProjectDependency>(getDependencies());

        return state;
    }

    public ProjectVersion getVersion() {
        return version;
    }

    public synchronized void load() throws ProjectException {
        refresh();

        new StatePersistance(this).load();
    }

    @Override
    public void remove() {
        super.remove();

        localWorkspace.notifyRemoved(this);
    }

    // --- protected

    public synchronized void save() throws ProjectException {
        saveProjectState();
    }

    private void saveProjectState() throws ProjectException {
        new StatePersistance(this).save();
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) {
        this.dependencies = new ArrayList<ProjectDependency>(dependencies);
    }

    @Override
    public void setState(StateHolder aState) throws PropertyException {
        ProjectStateHolder state = (ProjectStateHolder) aState;
        super.setState(state.parent);

        version = state.version;
        dependencies = new ArrayList<ProjectDependency>(state.dependencies);
    }
}

class StatePersistance {
    private static final Log log = LogFactory.getLog(StatePersistance.class);

    final private LocalProjectImpl project;
    final private File propertiesLocation;

    private static void createFolderThrowing(File folder) throws ProjectException {
        FolderHelper.checkOrCreateFolder(folder);
        if (!folder.exists()) {
            throw new ProjectException("Could not create properties folder ''{0}''!", null, folder.getAbsolutePath());
        }
    }

    StatePersistance(LocalProjectImpl project) {
        this.project = project;
        propertiesLocation = new File(project.getLocation(), FolderHelper.PROPERTIES_FOLDER);
    }

    private File getPropertiesFile(ProjectFolder folder) {
        return new File(propertiesLocation, folder.getArtefactPath().getStringValue(1)
                + ("/" + FolderHelper.FOLDER_PROPERTIES_FILE));
    }

    private File getPropertiesFile(ProjectResource resource) {
        return new File(propertiesLocation, resource.getArtefactPath().getStringValue(1)
                + FolderHelper.RESOURCE_PROPERTIES_EXT);
    }

    void load() {
        if (propertiesLocation.exists()) {
            loadAllStates(project);
        }
    }

    private void loadAllStates(LocalProjectFolderImpl folder) {
        loadState(folder, getPropertiesFile(folder));

        for (LocalProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                LocalProjectFolderImpl localFolder = (LocalProjectFolderImpl) artefact;
                loadAllStates(localFolder);
            } else {
                LocalProjectArtefactImpl localProjectArtefact = (LocalProjectArtefactImpl) artefact;
                loadState(localProjectArtefact, getPropertiesFile((ProjectResource) localProjectArtefact));
            }
        }
    }

    private void loadState(LocalProjectArtefactImpl artefact, File sourceFile) {
        if (!sourceFile.isFile()) {
            return;
        }

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(sourceFile));

            StateHolder state = (StateHolder) ois.readObject();

            artefact.setState(state);
        } catch (Exception e) {
            String msg = MsgHelper.format("Could not read state from file ''{0}''!", sourceFile.getAbsolutePath());
            log.error(msg, e);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    void save() throws ProjectException {
        createFolderThrowing(propertiesLocation);
        saveAllStates(project);
    }

    private void saveAllStates(LocalProjectFolderImpl folder) throws ProjectException {
        saveState(folder, getPropertiesFile(folder));

        for (LocalProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                LocalProjectFolderImpl localFolder = (LocalProjectFolderImpl) artefact;
                saveAllStates(localFolder);
            } else {
                LocalProjectArtefactImpl localProjectArtefact = (LocalProjectArtefactImpl) artefact;
                saveState(localProjectArtefact, getPropertiesFile((ProjectResource) localProjectArtefact));
            }
        }
    }

    private void saveState(LocalProjectArtefactImpl artefact, File destFile) {
        File folder = destFile.getParentFile();
        if (!FolderHelper.checkOrCreateFolder(folder)) {
            String msg = MsgHelper.format("Could not create folder ''{0}''!", folder.getAbsolutePath());
            log.error(msg);
            return;
        }

        StateHolder state = artefact.getState();

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(destFile));
            oos.writeObject(state);
            oos.flush();
        } catch (IOException e) {
            String msg = MsgHelper.format("Could not save state into file ''{0}''!", destFile.getAbsolutePath());
            log.error(msg, e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
