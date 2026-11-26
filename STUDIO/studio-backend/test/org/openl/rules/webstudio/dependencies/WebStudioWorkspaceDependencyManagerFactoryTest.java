package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;

class WebStudioWorkspaceDependencyManagerFactoryTest {

    private final WebStudio studio = mock(WebStudio.class);
    private final MavenDependencyResolver resolver = mock(MavenDependencyResolver.class);
    private final List<ProjectDescriptor> sessionProjects = new ArrayList<>();
    private final WebStudioWorkspaceDependencyManagerFactory factory = new WebStudioWorkspaceDependencyManagerFactory(
            studio);

    @BeforeEach
    void setUp() {
        when(studio.getAllProjects()).thenReturn(sessionProjects);
        when(studio.getMavenDependencyResolver()).thenReturn(resolver);
        when(studio.getExternalProperties()).thenReturn(Map.of());
        when(resolver.projectOf(any())).thenReturn(MavenDependencyResolver.DownloadedProject.NONE);
        when(resolver.librariesOf(any(), any())).thenReturn(MavenDependencyResolver.Libraries.NONE);
        when(resolver.projectsOfPom(any())).thenReturn(MavenDependencyResolver.PomProjects.NONE);
    }

    @Test
    void aProjectOfTheSessionWinsOverTheMavenRepository() {
        var domain = project("Domain");
        sessionProjects.add(domain);
        var rules = project("Rules", dependency("Domain", "com.example:domain:1.0.0"));

        assertEquals(List.of(rules, domain), List.copyOf(factory.resolveWorkspace(rules)));
        verify(resolver, never()).projectOf(any());
    }

    /**
     * A workspace the session has whole asks the Maven repository nothing: neither the walk nor the messages read the
     * {@code pom.xml} of a project whose dependencies are all opened.
     */
    @Test
    void aWorkspaceTheSessionHasWholeAsksTheMavenRepositoryNothing() {
        var common = project("Common");
        var domain = project("Domain", dependency("Common", null));
        sessionProjects.addAll(List.of(common, domain));
        var rules = project("Rules", dependency("Domain", "com.example:domain:1.0.0"));

        assertEquals(List.of(rules, domain, common), List.copyOf(factory.resolveWorkspace(rules)));
        var manager = factory.buildDependencyManager(rules);
        try {
            assertEquals(List.of(), manager.getDependencyMessages(rules));
            assertEquals(List.of(), manager.getDependencyMessages(domain));
        } finally {
            manager.shutdown();
        }
        verify(resolver, never()).projectsOfPom(any());
        verify(resolver, never()).projectOf(any());
    }

    /**
     * A project the session lacks is found by the {@code pom.xml} files the walk went through, read once it is
     * missing: the one read first, of the project walked first, wins.
     */
    @Test
    void aMissingProjectIsTheOneThePomOfTheFirstProjectWalkedNames() {
        var domain = project("Domain", dependency("Common", null));
        sessionProjects.add(domain);
        var rules = project("Rules", dependency("Domain", null));
        var rulesPom = ProjectPom.of("<project><artifactId>rules</artifactId></project>");
        var domainPom = ProjectPom.of("<project><artifactId>domain</artifactId></project>");
        when(studio.pomOf(rules)).thenReturn(rulesPom);
        when(studio.pomOf(domain)).thenReturn(domainPom);
        when(resolver.projectsOfPom(rulesPom)).thenReturn(new MavenDependencyResolver.PomProjects(
                Map.of("Common", "com.example:common:zip:1.0.0"), List.of()));
        when(resolver.projectsOfPom(domainPom)).thenReturn(new MavenDependencyResolver.PomProjects(
                Map.of("Common", "com.example:common:zip:2.0.0"), List.of()));
        var common = project("Common");
        when(resolver.projectOf(argThat(named("Common", "com.example:common:zip:1.0.0"))))
                .thenReturn(new MavenDependencyResolver.DownloadedProject(common, null));

        assertEquals(List.of(rules, domain, common), List.copyOf(factory.resolveWorkspace(rules)));
    }

    @Test
    void aProjectTheSessionLacksIsDownloadedWithWhatItDependsOn() {
        var common = project("Common");
        sessionProjects.add(common);
        var downloaded = project("Domain", dependency("Common", null));
        var domainDependency = dependency("Domain", "com.example:domain:1.0.0");
        when(resolver.projectOf(domainDependency)).thenReturn(
                new MavenDependencyResolver.DownloadedProject(downloaded, null));
        var rules = project("Rules", domainDependency, dependency(null, "com.example:lib:jar:1.0.0"));

        assertEquals(List.of(rules, downloaded, common), List.copyOf(factory.resolveWorkspace(rules)));
    }

