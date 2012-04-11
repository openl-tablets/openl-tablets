package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.lw.LocalProjectFolder;
import org.openl.rules.workspace.lw.LocalProjectResource;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.util.MsgHelper;

public class LocalProjectFolderImpl extends LocalProjectArtefactImpl implements LocalProjectFolder {
    private static final Log log = LogFactory.getLog(LocalProjectFolderImpl.class);

    private Map<String, LocalProjectArtefact> artefacts;

    private boolean isPendingRefresh;

    private FileFilter localWorkSpaceFileFilter;

    public LocalProjectFolderImpl(String name, ArtefactPath path, File location, FileFilter localWorkspaceFileFilter) {
        super(name, path, location);
        localWorkSpaceFileFilter = localWorkspaceFileFilter;

        artefacts = new HashMap<String, LocalProjectArtefact>();
        isPendingRefresh = true;
    }

    private void addArtefact(LocalProjectArtefactImpl newArtefact) {
        newArtefact.setChanged(false);
        newArtefact.setNew(true);

        artefacts.put(newArtefact.getName(), newArtefact);
        setChanged(true);
    }

    protected void addAsNew(File f) {
        String name = f.getName();
        ArtefactPath ap = getArtefactPath().withSegment(name);

        LocalProjectArtefactImpl newArtefact;
        if (f.isDirectory()) {
            // folder
            newArtefact = new LocalProjectFolderImpl(name, ap, f, localWorkSpaceFileFilter);
        } else {
            // file
            newArtefact = new LocalProjectResourceImpl(name, ap, f);
        }

        newArtefact.refresh();

        addArtefact(newArtefact);
    }

    public LocalProjectFolder addFolder(String name) throws ProjectException {
        // refresh if needed
        checkPendingRefresh();

        if (artefacts.get(name) != null) {
            throw new ProjectException("Artefact with name ''{0}'' already exists!", null, name);
        }

        File f = FolderHelper.generateSubLocation(getLocation(), name);
        if (!FolderHelper.checkOrCreateFolder(f)) {
            throw new ProjectException("Failed to create folder ''{0}''!", null, f.getAbsolutePath());
        }

        ArtefactPath ap = getArtefactPath().withSegment(name);
        LocalProjectFolderImpl newFolder = new LocalProjectFolderImpl(name, ap, f, localWorkSpaceFileFilter);

        addArtefact(newFolder);
        return newFolder;
    }

    public LocalProjectResource addResource(String name, ProjectResource resource) throws ProjectException {
        // refresh if needed
        checkPendingRefresh();

        if (artefacts.get(name) != null) {
            throw new ProjectException("Artefact with name ''{0}'' already exists!", null, name);
        }

        File f = FolderHelper.generateSubLocation(getLocation(), name);

        ArtefactPath ap = getArtefactPath().withSegment(name);
        LocalProjectResourceImpl newResource = new LocalProjectResourceImpl(name, ap, f);
        newResource.downloadArtefact(resource);

        addArtefact(newResource);
        return newResource;
    }

    private void checkPendingRefresh() {
        if (!isPendingRefresh) {
            return; // most likely all is OK
        }

        realRefresh();
    }

    protected void downloadArtefact(ProjectFolder folder) throws ProjectException {
        super.downloadArtefact(folder);

        File location = getLocation();
        if (!location.exists()) {
            if (!location.mkdirs()) {
                // TODO exception?
                String msg = MsgHelper.format("Cannot create folder ''{0}''!", location.getAbsolutePath());
                log.error(msg);
            }
        }

        for (ProjectArtefact pa : folder.getArtefacts()) {
            String name = pa.getName();
            ArtefactPath ap = getArtefactPath().withSegment(name);
            File f = new File(location, name);

            if (pa.isFolder()) {
                ProjectFolder pf = (ProjectFolder) pa;

                LocalProjectFolderImpl lpfi = new LocalProjectFolderImpl(name, ap, f, localWorkSpaceFileFilter);
                lpfi.downloadArtefact(pf);

                artefacts.put(name, lpfi);
            } else {
                ProjectResource pr = (ProjectResource) pa;

                LocalProjectResourceImpl lpri = new LocalProjectResourceImpl(name, ap, f);
                lpri.downloadArtefact(pr);

                artefacts.put(name, lpri);
            }
        }
    }

    public LocalProjectArtefact getArtefact(String name) throws ProjectException {
        // refresh if needed
        checkPendingRefresh();

        LocalProjectArtefact lpa = artefacts.get(name);
        if (lpa == null) {
            throw new ProjectException("Cannot find project artefact ''{0}''", null, name);
        }

        return lpa;
    }

    public Collection<LocalProjectArtefact> getArtefacts() {
        // refresh if needed
        checkPendingRefresh();

        return artefacts.values();
    }

    // --- protected

    @Override
    public StateHolder getState() {
        return super.getState();
    }

    public boolean hasArtefact(String name) {
        // refresh if needed
        checkPendingRefresh();

        return (artefacts.get(name) != null);
    }

    public boolean isFolder() {
        return true;
    }

    // --- private

    private void realRefresh() {
        isPendingRefresh = false;

        File[] files = getLocation().listFiles(localWorkSpaceFileFilter);

        HashMap<String, File> fileMap = new HashMap<String, File>();
        for (File f : files) {
            fileMap.put(f.getName(), f);
        }

        // check deleted
        for (Iterator<Map.Entry<String, LocalProjectArtefact>> it = artefacts.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, LocalProjectArtefact> entry = it.next();
            LocalProjectArtefact lpa = entry.getValue();
            String name = lpa.getName();
            if (!fileMap.containsKey(name)) {
                // was deleted
                lpa.remove();
                it.remove();

                setChanged(true);
            }
        }

        // check added & refresh
        for (File f : files) {
            String name = f.getName();
            LocalProjectArtefact lpa = artefacts.get(name);

            if (lpa == null) {
                // was added
                addAsNew(f);
            } else {
                if (f.isDirectory() != lpa.isFolder()) {
                    // folder->file or file->folder
                    lpa.remove();

                    addAsNew(f);
                } else {
                    // OK, go deeper
                    lpa.refresh();
                }
            }
        }
    }

    public void refresh() {
        // defer refresh till it is really needed
        isPendingRefresh = true;
    }

    @Override
    public void remove() {
        // remove artefacts
        for (LocalProjectArtefact lpa : artefacts.values()) {
            lpa.remove();
        }

        artefacts.clear();

        // clean up the rest (if any)
        FolderHelper.clearFolder(getLocation());

        // remove itself
        super.remove();
    }

    @Override
    protected void resetNewAndChanged() {

        for (LocalProjectArtefact artefact : artefacts.values()) {
            ((LocalProjectArtefactImpl) artefact).resetNewAndChanged();
        }

        super.resetNewAndChanged();
    }

    @Override
    public void setState(StateHolder aState) throws PropertyException {
        super.setState(aState);
    }
}
