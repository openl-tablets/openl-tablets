package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;

class ProjectDependencyResolverImplTest {

    private static RulesProject project(String name, String branch) {
        return project(name, branch, "design");
    }

    private static RulesProject project(String name, String branch, String repositoryId) {
        Repository repository = mock(Repository.class);
        when(repository.getId()).thenReturn(repositoryId);
        RulesProject project = mock(RulesProject.class);
        when(project.getName()).thenReturn(name);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getDesignProjectName()).thenReturn(name);
        when(project.getRepository()).thenReturn(repository);
        when(project.getBranch()).thenReturn(branch);
        return project;
    }

    /**
     * A workspace holding the given projects, whose index reports no project outside the branch each of them
     * is shown on. A test that needs a project to belong to another branch as well says so with
     * {@link #alsoInBranch}.
     */
    private static UserWorkspace workspace(RulesProject... projects) {
        UserWorkspace workspace = mock(UserWorkspace.class);
        DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);
        when(workspace.getProjects(false)).thenReturn(List.of(projects));
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        return workspace;
    }

    /** Makes the index report the project in one more branch than the one the workspace shows it on. */
    private static void alsoInBranch(UserWorkspace workspace, RulesProject project, String branch) {
        when(workspace.getDesignTimeRepository()
                .containsProject(project.getRepository().getId(), project.getDesignProjectName(), branch))
                        .thenReturn(true);
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
    void leavesADependencyOfAnotherBranchOfTheSameRepositoryUnresolved() throws Exception {
        // The branches of one repository are versions of the same content, so the copy on another branch is
        // a different version rather than another project. Opening it would compile against rules this branch
        // never had, so the dependency counts as missing instead.
        RulesProject target = project("Offer API", "master");
        RulesProject source = project("ACC Master Offer", "feature/ddd");
        ProjectDependencyDescriptor descriptor = dependency("Offer API");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(descriptor));
        when(descriptorResolver.getDependencies(target)).thenReturn(List.of());

        UserWorkspace workspace = workspace(source, target);

        var dependencies = resolver(descriptorResolver, workspace).getDependencies(source);

        assertEquals(1, dependencies.size());
        assertEquals("Offer API", dependencies.getFirst().name());
        assertNull(dependencies.getFirst().project());
        assertEquals(List.of(), resolver(descriptorResolver, workspace).getProjectDependencies(source));
    }

    @Test
    void resolvesADependencyTheBranchContainsThoughTheWorkspaceShowsItOnAnother() throws Exception {
        // The workspace shows every project on one branch at a time, and only this one was switched over.
        // The branch does contain the dependency, so it resolves: membership decides, not what is on display.
        RulesProject target = project("Offer API", "master");
        RulesProject source = project("ACC Master Offer", "feature/ddd");
        ProjectDependencyDescriptor descriptor = dependency("Offer API");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(descriptor));
        when(descriptorResolver.getDependencies(target)).thenReturn(List.of());

        UserWorkspace workspace = workspace(source, target);
        alsoInBranch(workspace, target, "feature/ddd");

        assertEquals(List.of(target), resolver(descriptorResolver, workspace).getProjectDependencies(source));
    }

    @Test
    void resolvesDependencyInAnotherRepositoryWhateverBranchItIsOn() throws Exception {
        // Two repositories keep no branches in step, so neither branch name means anything to the other.
        RulesProject target = project("Offer API", "master", "shared");
        RulesProject source = project("ACC Master Offer", "feature/ddd");
        ProjectDependencyDescriptor descriptor = dependency("Offer API");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(descriptor));
        when(descriptorResolver.getDependencies(target)).thenReturn(List.of());

        UserWorkspace workspace = workspace(source, target);

        assertEquals(List.of(target), resolver(descriptorResolver, workspace).getProjectDependencies(source));
    }

    @Test
    void prefersTheOwnRepositoryOverAMatchInAnotherOne() throws Exception {
        RulesProject foreign = project("Offer API", "master", "shared");
        RulesProject own = project("Offer API", "master");
        RulesProject source = project("ACC Master Offer", "master");
        ProjectDependencyDescriptor descriptor = dependency("Offer API");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(descriptor));
        when(descriptorResolver.getDependencies(foreign)).thenReturn(List.of());
        when(descriptorResolver.getDependencies(own)).thenReturn(List.of());

        UserWorkspace workspace = workspace(source, foreign, own);

        assertEquals(List.of(own), resolver(descriptorResolver, workspace).getProjectDependencies(source));
    }

    @Test
    void marksWhatAnotherDependencyDeclaresAsTransitive() throws Exception {
        // A -> B -> C: the panel sits next to rules.xml, which declares B alone, so C must say it is reached
        // through B. Both are still reported: compiling A needs C opened as well.
        RulesProject c = project("C", "master");
        RulesProject b = project("B", "master");
        RulesProject a = project("A", "master");
        ProjectDependencyDescriptor declaresB = dependency("B");
        ProjectDependencyDescriptor declaresC = dependency("C");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(a)).thenReturn(List.of(declaresB));
        when(descriptorResolver.getDependencies(b)).thenReturn(List.of(declaresC));
        when(descriptorResolver.getDependencies(c)).thenReturn(List.of());

        UserWorkspace workspace = workspace(a, b, c);

        var dependencies = resolver(descriptorResolver, workspace).getDependencies(a);

        assertEquals(List.of("B", "C"), dependencies.stream().map(ProjectDependency::name).toList());
        assertFalse(dependencies.getFirst().transitive());
        assertTrue(dependencies.getLast().transitive());
    }

    @Test
    void callsADependencyDirectWhicheverStepOfTheWalkReachesItFirst() throws Exception {
        // A declares both B and C, and B declares C as well. Which of the two the walk meets first is the
        // order rules.xml happens to list them in, and it must not decide what the panel says about C.
        RulesProject c = project("C", "master");
        RulesProject b = project("B", "master");
        RulesProject a = project("A", "master");
        ProjectDependencyDescriptor declaresB = dependency("B");
        ProjectDependencyDescriptor declaresC = dependency("C");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(a)).thenReturn(List.of(declaresB, declaresC));
        when(descriptorResolver.getDependencies(b)).thenReturn(List.of(declaresC));
        when(descriptorResolver.getDependencies(c)).thenReturn(List.of());

        UserWorkspace workspace = workspace(a, b, c);

        var dependencies = resolver(descriptorResolver, workspace).getDependencies(a);

        assertEquals(List.of("B", "C"), dependencies.stream().map(ProjectDependency::name).toList());
        assertFalse(dependencies.getFirst().transitive());
        assertFalse(dependencies.getLast().transitive(), "A declares C itself, so it is not reached through B.");
    }

    @Test
    void keepsResolvingInTheBranchOfTheProjectTheWalkStartedAt() throws Exception {
        // B is contained in the opened branch though the workspace shows it on another one. What B declares
        // must still be looked for in the opened branch, or the project compiles against a branch it never had.
        RulesProject grandchild = project("Rates", "master");
        RulesProject child = project("Offer API", "master");
        RulesProject source = project("ACC Master Offer", "feature/ddd");
        ProjectDependencyDescriptor declaresChild = dependency("Offer API");
        ProjectDependencyDescriptor declaresGrandchild = dependency("Rates");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(declaresChild));
        when(descriptorResolver.getDependencies(child)).thenReturn(List.of(declaresGrandchild));
        when(descriptorResolver.getDependencies(grandchild)).thenReturn(List.of());

        UserWorkspace workspace = workspace(source, child, grandchild);
        alsoInBranch(workspace, child, "feature/ddd");

        var dependencies = resolver(descriptorResolver, workspace).getDependencies(source);

        assertEquals(List.of(child), dependencies.stream().map(ProjectDependency::project)
                .filter(java.util.Objects::nonNull).toList());
        assertEquals("Rates", dependencies.getLast().name());
        assertNull(dependencies.getLast().project(), "The opened branch has no Rates, so it is missing.");
    }

    @Test
    void usedByKeepsTheBranchOfTheProjectAndAnyOtherRepository() throws Exception {
        // The dependents of a project are the ones its own branch has, plus those of other repositories,
        // whose branches nothing keeps in step with this one.
        RulesProject target = project("Offer API", "master");
        RulesProject sameBranch = project("ACC Master Offer", "master");
        RulesProject otherBranch = project("Historical Offer", "release-24.1");
        RulesProject otherRepository = project("Shared Offer", "trunk", "shared");
        ProjectDependencyDescriptor declared = dependency("Offer API");

        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(target)).thenReturn(List.of());
        for (RulesProject dependent : List.of(sameBranch, otherBranch, otherRepository)) {
            when(descriptorResolver.getDependencies(dependent)).thenReturn(List.of(declared));
        }

        UserWorkspace workspace = workspace(target, sameBranch, otherBranch, otherRepository);

        assertEquals(List.of(sameBranch, otherRepository),
                resolver(descriptorResolver, workspace).getDependsOnProject(target));
    }

    @Test
    void keepsADependencyTheWorkspaceHasNoProjectFor() throws Exception {
        // rules.xml may name a project nobody has: it is reported as declared, with no project of its own.
        RulesProject source = project("ACC Master Offer", "master");
        ProjectDependencyDescriptor declared = dependency("Ghost");
        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(declared));

        UserWorkspace workspace = workspace(source);

        var dependencies = resolver(descriptorResolver, workspace).getDependencies(source);

        assertEquals(1, dependencies.size());
        assertEquals("Ghost", dependencies.getFirst().name());
        assertNull(dependencies.getFirst().project());
        // The projects a caller can work with are still only the ones that resolved.
        assertEquals(List.of(), resolver(descriptorResolver, workspace).getProjectDependencies(source));
    }

    @Test
    void usedByMatchesAClosedProjectByBusinessName() throws Exception {
        // A closed project in a mapped repo has getName() carrying the internal path suffix, while the
        // dependency descriptor and business name stay clean. usedBy must still resolve regardless of state.
        RulesProject target = project("Offer API", "master");
        when(target.getName()).thenReturn("Offer API:ac415ce1");

        RulesProject source = project("ACC Master Offer", "master");

        ProjectDependencyDescriptor descriptor = dependency("Offer API");
        ProjectDescriptorArtefactResolver descriptorResolver = mock(ProjectDescriptorArtefactResolver.class);
        when(descriptorResolver.getDependencies(source)).thenReturn(List.of(descriptor));
        when(descriptorResolver.getDependencies(target)).thenReturn(List.of());

        UserWorkspace workspace = workspace(source, target);

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

        UserWorkspace workspace = workspace(source, targetA, targetB);

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

        UserWorkspace workspace = workspace(source, target);

        assertEquals(List.of(target), resolver(descriptorResolver, workspace).getProjectDependencies(source));
    }
}
