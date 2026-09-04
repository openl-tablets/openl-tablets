package org.openl.studio.projects.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.webstudio.web.repository.ProjectDescriptorArtefactResolver;
import org.openl.rules.workspace.uw.UserWorkspace;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectDependencyResolverImpl implements ProjectDependencyResolver {

    private final ProjectDescriptorArtefactResolver projectDescriptorResolver;
    private final ProjectListingContext listingContext;

    @Lookup
    protected UserWorkspace getUserWorkspace() {
        return null;
    }

    @Override
    public List<ProjectDependency> getDependencies(RulesProject project) {
        // Build the dependency-name index once per request; listing a page resolves dependencies for
        // every project and would otherwise rebuild it each time (O(N^2)).
        var projectIndex = listingContext.dependencyIndex(() ->
                getAllProjects().stream().collect(Collectors.groupingBy(this::dependencyLookupName)));

        var dependencies = new ArrayList<ProjectDependency>();
        var declared = declaredDependencies(project);
        var walk = new Walk(project.getRepository().getId(),
                project.getBranch(),
                declared.stream().map(ProjectDependencyDescriptor::getName).collect(Collectors.toSet()),
                projectIndex,
                new HashSet<>(Set.of(dependencyLookupName(project))));
        calcDependencies(project, declared, walk, dependencies);
        return dependencies;
    }

    /**
     * What a walk started at one project resolves against: that project's repository and branch, and the names
     * it declares itself.
     *
     * <p>The branch stays the one the project is opened on for every step inside its own repository, so a
     * dependency reached through another one is looked for where the project itself lives. Another repository
     * keeps its own branch, since nothing pairs the branches of two repositories.
     */
    private record Walk(String repositoryId,
                        @Nullable String branch,
                        Set<String> declared,
                        Map<String, List<RulesProject>> index,
                        Set<String> processed) {
    }

    @Override
    public List<RulesProject> getDependsOnProject(RulesProject project) throws ProjectException {
        var dependencyName = dependencyLookupName(project);
        var usedByIndex = listingContext.usedByIndex(this::buildUsedByIndex);
        var repositoryId = project.getRepository().getId();
        var branch = project.getBranch();
        return usedByIndex.getOrDefault(dependencyName, List.of())
                .stream()
                .filter(dependent -> visibleTo(dependent, repositoryId, branch))
                .toList();
    }

    /**
     * Returns the name a dependency descriptor uses to address the project.
     *
     * <p>A local-only project has no Design repository identity, so its logical {@code rules.xml} name is
     * authoritative. A repository-backed project keeps its stable business name. Its currently selected branch
     * may have an unsaved logical-name change that must not hide the project from another branch.
     */
    private String dependencyLookupName(RulesProject project) {
        return project.isLocalOnly() ? projectDescriptorResolver.getLogicalName(project) : project.getBusinessName();
    }

    private Map<String, List<RulesProject>> buildUsedByIndex() throws ProjectException {
        var index = new HashMap<String, List<RulesProject>>();
        for (RulesProject pr : getAllProjects()) {
            var dependencyNames = projectDescriptorResolver.getDependencies(pr).stream()
                    .map(ProjectDependencyDescriptor::getName)
                    .collect(Collectors.toSet());
            for (String dependencyName : dependencyNames) {
                index.computeIfAbsent(dependencyName, key -> new ArrayList<>()).add(pr);
            }
        }
        return index;
    }

    /**
     * What a project declares in its {@code rules.xml}, empty when the descriptor cannot be read.
     */
    private List<ProjectDependencyDescriptor> declaredDependencies(RulesProject project) {
        try {
            return projectDescriptorResolver.getDependencies(project);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // Skip this dependency
            return List.of();
        }
    }

    /**
     * Collects what the project depends on, walking into each dependency it resolves.
     *
     * <p>A dependency is direct when the project the walk started at declares it, whichever step of the walk
     * reaches it first. Otherwise another dependency brings it in and it is reported as transitive.
     *
     * @param dependenciesDescriptors what this project declares, already read by the caller
     */
    private void calcDependencies(RulesProject project,
                                  List<ProjectDependencyDescriptor> dependenciesDescriptors,
                                  Walk walk,
                                  Collection<ProjectDependency> result) {
        if (dependenciesDescriptors.isEmpty()) {
            return;
        }

        var repoId = project.getRepository().getId();
        var branch = repoId.equals(walk.repositoryId()) ? walk.branch() : project.getBranch();

        for (ProjectDependencyDescriptor dependency : dependenciesDescriptors) {
            var dependencyName = dependency.getName();
            if (walk.processed().add(dependencyName)) {
                // A name the workspace has no project for is kept as it is declared: the screen shows the
                // dependency and says it is missing, instead of hiding what rules.xml asks for.
                var resolved = resolveDependency(dependencyName, repoId, branch, walk.index());
                result.add(new ProjectDependency(dependencyName,
                        resolved.orElse(null),
                        !walk.declared().contains(dependencyName)));
                resolved.ifPresent(dep -> calcDependencies(dep, declaredDependencies(dep), walk, result));
            }
        }
    }

    /**
     * Resolves a dependency name to the project the branch of the depending project actually contains.
     *
     * <p>The own repository is searched in that branch alone. A name the branch does not contain is left
     * unresolved even when another branch has a project of that name.
     *
     * <p>Another repository is searched whatever branch it is on, because nothing keeps the branches of two
     * repositories in step. The own repository is preferred over such a match.
     */
    private Optional<RulesProject> resolveDependency(String dependencyName,
                                                     String repoId,
                                                     @Nullable String projectBranch,
                                                     Map<String, List<RulesProject>> projectIndex) {
        // Use index for O(1) lookup instead of filtering all projects
        List<RulesProject> candidateProjects = projectIndex.get(dependencyName);
        if (candidateProjects == null || candidateProjects.isEmpty()) {
            return Optional.empty();
        }
        var visible = candidateProjects.stream()
                .filter(candidate -> visibleTo(candidate, repoId, projectBranch))
                .toList();
        return visible.stream()
                .filter(candidate -> candidate.getRepository().getId().equals(repoId))
                .findFirst()
                .or(() -> visible.stream().findFirst());
    }

    /**
     * Whether a project belongs to what a project of the given repository and branch may refer to.
     *
     * <p>Inside one repository only the own branch counts: its branches are versions of the same content, so
     * a project another branch keeps is a different version rather than another project.
     *
     * <p>A project of another repository always counts, since the branches of two repositories are not kept
     * in step and neither of them can stand for the other.
     */
    private boolean visibleTo(RulesProject project, String repositoryId, @Nullable String branch) {
        return !project.getRepository().getId().equals(repositoryId) || containedInBranch(project, branch);
    }

    /**
     * Whether the given branch of the project's own repository contains it.
     *
     * <p>The workspace shows a project on one branch at a time, which is not necessarily the branch asked
     * about, so membership is taken from the cross-branch index rather than from the branch on display. A
     * repository without branches reports none, and then every branch contains everything it has.
     */
    private boolean containedInBranch(RulesProject project, @Nullable String branch) {
        if (branch == null || branch.equals(project.getBranch())) {
            return true;
        }
        var repositoryId = project.getRepository().getId();
        var name = project.getDesignProjectName();
        return listingContext.branchHoldsProject(repositoryId, name, branch,
                () -> getUserWorkspace().getDesignTimeRepository().containsProject(repositoryId, name, branch));
    }

    private Collection<RulesProject> getAllProjects() {
        return getUserWorkspace().getProjects(false);
    }
}
