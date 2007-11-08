package org.openl.rules.workspace.lw.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.lw.LocalProjectFolder;
import org.openl.rules.workspace.lw.LocalProjectResource;
import org.openl.rules.workspace.props.PropertiesContainer;
import org.openl.rules.workspace.props.Property;
import org.openl.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class LocalProjectImpl extends LocalProjectFolderImpl implements LocalProject {
    private ProjectVersion version;

    private LocalWorkspaceImpl localWorkspace;

    public LocalProjectImpl(String name, ArtefactPath path, File location, ProjectVersion version, LocalWorkspaceImpl localWorkspace) {
        super(name, path, location);

        this.version = version;
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
        // TODO return valid data
        return new LinkedList<ProjectDependency>();
    }

    public void load() throws ProjectException {
        refresh();
        load(this);
    }

    public synchronized void save() throws  ProjectException {
        save(this);
    }

    public void remove() {
        super.remove();

        localWorkspace.notifyRemoved(this);
    }

    // --- protected

    protected void downloadArtefact(Project project) throws ProjectException {
        super.downloadArtefact(project);

        setNew(false);
        setChanged(false);
    }

    private static void load(LocalProjectFolderImpl folder) throws ProjectException {
        File propFolder = getPropertiesFolder(folder);
        loadProperties(folder, getFolderPropertiesFile(propFolder));

        for (LocalProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact instanceof LocalProjectFolder) {
                load((LocalProjectFolderImpl) artefact);
            } else if (artefact instanceof LocalProjectResource) {
                loadProperties(artefact, new File(propFolder, artefact.getName()));
            }
        }
    }

    private static void save(LocalProjectFolderImpl folder) throws ProjectException {
        File propFolder = createPropertiesFolder(folder);
        try {
            saveProperties(folder, getFolderPropertiesFile(propFolder));
        } catch (IOException e) {
            throw new ProjectException("could not save properties for folder {0}", e, folder.getName());
        }

        for (LocalProjectArtefact artefact  : folder.getArtefacts()) {
            if (artefact instanceof LocalProjectFolder) {
                save((LocalProjectFolderImpl) artefact);
            } else if (artefact instanceof LocalProjectResource) {
                try {
                    saveProperties(artefact, new File(propFolder, artefact.getName()));
                } catch (IOException e) {
                    throw new ProjectException("could not save properties for resource {0}", e, artefact.getName());
                }
            }
        }
    }

    private static File getFolderPropertiesFile(File propFolder) {
        return new File(propFolder, FOLDER_PROPERTIES_FOLDER+ File.separator + FOLDER_PROPERTIES_FILE);
    }

    private static File getPropertiesFolder(LocalProjectFolderImpl folder) {
        return new File(folder.getLocation(), PROPERTIES_FOLDER);
    }

    private static void saveProperties(PropertiesContainer propertiesContainer, File destFile) throws IOException {
        List<Property> properties = new ArrayList<Property>(propertiesContainer.getProperties());
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(destFile));
        oos.writeObject(properties);
        oos.flush();
        oos.close();
    }

    private static void loadProperties(PropertiesContainer propertiesContainer, File sourceFile) {
        if (sourceFile.isFile()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(sourceFile));
                List<Property> properties = (List<Property>) ois.readObject();
                for (Property p : properties) {
                    propertiesContainer.addProperty(p);
                }
            } catch (Exception e) {
                Log.error("could not read properties from file {0}", e, sourceFile.getAbsolutePath());
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException ignore) {}
                }
            }
        }
    }

    private static File createPropertiesFolder(LocalProjectFolderImpl folder) throws ProjectException {
        File propFolder = getPropertiesFolder(folder);
        if (propFolder.isFile()) {
            if (!propFolder.delete()) {
                throw new ProjectException(new StringBuilder().append(PROPERTIES_FOLDER).append(" file exists in ")
                        .append(folder.getLocation()).append(" directory and can not be deleted").toString());
            }
        }

        if (!propFolder.exists()) {
            if (!propFolder.mkdirs()) {
                throw new ProjectException("could not create properties folder in " + folder.getLocation());
            }
        }

        File folderPropFolder = new File(propFolder, FOLDER_PROPERTIES_FOLDER);
        if (!folderPropFolder.exists()) {
            if (!folderPropFolder.mkdirs()) {
                throw new ProjectException("could not create " +  FOLDER_PROPERTIES_FOLDER + " folder in " + folderPropFolder);
            }
        }

        return propFolder;
    }
}
