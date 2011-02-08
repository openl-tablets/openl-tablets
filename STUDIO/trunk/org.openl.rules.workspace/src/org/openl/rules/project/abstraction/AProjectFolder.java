package org.openl.rules.project.abstraction;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.api.ResourceAPI;

public class AProjectFolder extends AProjectArtefact {
    private Map<String, AProjectArtefact> artefacts;

    public AProjectFolder(FolderAPI api, AProject project) {
        super(api, project);
    }

    @Override
    public FolderAPI getAPI() {
        return (FolderAPI) super.getAPI();
    }

    public AProjectArtefact getArtefact(String name) throws ProjectException {
        AProjectArtefact artefact = getArtefactsInternal().get(name);
        if (artefact == null) {
            throw new ProjectException("Cannot find project artefact ''{0}''", null, name);
        }

        return artefact;
    }

    public boolean hasArtefact(String name) {
        return getArtefactsInternal().containsKey(name);
    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        AProjectArtefact artefact = this;
        for (String s : artefactPath.getSegments()) {
            if (artefact.isFolder()) {
                artefact = ((AProjectFolder) artefact).getArtefact(s);
            } else {
                throw new ProjectException("Wrong path!");
            }
        }
        return artefact;
    }
    
    public AProjectFolder addFolder(String name) throws ProjectException {
        AProjectFolder createdFolder = new AProjectFolder(getAPI().addFolder(name), getProject());
        getArtefactsInternal().put(name, createdFolder);
        return createdFolder;
    }

    public AProjectResource addResource(String name, AProjectResource resource) throws ProjectException {
        return addResource(name, resource.getContent());
    }

    public AProjectResource addResource(String name, InputStream content) throws ProjectException {
        AProjectResource createdResource = new AProjectResource(getAPI().addResource(name, content), getProject());
        IOUtils.closeQuietly(content);
        getArtefactsInternal().put(name, createdResource);
        return createdResource;
    }

    public synchronized Collection<AProjectArtefact> getArtefacts() {
        return getArtefactsInternal().values();
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public void update(AProjectArtefact newFolder, CommonUser user, int major, int minor) throws ProjectException {
        super.update(newFolder, user, major, minor);
        if (this.isFolder()) {

            AProjectFolder folder = (AProjectFolder) newFolder;
            // remove absent
            for (AProjectArtefact artefact : getArtefacts()) {
                String name = artefact.getName();

                if (!folder.hasArtefact(name)) {
                    // was deleted
                    artefact.delete();
                } else {
                    AProjectArtefact newArtefact = folder.getArtefact(name);

                    if (newArtefact.isFolder() == artefact.isFolder()) {
                        // update existing
                        artefact.update(newArtefact, user, major, minor);
                    } else {
                        // the same name but other type
                        artefact.delete();
                    }
                }
            }

            // add new
            for (AProjectArtefact artefact : folder.getArtefacts()) {
                String name = artefact.getName();
                if (!hasArtefact(name)) {
                    if (artefact.isFolder()) {
                        addFolder(name).update(artefact, user, major, minor);
                    } else {
                        addResource(name, (AProjectResource) artefact).update(artefact, user, major, minor);
                    }
                }
            }
        }
        save(user, major, minor);
    }
    
    @Override
    public void smartUpdate(AProjectArtefact newFolder, CommonUser user, int major, int minor) throws ProjectException {
        if (newFolder.isModified()) {
            super.smartUpdate(newFolder, user, major, minor);
            if (this.isFolder()) {

                AProjectFolder folder = (AProjectFolder) newFolder;
                // remove absent
                for (AProjectArtefact artefact : getArtefacts()) {
                    String name = artefact.getName();

                    if (!folder.hasArtefact(name)) {
                        // was deleted
                        artefact.delete();
                    } else {
                        AProjectArtefact newArtefact = folder.getArtefact(name);

                        if (newArtefact.isFolder() == artefact.isFolder()) {
                            // update existing
                            artefact.smartUpdate(newArtefact, user, major, minor);
                        } else {
                            // the same name but other type
                            artefact.delete();
                        }
                    }
                }

                // add new
                for (AProjectArtefact artefact : folder.getArtefacts()) {
                    String name = artefact.getName();
                    if (!hasArtefact(name)) {
                        if (artefact.isFolder()) {
                            addFolder(name).update(artefact, user, major, minor);
                        } else {
                            addResource(name, (AProjectResource) artefact).update(artefact, user, major, minor);
                        }
                    }
                }
            }
            save(user, major, minor);
        }
    }

    private Object lock = new Object();

    protected Map<String, AProjectArtefact> getArtefactsInternal() {
        synchronized (lock) {
            if (artefacts == null) {
                artefacts = new HashMap<String, AProjectArtefact>();
                for (ArtefactAPI artefactAPI : getAPI().getArtefacts()) {
                    if (artefactAPI.isFolder()) {
                        artefacts.put(artefactAPI.getName(), new AProjectFolder((FolderAPI) artefactAPI, getProject()));
                    } else {
                        artefacts.put(artefactAPI.getName(), new AProjectResource((ResourceAPI) artefactAPI,
                                getProject()));
                    }
                }
            }
        }
        return artefacts;
    }

    @Override
    public void refresh() {
        super.refresh();
        synchronized (lock) {
            artefacts = null;
        }
    }
}
