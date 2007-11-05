package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.dtr.RepositoryProjectFolder;
import org.openl.rules.workspace.dtr.RepositoryProjectResource;
import org.openl.util.Log;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

    /** @deprecated */
    public RepositoryProjectFolderImpl(String name, ArtefactPath path) {
        super(name, path);
    }

    public RepositoryProjectArtefact getArtefact(String name) throws ProjectException {
        throw new ProjectException("Cannot find project artefact ''{0}''", null, name);
    }

    public Collection<RepositoryProjectArtefact> getArtefacts() {
        List<RepositoryProjectArtefact> result = new LinkedList<RepositoryProjectArtefact>();

        try {
            for (RFolder rf : rulesFolder.getFolders()) {
                RepositoryProjectFolder folder = initFolder(rf);

                result.add(folder);
            }
            for (RFile rf : rulesFolder.getFiles()) {
                RepositoryProjectResource resource = initFile(rf);

                result.add(resource);
            }
        } catch (RRepositoryException e) {
            // TODO throw exception?
            Log.error("Cannot get artefacts", e);
        }
        
        return result;
        
//        return new LinkedList<RepositoryProjectArtefact>();
    }
    
    // --- private
    
    private RepositoryProjectFolder initFolder(RFolder folder) {
        ArtefactPath ap = new ArtefactPathImpl(getArtefactPath());
        ap.add(folder.getName());
        
        return new RepositoryProjectFolderImpl(folder, ap);
    }

    private RepositoryProjectResource initFile(RFile file) {
        ArtefactPath ap = new ArtefactPathImpl(getArtefactPath());
        ap.add(file.getName());
        
        return new RepositoryProjectResourceImpl(file, ap);
    }
}
