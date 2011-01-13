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

    private final LocalArtefactAPI artefact;
    private final File propertiesLocation;

    public StatePersistance(LocalArtefactAPI artefact, File projectLocation) {
        this.artefact = artefact;
        propertiesLocation = new File(projectLocation, FolderHelper.PROPERTIES_FOLDER);
    }

    private File getPropertiesFile(LocalArtefactAPI artefact) {
        if (artefact.isFolder()) {
            return new File(propertiesLocation, artefact.getArtefactPath().withoutFirstSegment().getStringValue()
                    + ("/" + FolderHelper.FOLDER_PROPERTIES_FILE));
        } else {
            return new File(propertiesLocation, artefact.getArtefactPath().withoutFirstSegment().getStringValue()
                    + FolderHelper.RESOURCE_PROPERTIES_EXT);
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
        saveState(artefact);
    }

    private void saveState(LocalArtefactAPI artefact) {
        File destFile = getPropertiesFile(artefact);
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
