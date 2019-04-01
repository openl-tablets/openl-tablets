package org.openl.rules.project.impl.local;

import java.io.*;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.ResourceAPI;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.impl.StateHolder;
import org.openl.util.IOUtils;

public class LocalResourceAPI extends LocalArtefactAPI implements ResourceAPI {

    public LocalResourceAPI(File source, ArtefactPath path, LocalWorkspace workspace) {
        super(source, path, workspace);
    }

    @Override
    public void applyStateHolder(StateHolder stateHolder) {
        if (stateHolder instanceof ResourceStateHolder) {
            ResourceStateHolder state = (ResourceStateHolder) stateHolder;
            super.applyStateHolder(state.parent);
        } else {
            super.applyStateHolder(stateHolder);
        }
    }

    @Override
    public InputStream getContent() throws ProjectException {
        try {
            return new FileInputStream(source);
        } catch (FileNotFoundException e) {
            throw new ProjectException("Failed to get content.", e);
        }
    }

    @Override
    public long getSize() {
        return source.length();
    }

    @Override
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

    /**
     * @deprecated Is kept for backward compatibility to load old repository
     */
    @Deprecated
    private static class ResourceStateHolder implements StateHolder {
        private static final long serialVersionUID = -7598752238896061537L;

        private String resourceType;
        private StateHolder parent;
    }
}
