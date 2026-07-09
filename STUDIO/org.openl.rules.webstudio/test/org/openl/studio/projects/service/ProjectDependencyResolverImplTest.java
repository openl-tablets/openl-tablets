package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.repository.ProjectDescriptorArtefactResolver;
import org.openl.rules.workspace.uw.UserWorkspace;

class ProjectDependencyResolverImplTest {

    private static RulesProject project(String name, String branch) {
        Repository repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        RulesProject project = mock(RulesProject.class);
        when(project.getName()).thenReturn(name);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getRepository()).thenReturn(repository);
        when(project.getBranch()).thenReturn(branch);
        return project;
    }

    private static ProjectDependencyResolverImpl resolver(ProjectDescriptorArtefactResolver descriptorResolver,
                                                          UserWorkspace workspace) {
        return resolver(descriptorResolver, workspace, new ProjectListingContext());
    }

    private static ProjectDependencyResolverImpl resolver(ProjectDescriptorArtefactResolver descriptorResolver,
                                                          UserWorkspace workspace,
                                                          ProjectListingContext listingContext) {
        return new ProjectDependencyResolverImpl(descriptorResolver, listingContext) {
            @Override
            protected UserWorkspace getUserWorkspace() {
                return workspace;
            }
        };
    }

    private static ProjectDependencyDescriptor dependency(String name) {
        ProjectDependencyDescriptor descriptor = mock(ProjectDependencyDescriptor.class);
        when(descriptor.getName()).thenReturn(name);
        return descriptor;
    }

    @Test
    void resolvesDependencyOnAnotherBranchInTheSameRepository() throws Exception {
        // The dependency lives on the base branch while the project sits on a feature branch. It must
        // still resolve: a same-repository match on a different branch is preferred over none at all.
        RulesProject target = project("Offer API", "master");
        RulesProject source = project("ACC Master Offer", "feature/ddd");
        ProjectDependencyDescriptor descriptor = dependency("Offer API");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(descriptor));
        when(descriptorResolver.getDependencies(target)).thenReturn(List.of());

        UserWorkspace workspace = mock(UserWorkspace.class);
        when(workspace.getProjects()).thenReturn(List.of(source, target));

        assertEquals(List.of(target), resolver(descriptorResolver, workspace).getProjectDependencies(source));
    }

    @Test
    void usedByMatchesAClosedProjectByBusinessName() throws Exception {
        // A closed project in a mapped repo has getName() carrying the internal path suffix, while the
        // dependency descriptor and business name stay clean. usedBy must still resolve regardless of state.
        RulesProject target = mock(RulesProject.class);
        when(target.getName()).thenReturn("Offer API:ac415ce1");
        when(target.getBusinessName()).thenReturn("Offer API");

        RulesProject source = mock(RulesProject.class);
        when(source.getBusinessName()).thenReturn("ACC Master Offer");

        ProjectDependencyDescriptor descriptor = dependency("Offer API");
        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(descriptor));
        when(descriptorResolver.getDependencies(target)).thenReturn(List.of());

        UserWorkspace workspace = mock(UserWorkspace.class);
        when(workspace.getProjects()).thenReturn(List.of(source, target));

        assertEquals(List.of(source), resolver(descriptorResolver, workspace).getDependsOnProject(target));
    }

    @Test
    void usedByReadsProjectDescriptorsOncePerListingContext() throws Exception {
        RulesProject targetA = project("Offer API", "master");
        RulesProject targetB = project("Pricing API", "master");
        RulesProject source = project("ACC Master Offer", "master");
        ProjectDependencyDescriptor descriptorA = dependency("Offer API");
        ProjectDependencyDescriptor descriptorB = dependency("Pricing API");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(descriptorA, descriptorB));
        when(descriptorResolver.getDependencies(targetA)).thenReturn(List.of());
        when(descriptorResolver.getDependencies(targetB)).thenReturn(List.of());

        UserWorkspace workspace = mock(UserWorkspace.class);
        when(workspace.getProjects()).thenReturn(List.of(source, targetA, targetB));

        var resolver = resolver(descriptorResolver, workspace, new ProjectListingContext());

        assertEquals(List.of(source), resolver.getDependsOnProject(targetA));
        assertEquals(List.of(source), resolver.getDependsOnProject(targetB));
        verify(descriptorResolver, times(1)).getDependencies(source);
    }

    @Test
    void resolvesDependencyOnTheSameBranch() throws Exception {
        RulesProject target = project("PnC Premium Redistribution", "master");
        RulesProject source = project("Personal Motor Rating", "master");
        ProjectDependencyDescriptor descriptor = dependency("PnC Premium Redistribution");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(descriptor));
        when(descriptorResolver.getDependencies(target)).thenReturn(List.of());

        UserWorkspace workspace = mock(UserWorkspace.class);
        when(workspace.getProjects()).thenReturn(List.of(source, target));

        assertEquals(List.of(target), resolver(descriptorResolver, workspace).getProjectDependencies(source));
    }
}
