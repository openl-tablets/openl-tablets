package org.openl.rules.workspace.uw.impl;

import java.util.Collection;
import java.util.LinkedList;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.dtr.RepositoryProjectFolder;
import org.openl.rules.workspace.dtr.RepositoryProjectResource;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.lw.LocalProjectFolder;
import org.openl.rules.workspace.lw.LocalProjectResource;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspaceProjectResource;

public class UserWorkspaceProjectFolderImpl extends UserWorkspaceProjectArtefactImpl implements UserWorkspaceProjectFolder {
    private LocalProjectFolder localFolder;
    private RepositoryProjectFolder dtrFolder;
    
    protected UserWorkspaceProjectFolderImpl(UserWorkspaceProjectImpl project, LocalProjectFolder localFolder, RepositoryProjectFolder dtrFolder) {
        super(project, localFolder, dtrFolder);

        updateArtefact(localFolder, dtrFolder);
    }

    public UserWorkspaceProjectFolder addFolder(String name) throws ProjectException {
        if (isLocal()) {
            LocalProjectFolder local = localFolder.addFolder(name);
            // remote ?
            return wrapFolder(local, null);
        } else {
            throw new ProjectException("Cannot add folder ''{0}'' in read only mode", null, name);
        }
    }

    public UserWorkspaceProjectResource addResource(String name, ProjectResource resource) throws ProjectException {
        if (isLocal()) {
            LocalProjectResource local = localFolder.addResource(name, resource);
            // remote ?
            return wrapFile(local, null);
        } else {
            throw new ProjectException("Cannot add resource ''{0}'' in read only mode", null, name);
        }
    }

    public Collection<? extends UserWorkspaceProjectArtefact> getArtefacts() {
        LinkedList<UserWorkspaceProjectArtefact> result = new LinkedList<UserWorkspaceProjectArtefact>();

        if (isLocal()) {
            for (LocalProjectArtefact lpa : localFolder.getArtefacts()) {
                result.add(wrapLocalArtefact(lpa));
            }
        } else {
            for (RepositoryProjectArtefact rpa : dtrFolder.getArtefacts()) {
                result.add(wrapRepositoryArtefact(rpa));
            }
        }
        
        return result;
    }

    public UserWorkspaceProjectArtefact getArtefact(String name) throws ProjectException {
        if (isLocal()) {
            return wrapLocalArtefact(localFolder.getArtefact(name));
        } else {
            return wrapRepositoryArtefact(dtrFolder.getArtefact(name));
        }
    }
    
    public void delete() throws ProjectException {
        if (!isLocal()) {
            throw new ProjectException("Can modify local resource only!", null);
        }

        localFolder.remove();
    }

    // --- protected
    
    protected void updateArtefact(LocalProjectFolder localFolder, RepositoryProjectFolder dtrFolder) {
        super.updateArtefact(localFolder, dtrFolder);

        this.localFolder = localFolder;
        this.dtrFolder = dtrFolder;
    }
    
    protected UserWorkspaceProjectFolder wrapFolder(LocalProjectFolder local, RepositoryProjectFolder remote) {
        return new UserWorkspaceProjectFolderImpl(getProject(), local, remote);
    }
    
    protected UserWorkspaceProjectResource wrapFile(LocalProjectResource local, RepositoryProjectResource remote) {
        return new UserWorkspaceProjectResourceImpl(getProject(), local, remote);
    }
    
    protected UserWorkspaceProjectArtefact wrapLocalArtefact(LocalProjectArtefact lpa) {
        RepositoryProjectArtefact rpa = null;
        try {
            if (dtrFolder != null) {
                rpa = dtrFolder.getArtefact(lpa.getName());
            }
        } catch (ProjectException e) {
            // ignore
        }        
        if (lpa instanceof ProjectFolder) {
            if (!(rpa instanceof ProjectFolder)) rpa = null;
            return wrapFolder((LocalProjectFolder)lpa, (RepositoryProjectFolder)rpa);
        } else {
            if (rpa instanceof ProjectFolder) rpa = null;
            return wrapFile((LocalProjectResource)lpa, (RepositoryProjectResource)rpa);
        }
    }
    
    protected UserWorkspaceProjectArtefact wrapRepositoryArtefact(RepositoryProjectArtefact rpa) {
        if (rpa instanceof ProjectFolder) {
            return wrapFolder(null, (RepositoryProjectFolder)rpa);
        } else {
            return wrapFile(null, (RepositoryProjectResource)rpa);
        }
    }
}
