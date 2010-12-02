package org.openl.rules.project.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.StateHolder;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.util.MsgHelper;

public class StatePersistance {
    private static class LocalArtefactStateHolder implements StateHolder {
        private static final long serialVersionUID = 1049629652852513808L;

        Date effectiveDate;
        Date expirationDate;
        String LOB;
        ProjectVersion version;
        Map<String, Object> props;

        Collection<Property> properties;
        Collection<ProjectDependency> dependencies;
    }

    private static final Log log = LogFactory.getLog(StatePersistance.class);

    private final AProject project;
    private final File propertiesLocation;

    private static void createFolderThrowing(File folder) throws ProjectException {
        FolderHelper.checkOrCreateFolder(folder);
        if (!folder.exists()) {
            throw new ProjectException("Could not create properties folder ''{0}''!", null, folder.getAbsolutePath());
        }
    }

    public StatePersistance(AProject project, File projectLocation) {
        this.project = project;
        propertiesLocation = new File(projectLocation, FolderHelper.PROPERTIES_FOLDER);
    }

    private File getPropertiesFile(AProjectFolder folder) {
        return new File(propertiesLocation, folder.getArtefactPath().withoutFirstSegment().getStringValue()
                + ("/" + FolderHelper.FOLDER_PROPERTIES_FILE));
    }

    private File getPropertiesFile(AProjectResource resource) {
        return new File(propertiesLocation, resource.getArtefactPath().withoutFirstSegment().getStringValue()
                + FolderHelper.RESOURCE_PROPERTIES_EXT);
    }

    public void load() {
        if (propertiesLocation.exists()) {
            loadAllStates(project);
        }
    }

    private void loadAllStates(AProjectFolder folder) {
        loadState(folder, getPropertiesFile(folder));

        for (AProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                AProjectFolder localFolder = (AProjectFolder) artefact;
                loadAllStates(localFolder);
            } else {
                loadState(artefact, getPropertiesFile((AProjectResource) artefact));
            }
        }
    }

    private void loadState(AProjectArtefact artefact, File sourceFile) {
        if (!sourceFile.isFile()) {
            return;
        }

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(sourceFile));

            StateHolder state = (StateHolder) ois.readObject();

            applyState(artefact, (LocalArtefactStateHolder) state);
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

    public void save() throws ProjectException {
        createFolderThrowing(propertiesLocation);
        saveAllStates(project);
    }

    private void saveAllStates(AProjectFolder folder) throws ProjectException {
        saveState(folder, getPropertiesFile(folder));

        for (AProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                AProjectFolder localFolder = (AProjectFolder) artefact;
                saveAllStates(localFolder);
            } else {
                saveState(artefact, getPropertiesFile((AProjectResource) artefact));
            }
        }
    }

    private void saveState(AProjectArtefact artefact, File destFile) {
        File folder = destFile.getParentFile();
        if (!FolderHelper.checkOrCreateFolder(folder)) {
            String msg = MsgHelper.format("Could not create folder ''{0}''!", folder.getAbsolutePath());
            log.error(msg);
            return;
        }

        StateHolder state = getState(artefact);

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

    private void applyState(AProjectArtefact artefact, LocalArtefactStateHolder state) throws ProjectException {
        artefact.setEffectiveDate(state.effectiveDate);
        artefact.setExpirationDate(state.expirationDate);
        artefact.setLineOfBusiness(state.LOB);
        //FIXME
        ((LocalAPI)artefact.getAPI()).setCurrentVersion(state.version);
        artefact.setProps(state.props);

        for (Property property : state.properties) {
            try {
                artefact.addProperty(property);
            } catch (PropertyException e) {
                throw new ProjectException("Failed to add properties", e);
            }
        }
        if (artefact instanceof AProject) {
            ((AProject) artefact).setDependencies(state.dependencies);
        }
    }

    private StateHolder getState(AProjectArtefact artefact) {
        LocalArtefactStateHolder state = new LocalArtefactStateHolder();

        state.effectiveDate = artefact.getEffectiveDate();
        state.expirationDate = artefact.getExpirationDate();
        state.LOB = artefact.getLineOfBusiness();
        state.version = artefact.getVersion();
        state.props = artefact.getProps();

        state.properties = new ArrayList<Property>();
        state.properties.addAll(artefact.getProperties());
        if (artefact instanceof AProject) {
            state.dependencies = ((AProject) artefact).getDependencies();
        }

        return state;
    }

}
