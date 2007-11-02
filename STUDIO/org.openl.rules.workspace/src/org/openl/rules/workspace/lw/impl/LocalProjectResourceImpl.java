package org.openl.rules.workspace.lw.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.lw.LocalProjectResource;
import org.openl.util.Log;

import java.io.*;

public class LocalProjectResourceImpl extends LocalProjectArtefactImpl implements LocalProjectResource {
    // 1 KBytes
    public static final int DOWNLOAD_BUFFER_SIZE = 1024;
    
    private String resourceType;

    private long lastModified;

    public LocalProjectResourceImpl(String name, ArtefactPath path, File location) {
        super(name, path, location);

        resourceType = "unknown";
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

    public LocalProjectArtefact getArtefact(String name) throws ProjectException {
        throw new ProjectException("Cannot find project artefact ''{0}''", name);
    }

    public void refresh() {
        long lm = getLocation().lastModified();
        if (lm != lastModified) {
            setChanged(true);
            lastModified = lm;
        }
    }

    // --- protected

    protected void downloadArtefact(ProjectResource resource) throws ProjectException {
        InputStream is = resource.getContent();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(getLocation());
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];

            while (true) {
                int readed = is.read(buffer);
                if (readed < 0) break;

                fos.write(buffer, 0, readed);
            }
        } catch (IOException e) {
            throw new ProjectException("Cannot transfer resource context", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.error("Cannot close resource output stream", e);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.error("Cannot close resource input stream", e);
                }
            }
        }

        refresh();

        setNew(false);
        setChanged(false);
    }
}
