package org.openl.studio.projects.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        // Build the business-name index once per request; listing a page resolves dependencies for
        // every project and would otherwise rebuild it each time (O(N^2)).
        var projectIndex = listingContext.dependencyIndex(() ->
                getAllProjects().stream().collect(Collectors.groupingBy(RulesProject::getBusinessName)));

        var dependencies = new ArrayList<ProjectDependency>();
        calcDependencies(project, new HashSet<>(Set.of(project.getBusinessName())), dependencies, projectIndex);
        return dependencies;
    }

    @Override
    public List<RulesProject> getDependsOnProject(RulesProject project) throws ProjectException {
        // Match on the business name: a rules.xml <dependency> references a project by that name, and it
        // is stable across states, unlike getName() which for a closed project in a mapped repo carries the
        // internal path suffix and would not match.
        var businessName = project.getBusinessName();
        var usedByIndex = listingContext.usedByIndex(this::buildUsedByIndex);
        return usedByIndex.getOrDefault(businessName, List.of());
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

    private void calcDependencies(RulesProject project,
                                  Set<String> processedProjects,
                                  Collection<ProjectDependency> result,
                                  Map<String, List<RulesProject>> projectIndex) {
        List<ProjectDependencyDescriptor> dependenciesDescriptors;
        try {
            dependenciesDescriptors = projectDescriptorResolver.getDependencies(project);
            if (dependenciesDescriptors.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // Skip this dependency
            return;
        }

        var repoId = project.getRepository().getId();
        var projectBranch = project.getBranch();

        for (ProjectDependencyDescriptor dependency : dependenciesDescriptors) {
            var dependencyName = dependency.getName();
            if (processedProjects.add(dependencyName)) {
                // A name the workspace has no project for is kept as it is declared: the screen shows the
                // dependency and says it is missing, instead of hiding what rules.xml asks for.
                var resolved = resolveDependency(dependencyName, repoId, projectBranch, projectIndex);
                result.add(new ProjectDependency(dependencyName, resolved.orElse(null)));
                resolved.ifPresent(dep -> calcDependencies(dep, processedProjects, result, projectIndex));
            }
        }
    }

    /**
     * Resolves a dependency name to the best matching project by priority:
     * <ol>
     *     <li>same repository and same branch (or an unspecified branch);</li>
     *     <li>same repository, any branch (e.g. the dependency lives on the base branch while this
     *     project is on a feature branch) -- preferred over another repository;</li>
     *     <li>a different repository.</li>
     * </ol>
     */
    private static Optional<RulesProject> resolveDependency(String dependencyName,
                                                            String repoId,
                                                            String projectBranch,
                                                            Map<String, List<RulesProject>> projectIndex) {
        // Use index for O(1) lookup instead of filtering all projects
        List<RulesProject> candidateProjects = projectIndex.get(dependencyName);
        if (candidateProjects == null || candidateProjects.isEmpty()) {
            return Optional.empty();
        }
        Predicate<RulesProject> inSameRepo = p -> p.getRepository().getId().equals(repoId);
        return candidateProjects.stream()
                .filter(inSameRepo)
                .filter(p -> projectBranch == null || p.getBranch() == null || p.getBranch().equals(projectBranch))
                .findFirst()
                .or(() -> candidateProjects.stream().filter(inSameRepo).findFirst())
                .or(() -> candidateProjects.stream().filter(inSameRepo.negate()).findFirst());
    }

    private Collection<RulesProject> getAllProjects() {
        return getUserWorkspace().getProjects(false);
    }
}