    @Test
    void aProjectNamedByNameIsTheOneOfThatNameThePomDependsOn() {
        var rules = project("Rules", dependency("Domain", null));
        var downloaded = project("Domain", dependency("Common", null));
        var common = project("Common");
        var pom = ProjectPom.of("<project/>");
        when(studio.pomOf(rules)).thenReturn(pom);
        when(resolver.projectsOfPom(pom)).thenReturn(new MavenDependencyResolver.PomProjects(
                Map.of("Domain", "com.example:domain:zip:1.0.0", "Common", "com.example:common:zip:1.0.0"),
                List.of()));
        when(resolver.projectOf(argThat(named("Domain", "com.example:domain:zip:1.0.0"))))
                .thenReturn(new MavenDependencyResolver.DownloadedProject(downloaded, null));
        when(resolver.projectOf(argThat(named("Common", "com.example:common:zip:1.0.0"))))
                .thenReturn(new MavenDependencyResolver.DownloadedProject(common, null));

        assertEquals(List.of(rules, downloaded, common), List.copyOf(factory.resolveWorkspace(rules)),
                "a downloaded project finds what the pom.xml of the project depending on it named");
    }

    @Test
    void aProjectNeitherTheSessionNorTheRepositoryHasIsLeftOut() {
        var rules = project("Rules", dependency("Domain", "com.example:domain:1.0.0"), dependency("Other", null));

        assertEquals(List.of(rules), List.copyOf(factory.resolveWorkspace(rules)));
    }

