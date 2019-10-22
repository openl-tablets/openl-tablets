package org.openl.rules.project.impl.local;

import java.io.*;

import org.openl.rules.common.ProjectException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.StateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatePersistance {
    private final Logger log = LoggerFactory.getLogger(StatePersistance.class);

    private final LocalArtefactAPI artefact;
    private final File propertiesLocation;

    public StatePersistance(LocalArtefactAPI artefact, File projectLocation) {
        this.artefact = artefact;
        propertiesLocation = new File(projectLocation, FolderHelper.PROPERTIES_FOLDER);
    }

    private File getPropertiesFile(LocalArtefactAPI artefact) {
        if (artefact.isFolder()) {
            return new File(propertiesLocation,
                artefact.getArtefactPath()
                    .withoutFirstSegment()
                    .getStringValue() + (File.separator + FolderHelper.FOLDER_PROPERTIES_FILE));
        } else {
            return new File(propertiesLocation,
                artefact.getArtefactPath()
                    .withoutFirstSegment()
                    .getStringValue() + FolderHelper.RESOURCE_PROPERTIES_EXT);
        }
    }

    public void load() {
        if (propertiesLocation.exists()) {
            loadState(artefact);
        }
    }

    private void loadState(LocalArtefactAPI artefact) {
        File sourceFile = getPropertiesFile(artefact);
        if (!sourceFile.isFile()) {
            return;
        }

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(sourceFile)));

            StateHolder state = (StateHolder) ois.readObject();

            artefact.applyStateHolder(state);
        } catch (Exception e) {
            log.error("Could not read state from file ''{}''.", sourceFile.getAbsolutePath(), e);
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
        saveState(artefact);
    }

    /**
     * Checks if state of the artefact was saved.
     *
     * @return <code>true</code> if state of the artefact was previously saved and <code>false</code> otherwise.
     */
    public boolean isStateSaved() {
        return getPropertiesFile(artefact).exists();
    }

    private void saveState(LocalArtefactAPI artefact) {
        File destFile = getPropertiesFile(artefact);
        File folder = destFile.getParentFile();
        if (!FolderHelper.checkOrCreateFolder(folder)) {
            log.error("Could not create folder ''{}''.", folder.getAbsolutePath());
            return;
        }

        StateHolder state = artefact.getStateHolder();

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)));
            oos.writeObject(state);
            oos.flush();
        } catch (IOException e) {
            log.error("Could not save state into file ''{}''.", destFile.getAbsolutePath(), e);
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
