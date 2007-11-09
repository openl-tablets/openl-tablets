package org.openl.rules.workspace.lw.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.lw.LocalProjectFolder;
import org.openl.rules.workspace.lw.LocalProjectResource;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.FilenameFilter;

public class LocalProjectFolderImpl extends LocalProjectArtefactImpl implements LocalProjectFolder {
    public static final String PROPERTIES_FOLDER = ".studioProps";
    public static final String FOLDER_PROPERTIES_FOLDER = "folder-props";
    public static final String FOLDER_PROPERTIES_FILE = "folder.properties";

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
            throw new ProjectException("Cannot find project artefact ''{0}''", null, name);
        }

        return lpa;
    }

    public LocalProjectFolder addFolder(String name) throws ProjectException {
        if (artefacts.get(name) != null) {
            throw new ProjectException("Artefact with name ''{0}'' already exists!", null, name);
        }
        
        File f = FolderHelper.generateSubLocation(getLocation(), name);
        if (!FolderHelper.checkOrCreateFolder(f)) {
            throw new ProjectException("Failed to create folder ''{0}''!", null, f.getAbsolutePath());
        }

        ArtefactPath ap = getArtefactPath().add(name);
        LocalProjectFolderImpl newFolder = new LocalProjectFolderImpl(name, ap, f);

        addArtefact(newFolder);
        return newFolder;
    }
    
    public LocalProjectResource addResource(String name, ProjectResource resource) throws ProjectException {
        if (artefacts.get(name) != null) {
            throw new ProjectException("Artefact with name ''{0}'' already exists!", null, name);
        }

        File f = FolderHelper.generateSubLocation(getLocation(), name);

        ArtefactPath ap = getArtefactPath().add(name);
        LocalProjectResourceImpl newResource = new LocalProjectResourceImpl(name, ap, f);
        newResource.downloadArtefact(resource);

        addArtefact(newResource);
        return newResource;
    }

    public void refresh() {
        File[] files = getLocation().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !PROPERTIES_FOLDER.equals(name);
            }
        });

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
                artefacts.remove(name);

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
        FolderHelper.clearFolder(getLocation());

        // remove itself
        super.remove();
    }

    public boolean isFolder() {
        return true;
    }
    
    public boolean hasArtefact(String name) {
        return (artefacts.get(name) != null);
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

        addArtefact(newArtefact);
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

            if (pa.isFolder()) {
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
    
    // --- private

    private void addArtefact(LocalProjectArtefactImpl newArtefact) {
        newArtefact.setChanged(false);
        newArtefact.setNew(true);

        artefacts.put(newArtefact.getName(), newArtefact);
        setChanged(true);
    }
}
