package org.openl.rules.project.impl.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.ProjectException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.StateHolder;
import org.openl.util.MsgHelper;

public class StatePersistance {
    private static final Log log = LogFactory.getLog(StatePersistance.class);

    private final LocalFolderAPI project;
    private final File propertiesLocation;

    private static void createFolderThrowing(File folder) throws ProjectException {
        FolderHelper.checkOrCreateFolder(folder);
        if (!folder.exists()) {
            throw new ProjectException("Could not create properties folder ''{0}''!", null, folder.getAbsolutePath());
        }
    }

    public StatePersistance(LocalFolderAPI project, File projectLocation) {
        this.project = project;
        propertiesLocation = new File(projectLocation, FolderHelper.PROPERTIES_FOLDER);
    }

    private File getPropertiesFile(LocalFolderAPI folder) {
        return new File(propertiesLocation, folder.getArtefactPath().withoutFirstSegment().getStringValue()
                + ("/" + FolderHelper.FOLDER_PROPERTIES_FILE));
    }

    private File getPropertiesFile(LocalResourceAPI resource) {
        return new File(propertiesLocation, resource.getArtefactPath().withoutFirstSegment().getStringValue()
                + FolderHelper.RESOURCE_PROPERTIES_EXT);
    }

    public void load() {
        if (propertiesLocation.exists()) {
            loadAllStates(project);
        }
    }

    private void loadAllStates(LocalFolderAPI folder) {
        loadState(folder, getPropertiesFile(folder));

        for (LocalArtefactAPI artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                LocalFolderAPI localFolder = (LocalFolderAPI) artefact;
                loadAllStates(localFolder);
            } else {
                loadState(artefact, getPropertiesFile((LocalResourceAPI) artefact));
            }
        }
    }

    private void loadState(LocalArtefactAPI artefact, File sourceFile) {
        if (!sourceFile.isFile()) {
            return;
        }

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(sourceFile));

            StateHolder state = (StateHolder) ois.readObject();

            artefact.applyStateHolder(state);
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

    private void saveAllStates(LocalFolderAPI folder) throws ProjectException {
        saveState(folder, getPropertiesFile(folder));

        for (LocalArtefactAPI artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                LocalFolderAPI localFolder = (LocalFolderAPI) artefact;
                saveAllStates(localFolder);
            } else {
                saveState(artefact, getPropertiesFile((LocalResourceAPI) artefact));
            }
        }
    }

    private void saveState(LocalArtefactAPI artefact, File destFile) {
        File folder = destFile.getParentFile();
        if (!FolderHelper.checkOrCreateFolder(folder)) {
            String msg = MsgHelper.format("Could not create folder ''{0}''!", folder.getAbsolutePath());
            log.error(msg);
            return;
        }

        StateHolder state = artefact.getStateHolder();

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
