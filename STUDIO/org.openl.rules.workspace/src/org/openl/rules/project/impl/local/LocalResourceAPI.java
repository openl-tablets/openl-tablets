package org.openl.rules.project.impl.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.ResourceAPI;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.impl.StateHolder;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalResourceAPI extends LocalArtefactAPI implements ResourceAPI {
    private final Logger log = LoggerFactory.getLogger(LocalResourceAPI.class);

    private String resourceType;

    public LocalResourceAPI(File source, ArtefactPath path, LocalWorkspace workspace) {
        super(source, path, workspace);
        resourceType = "unknown";
    }

    @Override
    public StateHolder getStateHolder() {
        ResourceStateHolder state = new ResourceStateHolder();
        state.resourceType = this.resourceType;
        state.parent = super.getStateHolder();
        return state;
    }

    @Override
    public void applyStateHolder(StateHolder stateHolder) {
        if (stateHolder instanceof ResourceStateHolder) {
            ResourceStateHolder state = (ResourceStateHolder) stateHolder;
            super.applyStateHolder(state.parent);
            this.resourceType = state.resourceType;
        } else {
            // TODO consider exception throwing
            log.error("Incorrect type of stateHolder. Was: '{}', but should be 'ArtefactStateHolder", stateHolder.getClass().getName());
        }
    }

    public String getResourceType() {
        return resourceType;
    }

    public InputStream getContent() throws ProjectException {
        try {
            return new FileInputStream(source);
        } catch (FileNotFoundException e) {
            throw new ProjectException("Failed to get content.", e);
        }
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(source);
            IOUtils.copy(inputStream, fos);
            notifyModified();
        } catch (IOException e) {
            throw new ProjectException("Failed to set content.", e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private static class ResourceStateHolder implements StateHolder {
        private static final long serialVersionUID = -7598752238896061537L;

        private String resourceType;
        private StateHolder parent;
    }
}
