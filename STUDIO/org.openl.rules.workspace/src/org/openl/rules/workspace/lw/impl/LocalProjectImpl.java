package org.openl.rules.workspace.lw.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.props.Property;
import org.openl.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LocalProjectImpl extends LocalProjectFolderImpl implements LocalProject {
    private ProjectVersion version;

    private LocalWorkspaceImpl localWorkspace;

    private Collection<ProjectDependency> dependencies;

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
        return dependencies == null ? Collections.EMPTY_LIST : Collections.unmodifiableCollection(dependencies);
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) {
        this.dependencies = new ArrayList<ProjectDependency>(dependencies);
    }

    public void load() throws ProjectException {
        refresh();
        load(this, true);
    }

    public synchronized void save() throws  ProjectException {
        save(this, true);
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

    private static void load(LocalProjectFolderImpl folder, boolean project) throws ProjectException {
        File propFolder = getPropertiesFolder(folder);
        if (project) {
            loadProperties((LocalProjectImpl)folder, getFolderPropertiesFile(propFolder));
        } else {
            loadProperties(folder, getFolderPropertiesFile(propFolder));
        }

        for (LocalProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                load((LocalProjectFolderImpl) artefact, false);
            } else {
                loadProperties(artefact, new File(propFolder, artefact.getName()));
            }
        }
    }

    private static void save(LocalProjectFolderImpl folder, boolean project) throws ProjectException {
        File propFolder = createPropertiesFolder(folder);
        try {
            if (project) {
                saveProperties((LocalProjectImpl)folder, getFolderPropertiesFile(propFolder));
            } else {
                saveProperties(folder, getFolderPropertiesFile(propFolder));
            }
        } catch (IOException e) {
            throw new ProjectException("could not save properties for folder {0}", e, folder.getName());
        }

        for (LocalProjectArtefact artefact  : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                save((LocalProjectFolderImpl) artefact, false);
            } else {
                try {
                    saveProperties(artefact, new File(propFolder, artefact.getName()));
                } catch (IOException e) {
                    throw new ProjectException("could not save properties for resource {0}", e, artefact.getName());
                }
            }
        }
    }

    private static File getFolderPropertiesFile(File propFolder) {
        return new File(propFolder, FOLDER_PROPERTIES_FOLDER + File.separator + FOLDER_PROPERTIES_FILE);
    }

    private static File getPropertiesFolder(LocalProjectFolderImpl folder) {
        return new File(folder.getLocation(), PROPERTIES_FOLDER);
    }

    private static void saveProperties(LocalProjectArtefact lpa, File destFile) throws IOException {
        ArtefactStateHolder stateHolder = new ArtefactStateHolder(new ArrayList<Property>(lpa.getProperties()),
                lpa.getEffectiveDate(),
                lpa.getExpirationDate(),
                lpa.getLineOfBusiness());

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(destFile));
        oos.writeObject(stateHolder);
        oos.flush();
        oos.close();
    }

    private static void saveProperties(LocalProjectImpl lpa, File destFile) throws IOException {
        ProjectStateHolder stateHolder = new ProjectStateHolder(new ArrayList<Property>(lpa.getProperties()),
                lpa.getEffectiveDate(),
                lpa.getExpirationDate(),
                lpa.getLineOfBusiness(),
                lpa.getDependencies());

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(destFile));
        oos.writeObject(stateHolder);
        oos.flush();
        oos.close();
    }

    private static void loadProperties(LocalProjectArtefact lpa, File sourceFile) {
        if (sourceFile.isFile()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(sourceFile));
                ArtefactStateHolder stateHolder = (ArtefactStateHolder) ois.readObject();
                for (Property p : stateHolder.properties) {
                    lpa.addProperty(p);
                }
                lpa.setEffectiveDate(stateHolder.effectiveDate);
                lpa.setExpirationDate(stateHolder.expirationDate);
                lpa.setLineOfBusiness(stateHolder.LOB);
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

    private static void loadProperties(LocalProjectImpl lpa, File sourceFile) {
        if (sourceFile.isFile()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(sourceFile));
                ProjectStateHolder stateHolder = (ProjectStateHolder) ois.readObject();
                for (Property p : stateHolder.properties) {
                    lpa.addProperty(p);
                }
                lpa.setEffectiveDate(stateHolder.effectiveDate);
                lpa.setExpirationDate(stateHolder.expirationDate);
                lpa.setLineOfBusiness(stateHolder.LOB);
                lpa.setDependencies(stateHolder.dependencies);
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

    private static class ArtefactStateHolder implements Serializable {
        List<Property> properties;
        Date effectiveDate;
        Date expirationDate;
        String LOB;

        ArtefactStateHolder(List<Property> properties, Date effectiveDate, Date expirationDate, String LOB) {
            this.properties = properties;
            this.effectiveDate = effectiveDate;
            this.expirationDate = expirationDate;
            this.LOB = LOB;
        }
    }

    private static class ProjectStateHolder  extends ArtefactStateHolder {
        Collection<ProjectDependency> dependencies;
        ProjectStateHolder(List<Property> properties, Date effectiveDate, Date expirationDate,
                String LOB, Collection<ProjectDependency> dependencies) {
            super(properties, effectiveDate, expirationDate, LOB);
            this.dependencies = dependencies;
        }
    }
}
