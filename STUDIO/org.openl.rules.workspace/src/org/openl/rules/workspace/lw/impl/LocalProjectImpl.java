package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.util.Log;

public class LocalProjectImpl extends LocalProjectFolderImpl implements LocalProject {
    private static final LinkedList<ProjectDependency> EMPTY_LIST = new LinkedList<ProjectDependency>();
    
    private ProjectVersion version;
    private Collection<ProjectDependency> dependencies;

    private LocalWorkspaceImpl localWorkspace;

    public LocalProjectImpl(String name, ArtefactPath path, File location, LocalWorkspaceImpl localWorkspace) {
        super(name, path, location);

        version = new RepositoryProjectVersionImpl(0, 0, 0, null);
        this.localWorkspace = localWorkspace;
    }

    public LocalProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        LocalProjectArtefact lpa = this;

        for (String s : artefactPath.getSegments()) {
            lpa = lpa.getArtefact(s);
        }

        return lpa;
    }

    public ProjectVersion getVersion() {
        return version;
    }

    public Collection<ProjectDependency> getDependencies() {
        return (dependencies == null) ? EMPTY_LIST : Collections.unmodifiableCollection(dependencies);
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) {
        this.dependencies = new ArrayList<ProjectDependency>(dependencies);
    }

    public synchronized void load() throws ProjectException {
        refresh();
        
        loadAllStates(this);
    }

    public synchronized void save() throws  ProjectException {
        saveAllStates(this);
    }

    public void remove() {
        super.remove();

        localWorkspace.notifyRemoved(this);
    }

    public void checkedIn(ProjectVersion newVersion) {
	// update new version
	version = newVersion;
	// reset all isNew & isChanged
	resetNewAndChanged();
	
        try {
	    save();
	} catch (ProjectException e) {
	    Log.error("Failed to save local project state", e);
	}
    }

    // --- protected

    protected void downloadArtefact(Project project) throws ProjectException {
        super.downloadArtefact(project);

        version = project.getVersion();
        dependencies = project.getDependencies();
        
	// reset all isNew & isChanged
        resetNewAndChanged();
    }

    private static void saveState(LocalProjectArtefactImpl artefact, File destFile) {
        StateHolder state = artefact.getState();

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(destFile));
            oos.writeObject(state);
            oos.flush();
        } catch (IOException e) {
            Log.error("Could not save state into file {0}", e, destFile.getAbsolutePath());
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
    
    private static void loadState(LocalProjectArtefactImpl artefact, File sourceFile) {
        if (!sourceFile.isFile()) return;

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(sourceFile));

            StateHolder state = (StateHolder) ois.readObject();

            artefact.setState(state);
        } catch (Exception e) {
            Log.error("Could not read state from file {0}", e, sourceFile.getAbsolutePath());
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

    private static void loadAllStates(LocalProjectFolderImpl folder) throws ProjectException {
        File propFolder = getPropertiesFolder(folder);
        
        loadState(folder, getFolderPropertiesFile(propFolder));
        
        for (LocalProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                loadAllStates((LocalProjectFolderImpl) artefact);
            } else {
                loadState((LocalProjectArtefactImpl)artefact, getResourcePropertiesFile(propFolder, artefact));
            }
        }
    }

    private static void saveAllStates(LocalProjectFolderImpl folder) throws ProjectException {
        File propFolder = createPropertiesFolder(folder);

        saveState(folder, getFolderPropertiesFile(propFolder));
        
        for (LocalProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                saveAllStates((LocalProjectFolderImpl) artefact);
            } else {
                saveState((LocalProjectArtefactImpl)artefact, getResourcePropertiesFile(propFolder, artefact));
            }
        }
    }

    private static File getFolderPropertiesFile(File propFolder) {
        return new File(propFolder, FOLDER_PROPERTIES_FOLDER + File.separator + FOLDER_PROPERTIES_FILE);
    }

    private static File getResourcePropertiesFile(File propFolder, LocalProjectArtefact artefact) {
        return new File(propFolder, artefact.getName() + RESOURCE_PROPERTIES_EXT);
    }

    private static File getPropertiesFolder(LocalProjectFolderImpl folder) {
        return new File(folder.getLocation(), PROPERTIES_FOLDER);
    }

    private static File createPropertiesFolder(LocalProjectFolderImpl folder) throws ProjectException {
        File propFolder = getPropertiesFolder(folder);
        if (propFolder.isFile()) {
            if (!propFolder.delete()) {
                throw new ProjectException("''{0}'' file exists in ''{1}'' directory and can not be deleted", null, PROPERTIES_FOLDER, folder.getLocation());
            }
        }

        if (!propFolder.exists()) {
            if (!propFolder.mkdirs()) {
                throw new ProjectException("Could not create properties folder in " + folder.getLocation());
            }
        }

        File folderPropFolder = new File(propFolder, FOLDER_PROPERTIES_FOLDER);
        if (!folderPropFolder.exists()) {
            if (!folderPropFolder.mkdirs()) {
                throw new ProjectException("Could not create " +  FOLDER_PROPERTIES_FOLDER + " folder in " + folderPropFolder);
            }
        }

        return propFolder;
    }

    public StateHolder getState() {
        ProjectStateHolder state = new ProjectStateHolder();
        
        state.parent = super.getState();
        
        state.version = version;
        state.dependencies = new ArrayList<ProjectDependency>(dependencies);
        
        return state;
    }
    
    public void setState(StateHolder aState) throws PropertyException {
        ProjectStateHolder state = (ProjectStateHolder) aState;
        super.setState(state.parent);
        
        version = state.version;
        dependencies = new ArrayList<ProjectDependency>(state.dependencies);
    }

    private static class ProjectStateHolder  implements StateHolder {
        private static final long serialVersionUID = 1659670527173898554L;

        StateHolder parent;
        
        ProjectVersion version;
        Collection<ProjectDependency> dependencies;
    }
}
