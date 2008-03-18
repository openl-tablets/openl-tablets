package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.dtr.RepositoryProjectFolder;
import org.openl.rules.workspace.dtr.RepositoryProjectResource;
import org.openl.util.Log;

public class RepositoryProjectFolderImpl extends RepositoryProjectArtefactImpl implements RepositoryProjectFolder {
    private RFolder rulesFolder;
    
    protected RepositoryProjectFolderImpl(RFolder rulesFolder, ArtefactPath path) {
        super(rulesFolder, path);
        
        this.rulesFolder = rulesFolder;
    }

    RepositoryProjectFolderImpl(RProject rulesProject, RFolder rulesFolder, ArtefactPath path) {
        super(rulesProject, path);
        
        this.rulesFolder = rulesFolder;
    }

    public RepositoryProjectArtefact getArtefact(String name) throws ProjectException {
        RRepositoryException rre = null;

        try {
            for (RFolder f : rulesFolder.getFolders()) {
                if (name.equals(f.getName())) {
                    return wrapFolder(f);
                }
            }
            for (RFile f : rulesFolder.getFiles()) {
                if (name.equals(f.getName())) {
                    return wrapFile(f);
                }
            }
        } catch (RRepositoryException e) {
            rre = e;
        }
        
        throw new ProjectException("Cannot find project artefact ''{0}''", rre, name);
    }

    public Collection<RepositoryProjectArtefact> getArtefacts() {
        List<RepositoryProjectArtefact> result = new LinkedList<RepositoryProjectArtefact>();

        try {
            for (RFolder rf : rulesFolder.getFolders()) {
                RepositoryProjectFolder folder = wrapFolder(rf);

                result.add(folder);
            }
            for (RFile rf : rulesFolder.getFiles()) {
                RepositoryProjectResource resource = wrapFile(rf);

                result.add(resource);
            }
        } catch (RRepositoryException e) {
            // TODO throw exception?
            Log.error("Cannot get artefacts", e);
        }
        
        return result;
        
//        return new LinkedList<RepositoryProjectArtefact>();
    }
    
    public void update(ProjectArtefact srcArtefact) throws ProjectException {
        ProjectFolder srcFolder = (ProjectFolder) srcArtefact;
        super.update(srcArtefact);
        
        // remove absent
        for (RepositoryProjectArtefact rpa : getArtefacts()) {
            String name = rpa.getName();

            if (!srcFolder.hasArtefact(name)) {
        	// was deleted
        	rpa.delete();
            } else {
                ProjectArtefact artefact = srcFolder.getArtefact(name);

                if (rpa.isFolder() == artefact.isFolder()) {
                    // update existing
                    rpa.update(artefact);
                } else {
                    // the same name but other type
                    rpa.delete();
                }
            }
        }
        
        // add new
        for (ProjectArtefact pa : srcFolder.getArtefacts()) {
            String name = pa.getName();
            
            if (!hasArtefact(name)) {
                // absent ?
                addArtefact(pa);
            }
        }
    }

    public void delete() throws ProjectException {
        try {
            rulesFolder.delete();
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to delete project folder ''{0}''", e, getArtefactPath().getStringValue());
        }        
    }

    public boolean hasArtefact(String name) {
        try {
            getArtefact(name);
            return true;
        } catch (ProjectException e) {
            return false;
        }        
    }

    public boolean isFolder() {
        return true;
    }

    // --- private
    
    private RepositoryProjectFolder wrapFolder(RFolder folder) {
        ArtefactPath ap = getArtefactPath().withSegment(folder.getName());
        
        return new RepositoryProjectFolderImpl(folder, ap);
    }

    private RepositoryProjectResource wrapFile(RFile file) {
        ArtefactPath ap = getArtefactPath().withSegment(file.getName());
        
        return new RepositoryProjectResourceImpl(file, ap);
    }
    
    private void addArtefact(ProjectArtefact artefact) throws ProjectException {
        try {
            if (artefact.isFolder()) {
                // folder
                ProjectFolder folder = (ProjectFolder) artefact;
                RFolder rf = rulesFolder.createFolder(folder.getName());
                wrapFolder(rf).update(folder);
            } else {
                // resource
                ProjectResource res = (ProjectResource) artefact;
                RFile rf = rulesFolder.createFile(res.getName());
                wrapFile(rf).update(res);
            }
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to update artefact ''{0}''", e, getArtefactPath().getStringValue());
        }        
    }
}
