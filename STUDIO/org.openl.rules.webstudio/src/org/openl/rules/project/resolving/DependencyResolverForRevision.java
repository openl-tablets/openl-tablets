package org.openl.rules.project.resolving;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;

import java.io.File;
import java.util.List;

/**
 * Resolves specified OpenL project revision's dependencies.
 */
public class DependencyResolverForRevision {
    private final RulesProjectResolver rulesProjectResolver;
    private final TemporaryRevisionsStorage temporaryRevisionsStorage;

    public DependencyResolverForRevision(RulesProjectResolver rulesProjectResolver, TemporaryRevisionsStorage temporaryRevisionsStorage) {
        this.rulesProjectResolver = rulesProjectResolver;
        this.temporaryRevisionsStorage = temporaryRevisionsStorage;
    }

    public ProjectDescriptor getProjectDescriptor(AProject project) throws ProjectException, ProjectResolvingException {
        File projectFolder = temporaryRevisionsStorage.getRevision(project.getAPI());
        ResolvingStrategy resolvingStrategy = rulesProjectResolver.isRulesProject(projectFolder);
        return resolvingStrategy != null ? resolvingStrategy.resolveProject(projectFolder) : null;

    }

    public List<ProjectDependencyDescriptor> getDependencies(AProject project) throws ProjectException, ProjectResolvingException {
        ProjectDescriptor pd = getProjectDescriptor(project);
        return pd != null ? pd.getDependencies() : null;
    }
}
