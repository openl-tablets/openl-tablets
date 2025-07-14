package org.openl.rules.webstudio.web.repository;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;

class RepositoryFilter implements IFilter<AProjectArtefact> {
    private final String repositoryId;

    RepositoryFilter(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass != null && AProject.class.isAssignableFrom(aClass);
    }

    @Override
    public boolean select(AProjectArtefact project) {
        return repositoryId.equals(project.getRepository().getId());
    }
}
