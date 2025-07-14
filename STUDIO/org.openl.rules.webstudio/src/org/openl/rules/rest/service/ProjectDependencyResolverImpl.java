package org.openl.rules.rest.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;

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
        List<RulesProject> dependencies = new ArrayList<>();
        calcDependencies(project, new HashSet<>(Set.of(project.getBusinessName())), dependencies);
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

        Set<String> dependencyNames = dependenciesDescriptors.stream()
                .map(ProjectDependencyDescriptor::getName)
                .collect(Collectors.toSet());
        String repoId = project.getRepository().getId();

        // Separate projects based on the match of the repository with the repository of the project for which the
        // dependencies are searched, since such projects have priority when the name matches.
        Map<Boolean, List<RulesProject>> projects = getAllProjects().stream()
                .filter(p -> dependencyNames.contains(p.getBusinessName()))
                .collect(Collectors.partitioningBy(p -> p.getRepository().getId().equals(repoId)));

        for (String dependencyName : dependencyNames) {
            if (!processedProjects.add(dependencyName)) {
                continue;
            }
            // Since there can be projects with the same name even in the same repository, if the source repository has
            // a project with the search name in the same branch as the project for which the dependency is searched,
            // then it is returned. But if the dependent project is in another branch, then the search in other
            // repositories
            // is not performed.
            Optional<RulesProject> dependentProject = Optional.empty();
            if (!projects.get(Boolean.TRUE).isEmpty()) {
                dependentProject = projects.get(Boolean.TRUE)
                        .stream()
                        .filter(
                                // businessName same and branch is the same, if branch is not null - typically when repository does not support branches
                                p -> p.getBusinessName().equals(dependencyName) && (null == p.getBranch() || p.getBranch().equals(project.getBranch())))
                        .findFirst();
            }
            if (dependentProject.isEmpty() && !projects.get(Boolean.FALSE).isEmpty()) {
                dependentProject = projects.get(Boolean.FALSE)
                        .stream()
                        .filter(p -> p.getBusinessName().equals(dependencyName))
                        .findFirst();
            }

            dependentProject.ifPresent(dep -> {
                result.add(dep);
                calcDependencies(dep, processedProjects, result);
            });
        }
    }

    private Collection<RulesProject> getAllProjects() {
        return getUserWorkspace().getProjects();
    }
}
