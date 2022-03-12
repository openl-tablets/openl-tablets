package org.openl.rules.rest.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectDependencyResolverImpl implements ProjectDependencyResolver {

    private final Logger log = LoggerFactory.getLogger(ProjectDependencyResolverImpl.class);

    private final ProjectDescriptorArtefactResolver projectDescriptorResolver;

    @Autowired
    public ProjectDependencyResolverImpl(ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        this.projectDescriptorResolver = projectDescriptorResolver;
    }

    @Override
    public List<RulesProject> getProjectDependencies(RulesProject project) {
        List<RulesProject> dependencies = new ArrayList<>();
        calcDependencies(project, new ArrayList<>(List.of(project.getBusinessName())), dependencies);
        return dependencies;
    }

    @Override
    public List<RulesProject> getDependsOnProject(RulesProject project) throws ProjectException {
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
            Collection<String> processedProjects,
            Collection<RulesProject> result) {
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

        List<String> dependencyNames = dependenciesDescriptors.stream()
            .map(ProjectDependencyDescriptor::getName)
            .collect(Collectors.toList());
        String repoId = project.getRepository().getId();

        // Separate projects based on the match of the repository with the repository of the project for which the
        // dependencies are searched, since such projects have priority when the name matches.
        Map<Boolean, List<RulesProject>> projects = getAllProjects().stream()
            .filter(p -> dependencyNames.contains(p.getBusinessName()))
            .collect(Collectors.partitioningBy(p -> p.getRepository().getId().equals(repoId)));

        for (String dependencyName : dependencyNames) {
            if (processedProjects.contains(dependencyName)) {
                continue;
            } else {
                processedProjects.add(dependencyName);
            }
            // Since there can be projects with the same name even in the same repository, if the source repository has
            // a project with the search name in the same branch as the project for which the dependency is searched,
            // then it is returned. But if the dependent project is in another branch, then the search in other
            // repositories
            // is not performed.
            Optional<RulesProject> dependentProject;
            if (!projects.get(true).isEmpty()) {
                dependentProject = projects.get(true)
                    .stream()
                    .filter(
                        p -> p.getBusinessName().equals(dependencyName) && p.getBranch().equals(project.getBranch()))
                    .findFirst();
            } else {
                dependentProject = projects.get(false)
                    .stream()
                    .filter(p -> p.getBusinessName().equals(dependencyName))
                    .findFirst();
            }

            if (dependentProject.isPresent()) {
                result.add(dependentProject.get());
                calcDependencies(dependentProject.get(), processedProjects, result);
            }

        }
    }

    private Collection<RulesProject> getAllProjects() {
        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        return userWorkspace.getProjects();
    }
}
