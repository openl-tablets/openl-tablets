package org.openl.rules.ruleservice.conf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.ruleservice.core.Resource;
import org.openl.rules.ruleservice.core.ResourceLoader;

class ResourceLoaderImpl implements ResourceLoader {

    private AProject project;

    public ResourceLoaderImpl(AProject project) {
        this.project = Objects.requireNonNull(project, "project cannot be null");
    }

    @Override
    public Resource getResource(String name) {
        try {
            AProjectArtefact artefact = project.getArtefact(name);
            if (artefact instanceof AProjectResource) {
                final AProjectResource resource = (AProjectResource) artefact;
                return new Resource() {
                    @Override
                    public InputStream getResourceAsStream() throws IOException {
                        try {
                            return resource.getContent();
                        } catch (ProjectException e) {
                            throw new IOException(e);
                        }
                    }

                    @Override
                    public boolean exists() {
                        return true;
                    }
                };
            }
        } catch (Exception e) {
            return NOT_FOUNT_RESOURCE;
        }
        return NOT_FOUNT_RESOURCE;
    }

    private static final Resource NOT_FOUNT_RESOURCE = new Resource() {
        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public InputStream getResourceAsStream() throws IOException {
            throw new IOException("Resource is not found!");
        }
    };
}
