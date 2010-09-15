package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.lw.LocalProjectResource;
import org.openl.rules.workspace.props.PropertyException;

public class LocalProjectResourceImpl extends LocalProjectArtefactImpl implements LocalProjectResource {
    private static class ResourceStateHolder implements StateHolder {
        private static final long serialVersionUID = -8202714450047051624L;

        StateHolder parent;

        String resourceType;
        long lastModified;
    }
    private static final Log log = LogFactory.getLog(LocalProjectResourceImpl.class);

    // 8 KBytes
    public static final int DOWNLOAD_BUFFER_SIZE = 8 * 1024;

    private String resourceType;

    private long lastModified;

    public LocalProjectResourceImpl(String name, ArtefactPath path, File location) {
        super(name, path, location);

        resourceType = "unknown";
    }

    protected void downloadArtefact(ProjectResource resource) throws ProjectException {
        super.downloadArtefact(resource);

        InputStream is = resource.getContent();

        setLocalContent(is);

        refresh();
    }

    public LocalProjectArtefact getArtefact(String name) throws ProjectException {
        throw new ProjectException("Cannot find project artefact ''{0}''", null, name);
    }

    public InputStream getContent() throws ProjectException {
        try {
            return new FileInputStream(getLocation());
        } catch (FileNotFoundException e) {
            throw new ProjectException("Failed to get content", e);
        }
    }

    public String getResourceType() {
        return resourceType;
    }

    @Override
    public StateHolder getState() {
        ResourceStateHolder state = new ResourceStateHolder();

        state.parent = super.getState();

        state.resourceType = resourceType;
        state.lastModified = lastModified;

        return state;
    }

    public boolean hasArtefact(String name) {
        return false;
    }

    // --- protected

    public boolean isFolder() {
        return false;
    }

    public void refresh() {
        long lm = getLocation().lastModified();
        if (lm != lastModified) {
            setChanged(true);
            lastModified = lm;
        }
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        setLocalContent(inputStream);

        setChanged(true);
    }

    protected void setLocalContent(InputStream is) throws ProjectException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(getLocation());
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];

            while (true) {
                int readed = is.read(buffer);
                if (readed < 0) {
                    break;
                }

                fos.write(buffer, 0, readed);
            }
        } catch (IOException e) {
            throw new ProjectException("Cannot transfer resource context!", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.error("Cannot close resource output stream!", e);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("Cannot close resource input stream!", e);
                }
            }
        }
    }

    @Override
    public void setState(StateHolder aState) throws PropertyException {
        ResourceStateHolder state = (ResourceStateHolder) aState;
        super.setState(state.parent);

        resourceType = state.resourceType;
        lastModified = state.lastModified;
    }
}
