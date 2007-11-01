package org.openl.rules.lw.impl;

import org.openl.rules.lw.LocalProjectFolder;
import org.openl.rules.lw.LocalProjectArtefact;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.commons.projects.ProjectFolder;
import org.openl.rules.commons.projects.ProjectArtefact;
import org.openl.rules.commons.projects.ProjectResource;
import org.openl.rules.commons.artefacts.ArtefactPath;
import org.openl.rules.commons.Utils;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

public class LocalProjectFolderImpl extends LocalProjectArtefactImpl implements LocalProjectFolder {
    private Map<String, LocalProjectArtefact> artefacts;

    public LocalProjectFolderImpl(String name, ArtefactPath path, File location) {
        super(name, path, location);

        artefacts = new HashMap<String, LocalProjectArtefact>();
    }


    public Collection<LocalProjectArtefact> getArtefacts() {
        return artefacts.values();
    }

    public LocalProjectArtefact getArtefact(String name) throws ProjectException {
        LocalProjectArtefact lpa = artefacts.get(name);
        if (lpa == null) {
            throw new ProjectException("Cannot find project artefact ''{0}''", name);
        }

        return lpa;
    }

    public void refresh() {
        File[] files = getLocation().listFiles();

        HashMap<String, File> fileMap = new HashMap<String, File>();
        for (File f : files) {
            fileMap.put(f.getName(), f);
        }

        // check deleted
        for (LocalProjectArtefact lpa : artefacts.values()) {
            String name = lpa.getName();
            if (!fileMap.containsKey(name)) {
                // was deleted
                lpa.remove();

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
                boolean isFolder = lpa instanceof LocalProjectFolder;
                if (f.isDirectory() != isFolder) {
                    // folder->file or file->folder
                    lpa.remove();

                    addAsNew(f);
                } else {
                    // ok
                    lpa.refresh();
                }
            }
        }
    }

    public void remove() {
        // remove artefacts
        for(LocalProjectArtefact lpa : artefacts.values()) {
            lpa.remove();
        }

        artefacts.clear();

        // clean up the rest (if any)
        Utils.clearFolder(getLocation());

        // remove itself
        super.remove();
    }

    // --- protected

    protected void addAsNew(File f) {
        String name = f.getName();
        ArtefactPath ap = getArtefactPath().add(name);

        LocalProjectArtefactImpl newArtefact;
        if (f.isDirectory()) {
            // folder
            newArtefact = new LocalProjectFolderImpl(name, ap, f);
        } else {
            // file
            newArtefact = new LocalProjectResourceImpl(name, ap, f);
        }

        newArtefact.refresh();

        newArtefact.setChanged(false);
        newArtefact.setNew(true);

        artefacts.put(name, newArtefact);
        setChanged(true);
    }

    protected void downloadArtefact(ProjectFolder folder) throws ProjectException {
        File location = getLocation();
        if (!location.exists()) {
            if (!location.mkdirs()) {
                // TODO exception?
            }
        }

        for(ProjectArtefact pa : folder.getArtefacts()) {
            String name = pa.getName();
            ArtefactPath ap = getArtefactPath().add(name);
            File f = new File(location, name);

            if (pa instanceof ProjectFolder) {
                ProjectFolder pf = (ProjectFolder) pa;

                LocalProjectFolderImpl lpfi = new LocalProjectFolderImpl(name, ap, f);
                lpfi.downloadArtefact(pf);

                artefacts.put(name, lpfi);
            } else {
                ProjectResource pr = (ProjectResource) pa;

                LocalProjectResourceImpl lpri = new LocalProjectResourceImpl(name, ap, f);
                lpri.downloadArtefact(pr);

                artefacts.put(name, lpri);
            }
        }

        setNew(false);
        setChanged(false);
    }
}
