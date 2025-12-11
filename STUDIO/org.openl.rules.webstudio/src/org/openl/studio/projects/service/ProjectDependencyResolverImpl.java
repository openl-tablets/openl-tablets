package org.openl.studio.projects.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.webstudio.web.repository.ProjectDescriptorArtefactResolver;
import org.openl.rules.workspace.uw.UserWorkspace;

@Service
public class ProjectDependencyResolverImpl implements ProjectDependencyResolver {

    private final Logger log = LoggerFactory.getLogger(ProjectDependencyResolverImpl.class);

    private final ProjectDescriptorArtefactResolver projectDescriptorResolver;

    @Autowired
    public ProjectDependencyResolverImpl(ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        this.projectDescriptorResolver = projectDescriptorResolver;
    }

    @Lookup
    protected UserWorkspace getUserWorkspace() {
        return null;
    }

    @Override
    public List<RulesProject> getProjectDependencies(RulesProject project) {
        // Fetch all projects once and build an index for O(1) lookup by business name
        Collection<RulesProject> allProjects = getAllProjects();
        Map<String, List<RulesProject>> projectIndex = allProjects.stream()
                .collect(Collectors.groupingBy(RulesProject::getBusinessName));

        List<RulesProject> dependencies = new ArrayList<>();
        calcDependencies(project, new HashSet<>(Set.of(project.getBusinessName())), dependencies, projectIndex);
        return dependencies;
    }

    @Override
    public List<RulesProject> getDependsOnProject(RulesProject project) throws ProjectException, JAXBException {
        List<RulesProject> result = new ArrayList<>();
        for (RulesProject pr : getAllProjects()) {
            List<ProjectDependencyDescriptor> dependencies = projectDescriptorResolver.getDependencies(pr);
            if (dependencies.stream().anyMatch(p -> p.getName().equals(project.getName()))) {
                result.add(pr);
            }
        }
        return result;
    }

    private void calcDependencies(RulesProject project,
                                  Set<String> processedProjects,
                                  Collection<RulesProject> result,
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

        String repoId = project.getRepository().getId();
        String projectBranch = project.getBranch();

        for (ProjectDependencyDescriptor dependency : dependenciesDescriptors) {
            String dependencyName = dependency.getName();
            if (!processedProjects.add(dependencyName)) {
                continue;
            }

            // Use index for O(1) lookup instead of filtering all projects
            List<RulesProject> candidateProjects = projectIndex.get(dependencyName);
            if (candidateProjects == null || candidateProjects.isEmpty()) {
                continue;
            }

            // Find the best matching project:
            // 1. Priority: same repository and same branch (or null branch)
            // 2. Fallback: same repository, different branch (but don't search other repos if found)
            // 3. Fallback: different repository

            // Try to find in the same repository with matching branch
            Optional<RulesProject> dependentProject = candidateProjects.stream()
                    .filter(p -> p.getRepository().getId().equals(repoId))
                    .filter(p -> projectBranch == null || p.getBranch() == null || p.getBranch().equals(projectBranch))
                    .findFirst();

            // If no match in same repo with same branch, check if any project exists in same repo
            boolean foundInSameRepo = false;
            if (dependentProject.isEmpty()) {
                foundInSameRepo = candidateProjects.stream()
                        .anyMatch(p -> p.getRepository().getId().equals(repoId));
            }

            // Only search in other repositories if not found in the same repository
            if (dependentProject.isEmpty() && !foundInSameRepo) {
                dependentProject = candidateProjects.stream()
                        .filter(p -> !p.getRepository().getId().equals(repoId))
                        .findFirst();
            }

            dependentProject.ifPresent(dep -> {
                result.add(dep);
                calcDependencies(dep, processedProjects, result, projectIndex);
            });
        }
    }

    private Collection<RulesProject> getAllProjects() {
        return getUserWorkspace().getProjects();
    }
}