    @Test
    void withoutAResolverOnlyTheProjectsOfTheSessionAreFound() {
        when(studio.getMavenDependencyResolver()).thenReturn(null);
        var rules = project("Rules", dependency("Domain", "com.example:domain:1.0.0"));

        assertEquals(List.of(rules), List.copyOf(factory.resolveWorkspace(rules)));
        var manager = factory.buildDependencyManager(rules);
        try {
            assertEquals(List.of(), manager.getLibraries(rules));
            assertEquals(List.of(), manager.getProvidedLibraries(rules));
            assertEquals(List.of(), manager.getDependencyMessages(rules));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void theManagerReadsTheLibrariesAndReportsWhatWasNotDownloaded() throws Exception {
        var library = Path.of("repository", "lib-1.0.0.jar").toAbsolutePath();
        var provided = Path.of("repository", "embedded-1.0.0.jar").toAbsolutePath();
        var libraryProblem = new OpenLMessage("Cannot download a Maven dependency: lib", Severity.ERROR);
        var projectProblem = new OpenLMessage("Cannot download a Maven dependency: domain", Severity.ERROR);
        var common = project("Common");
        sessionProjects.add(common);
        var domainDependency = dependency("Domain", "com.example:domain:1.0.0");
        var commonDependency = dependency("Common", "com.example:common:1.0.0");
        var rules = project("Rules",
                domainDependency,
                commonDependency,
                dependency("Plain", null),
                dependency(null, "com.example:lib:jar:1.0.0"));
        when(resolver.librariesOf(eq(rules), any())).thenReturn(
                new MavenDependencyResolver.Libraries(List.of(library), List.of(provided), List.of(libraryProblem)));
        when(resolver.projectOf(domainDependency)).thenReturn(
                new MavenDependencyResolver.DownloadedProject(null, projectProblem));

        var manager = factory.buildDependencyManager(rules);
        try {
            assertEquals(List.of(library.toUri().toURL()), manager.getLibraries(rules));
            assertEquals(List.of(provided.toUri().toURL()), manager.getProvidedLibraries(rules));
            assertEquals(List.of(libraryProblem, projectProblem), List.copyOf(manager.getDependencyMessages(rules)));
            assertEquals(List.of(), manager.getDependencyMessages(common));
        } finally {
            manager.shutdown();
        }
        verify(resolver, never()).projectOf(commonDependency);
    }

    @Test
    void reportsWhatWentWrongWithTheLibrariesOfAProjectDeclaringNoDependency() {
        var problem = new OpenLMessage("Cannot read the pom.xml of the project: no parent", Severity.ERROR);
        var rules = project("Rules");
        when(resolver.librariesOf(eq(rules), any())).thenReturn(
                new MavenDependencyResolver.Libraries(List.of(), List.of(), List.of(problem)));

        var manager = factory.buildDependencyManager(rules);
        try {
            assertEquals(List.of(problem), List.copyOf(manager.getDependencyMessages(rules)));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void compilesOneProjectOfANameAndTellsTheOtherOneLeftOut() {
        var domain1 = dependency("Domain", "com.example:domain:1.0.0");
        var domain2 = dependency("Domain", "com.example:domain:2.0.0");
        var uses = at(project("UsesDomain", domain1), "/workspace/UsesDomain");
        var custom = at(project("CustomDomain", domain2), "/workspace/CustomDomain");
        sessionProjects.addAll(List.of(uses, custom));
        var downloaded1 = at(project("Domain"), "/maven/com/example/domain/1.0.0/domain-1.0.0-1");
        var downloaded2 = at(project("Domain"), "/maven/com/example/domain/2.0.0/domain-2.0.0-1");
        when(resolver.projectOf(domain1)).thenReturn(new MavenDependencyResolver.DownloadedProject(downloaded1, null));
        when(resolver.projectOf(domain2)).thenReturn(new MavenDependencyResolver.DownloadedProject(downloaded2, null));
        var portal = at(project("Portal", dependency("UsesDomain", null), dependency("CustomDomain", null)),
                "/workspace/Portal");

        var workspace = factory.workspaceOf(portal);

        assertEquals(List.of(portal, uses, downloaded1, custom), List.copyOf(workspace.projects()));
        assertSame(downloaded1, workspace.projects().stream().filter(downloaded1::equals).findFirst().orElseThrow());
        var conflict = new DependencyConflict("Domain",
                "Portal",
                new DependencyConflict.Request("UsesDomain", "com.example:domain:1.0.0"),
                new DependencyConflict.Request("CustomDomain", "com.example:domain:2.0.0"));
        assertEquals(List.of(conflict), workspace.conflicts());
        var manager = factory.buildDependencyManager(portal);
        try {
            var warning = single(manager.getDependencyMessages(portal));
            assertEquals(Severity.WARN, warning.getSeverity());
            assertEquals("Two projects 'Domain' are required: com.example:domain:1.0.0, which 'UsesDomain' depends on, "
                    + "is compiled, and com.example:domain:2.0.0, which 'CustomDomain' depends on, is left out.",
                    warning.getSummary());
            assertEquals(List.of(), manager.getDependencyMessages(custom), "told once, on the project compiled");
            assertEquals(List.of(), manager.getDependencyMessages(uses));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void aProjectReachedTwiceIsNoConflict() {
        var common = at(project("Common"), "/workspace/Common");
        var domain = at(project("Domain", dependency("Common", null)), "/workspace/Domain");
        sessionProjects.addAll(List.of(common, domain));
        var rules = at(project("Rules", dependency("Domain", null), dependency("Common", null)), "/workspace/Rules");

        var workspace = factory.workspaceOf(rules);

        assertEquals(List.of(rules, domain, common), List.copyOf(workspace.projects()));
        assertEquals(List.of(), workspace.conflicts());
    }

    @Test
    void tellsTheProjectOfTheWorkspaceAndTheCompiledOneByTheirNames() {
        var conflict = new DependencyConflict("Domain", "Domain", new DependencyConflict.Request(null, null),
                new DependencyConflict.Request("Rules", null));

        assertEquals("Two projects 'Domain' are required: the project 'Domain' of the workspace is compiled, and the "
                + "project 'Domain' of the workspace, which 'Rules' depends on, is left out.",
                conflict.message().getSummary());
        assertFalse(conflict.concerns("Rules"));
        assertTrue(conflict.concerns("Domain"));
        assertFalse(conflict.concerns("Other"));
    }

    private static ArgumentMatcher<ProjectDependencyDescriptor> named(String name, String coordinates) {
        return dependency -> dependency != null && name.equals(dependency.getName())
                && coordinates.equals(dependency.getMavenArtifact());
    }

    private static ProjectDescriptor at(ProjectDescriptor project, String folder) {
        project.setProjectFolder(Path.of(folder));
        return project;
    }

    private static OpenLMessage single(Collection<OpenLMessage> messages) {
        assertEquals(1, messages.size(), messages::toString);
        return messages.iterator().next();
    }

    private static ProjectDescriptor project(String name, ProjectDependencyDescriptor... dependencies) {
        var project = new ProjectDescriptor();
        project.setName(name);
        project.setDependencies(dependencies.length == 0 ? null : List.of(dependencies));
        return project;
    }

    private static ProjectDependencyDescriptor dependency(String name, String coordinates) {
        var dependency = new ProjectDependencyDescriptor();
        dependency.setName(name);
        dependency.setMavenArtifact(coordinates);
        return dependency;
    }
}
