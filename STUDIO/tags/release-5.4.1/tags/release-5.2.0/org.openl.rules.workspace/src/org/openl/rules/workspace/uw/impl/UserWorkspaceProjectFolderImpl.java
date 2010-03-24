package org.openl.rules.workspace.uw.impl;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.workspace.abstracts.ProjectException;
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

public class UserWorkspaceProjectFolderImpl extends UserWorkspaceProjectArtefactImpl implements
        UserWorkspaceProjectFolder {
    private static final Log log = LogFactory.getLog(UserWorkspaceProjectFolderImpl.class);

    private LocalProjectFolder localFolder;
    private RepositoryProjectFolder dtrFolder;

    protected UserWorkspaceProjectFolderImpl(UserWorkspaceProjectImpl project, LocalProjectFolder localFolder,
            RepositoryProjectFolder dtrFolder) {
        super(project, localFolder, dtrFolder);

        updateArtefact(localFolder, dtrFolder);
    }

    public UserWorkspaceProjectFolder addFolder(String name) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot add folder ''{0}'' in read only mode", null, name);
        } else {
            // check(PRIVILEGE_EDIT);

            LocalProjectFolder local = localFolder.addFolder(name);
            // remote ?
            return wrapFolder(local, null);
        }
    }

    public UserWorkspaceProjectResource addResource(String name, ProjectResource resource) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot add resource ''{0}'' in read only mode", null, name);
        } else {
            // check(PRIVILEGE_EDIT);

            LocalProjectResource local = localFolder.addResource(name, resource);
            // remote ?
            return wrapFile(local, null);
        }
    }

    public void delete() throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot delete in read only mode!", null);
        }

        // check(PRIVILEGE_EDIT);

        localFolder.remove();
    }

    public UserWorkspaceProjectArtefact getArtefact(String name) throws ProjectException {
        if (isLocal()) {
            return wrapLocalArtefact(localFolder.getArtefact(name));
        } else {
            return wrapRepositoryArtefact(dtrFolder.getArtefact(name));
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

    public boolean hasArtefact(String name) {
        if (isLocal()) {
            return localFolder.hasArtefact(name);
        } else {
            return dtrFolder.hasArtefact(name);
        }
    }

    public boolean isFolder() {
        return true;
    }

    // --- protected

    protected void updateArtefact(LocalProjectFolder localFolder, RepositoryProjectFolder dtrFolder) {
        super.updateArtefact(localFolder, dtrFolder);

        this.localFolder = localFolder;
        this.dtrFolder = dtrFolder;
    }

    protected UserWorkspaceProjectResource wrapFile(LocalProjectResource local, RepositoryProjectResource remote) {
        return new UserWorkspaceProjectResourceImpl(getProject(), local, remote);
    }

    protected UserWorkspaceProjectFolder wrapFolder(LocalProjectFolder local, RepositoryProjectFolder remote) {
        return new UserWorkspaceProjectFolderImpl(getProject(), local, remote);
    }

    protected UserWorkspaceProjectArtefact wrapLocalArtefact(LocalProjectArtefact lpa) {
        RepositoryProjectArtefact rpa = null;
        boolean isRemoteFolder = false;
        try {
            if (dtrFolder != null && dtrFolder.hasArtefact(lpa.getName())) {
                rpa = dtrFolder.getArtefact(lpa.getName());
                isRemoteFolder = rpa.isFolder();
            }
        } catch (ProjectException e) {
            // ignore
            log.error("wrapLocalArtefact", e);
        }

        if (lpa.isFolder()) {
            if (!isRemoteFolder) {
                rpa = null;
            }
            return wrapFolder((LocalProjectFolder) lpa, (RepositoryProjectFolder) rpa);
        } else {
            if (isRemoteFolder) {
                rpa = null;
            }
            return wrapFile((LocalProjectResource) lpa, (RepositoryProjectResource) rpa);
        }
    }

    protected UserWorkspaceProjectArtefact wrapRepositoryArtefact(RepositoryProjectArtefact rpa) {
        if (rpa.isFolder()) {
            return wrapFolder(null, (RepositoryProjectFolder) rpa);
        } else {
            return wrapFile(null, (RepositoryProjectResource) rpa);
        }
    }
}
