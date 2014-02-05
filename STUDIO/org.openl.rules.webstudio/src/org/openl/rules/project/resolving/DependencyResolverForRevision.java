package org.openl.rules.project.resolving;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolves specified OpenL project revision's dependencies.
 */
public class DependencyResolverForRevision {
    private final RulesProjectResolver rulesProjectResolver;
    private final TemporaryRevisionsStorage temporaryRevisionsStorage;

    /**
     * Project descriptors cache.
     * Replace with ehcache if GC occurs too often.
     */
    private final Map<String, ProjectDescriptor> cache = new WeakHashMap<String, ProjectDescriptor>();

    public DependencyResolverForRevision(RulesProjectResolver rulesProjectResolver, TemporaryRevisionsStorage temporaryRevisionsStorage) {
        this.rulesProjectResolver = rulesProjectResolver;
        this.temporaryRevisionsStorage = temporaryRevisionsStorage;
    }

    public ProjectDescriptor getProjectDescriptor(AProject project) throws ProjectException, ProjectResolvingException {
        File projectFolder = temporaryRevisionsStorage.getRevision(project.getAPI());

        ProjectDescriptor descriptor = cache.get(projectFolder.getAbsolutePath());
        if (descriptor != null) {
            return descriptor;
        }

        ResolvingStrategy resolvingStrategy = rulesProjectResolver.isRulesProject(projectFolder);
        descriptor = resolvingStrategy != null ? resolvingStrategy.resolveProject(projectFolder) : null;

        if (descriptor != null) {
            cache.put(projectFolder.getAbsolutePath(), descriptor);
        }

        return descriptor;
    }

    public List<ProjectDependencyDescriptor> getDependencies(AProject project) throws ProjectException, ProjectResolvingException {
        ProjectDescriptor pd = getProjectDescriptor(project);
        return pd != null ? pd.getDependencies() : null;
    }
}
