package org.openl.studio.deployment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.webstudio.dependencies.MavenDependencyResolver;
import org.openl.rules.webstudio.dependencies.ProjectPom;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.deployment.model.DependencySource;
import org.openl.studio.projects.service.ProjectDependency;
import org.openl.studio.projects.service.ProjectDependencyResolver;

class MavenDeploymentPlannerTest {

    @TempDir
    private Path temp;

    private Path remote;
    private MavenDependencyResolver maven;
    private final ProjectDependencyResolver dependencies = mock(ProjectDependencyResolver.class);
    private final RulesProject rules = project("Rules");
    private final RulesProject domain = project("Domain");
    private final RulesProject common = project("Common");

    @BeforeEach
    void setUp() throws IOException {
        remote = Files.createDirectories(temp.resolve("remote"));
        maven = new MavenDependencyResolver(remote.toUri().toString(), null, null, temp.resolve("local").toString());
        publish("com/example/lib/1.0.0/lib-1.0.0.jar", Map.of("lib.txt", "lib"));
        publish("com/example/domain/1.0.0/domain-1.0.0.zip", Map.of("rules.xml", """
                <project><name>Domain</name><dependencies><dependency><name>Shared</name>\
                <mavenArtifact>com.example:shared:1.0.0</mavenArtifact></dependency></dependencies></project>"""));
        publish("com/example/shared/1.0.0/shared-1.0.0.zip",
                Map.of("rules.xml", "<project><name>Shared</name></project>"));
        // Rules depends on Domain, which a design repository has too, and Domain on Common
        when(dependencies.getDependencies(rules)).thenReturn(List.of(
                new ProjectDependency("Domain", domain, false, "com.example:domain:1.0.0"),
                new ProjectDependency("Common", common, true, null)));
        when(dependencies.getDependencies(domain)).thenReturn(List.of(new ProjectDependency("Common", common, false,
                null)));
        when(dependencies.getLibraries(any())).thenReturn(List.of());
        when(dependencies.getLibraries(rules)).thenReturn(List.of("com.example:lib:jar:1.0.0"));
    }

    @AfterEach
    void tearDown() {
        maven.destroy();
    }

    @Test
    void takesTheProjectsOfTheDesignRepositoriesAndDownloadsTheLibraries() {
        var plan = new MavenDeploymentPlanner(dependencies, maven).plan(rules, DependencySource.REPOSITORY);

        assertEquals(List.of(rules, domain, common), plan.repositoryProjects());
        assertEquals(Map.of(), plan.extras().projects());
        assertEquals(List.of("lib-1.0.0.jar"), fileNames(plan.extras().libraries().get("Rules")));
    }

    @Test
    void downloadsTheLibrariesThePomOfAProjectDeclares() throws IOException {
        publish("com/example/packed/2.0.0/packed-2.0.0.jar", Map.of("packed.txt", "packed"));
        when(dependencies.getPom(domain)).thenReturn(ProjectPom.of("""
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>domain</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>packed</artifactId>
                            <version>2.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """));

        var plan = new MavenDeploymentPlanner(dependencies, maven).plan(rules, DependencySource.REPOSITORY);

        assertEquals(List.of("packed-2.0.0.jar"), fileNames(plan.extras().libraries().get("Domain")));
    }

    @Test
    void downloadsADependencyNamedByNameByTheCoordinatesThePomGives() throws IOException {
        publish("com/example/domain-rules/2.0.0/domain-rules-2.0.0.zip",
                Map.of("rules.xml", "<project><name>Domain</name></project>"));
        when(dependencies.getPom(rules)).thenReturn(ProjectPom.of("""
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>rules</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>domain-rules</artifactId>
                            <version>2.0.0</version>
                            <type>zip</type>
                        </dependency>
                    </dependencies>
                </project>
                """));

        var plan = new MavenDeploymentPlanner(dependencies, maven).plan(rules, DependencySource.MAVEN);

        assertEquals(List.of(rules), plan.repositoryProjects(), "the archive of Domain names no Common");
        assertTrue(plan.extras().projects().get("Domain").toString().contains("2.0.0"),
                "the pom.xml wins over the coordinates of rules.xml");
    }

    /** A name nothing is found for stays open: a later project of the walk gives its coordinates, as it compiles. */
    @Test
    void downloadsADependencyALaterProjectGivesTheCoordinatesOf() {
        when(dependencies.getDependencies(rules)).thenReturn(List.of(
                new ProjectDependency("Shared", null, false, null),
                new ProjectDependency("Domain", domain, false, null)));
        when(dependencies.getDependencies(domain)).thenReturn(List.of(
                new ProjectDependency("Shared", null, false, "com.example:shared:1.0.0")));

        var plan = new MavenDeploymentPlanner(dependencies, maven).plan(rules, DependencySource.REPOSITORY);

        assertEquals(List.of(rules, domain), plan.repositoryProjects());
        assertEquals(Set.of("Shared"), plan.extras().projects().keySet());
    }

    @Test
    void takesTheArchivesOfTheMavenRepositoryWhenAsked() {
        var plan = new MavenDeploymentPlanner(dependencies, maven).plan(rules, DependencySource.MAVEN);

        assertEquals(List.of(rules), plan.repositoryProjects(),
                "Common is needed by the Domain of the repository only");
        assertEquals(List.of("Domain", "Shared"), List.copyOf(plan.extras().projects().keySet()));
        assertTrue(Files.isRegularFile(plan.extras().projects().get("Shared").resolve("rules.xml")));
    }

    @Test
    void downloadsAProjectNoDesignRepositoryHas() {
        when(dependencies.getDependencies(rules)).thenReturn(List.of(new ProjectDependency("Domain", null, false,
                "com.example:domain:1.0.0")));

        var plan = new MavenDeploymentPlanner(dependencies, maven).plan(rules, DependencySource.REPOSITORY);

        assertEquals(List.of("Domain", "Shared"), List.copyOf(plan.extras().projects().keySet()));
    }

    @Test
    void holdsTheProjectOfANameTheProjectIsCompiledWith() throws IOException {
        publish("com/example/domain/2.0.0/domain-2.0.0.zip",
                Map.of("rules.xml", "<project><name>Domain</name></project>"));
        // Common, declared first, reaches Domain 1.0.0 before Rules names Domain 2.0.0 itself, as a compilation does
        when(dependencies.getDependencies(rules)).thenReturn(List.of(
                new ProjectDependency("Common", common, false, null),
                new ProjectDependency("Domain", null, false, "com.example:domain:2.0.0")));
        when(dependencies.getDependencies(common)).thenReturn(List.of(
                new ProjectDependency("Domain", null, false, "com.example:domain:1.0.0")));

        var plan = new MavenDeploymentPlanner(dependencies, maven).plan(rules, DependencySource.REPOSITORY);

        assertEquals(List.of(rules, common), plan.repositoryProjects());
        assertTrue(plan.extras().projects().get("Domain").toString().contains("1.0.0"),
                plan.extras().projects().toString());
    }

    @Test
    void holdsTheDependencyProjectsTheWalkFromTheDeployedProjectResolves() {
        // Domain, shown on another branch, finds another Common there; Rules is compiled with the Common of its branch
        var otherCommon = project("Common");
        when(dependencies.getDependencies(domain)).thenReturn(List.of(new ProjectDependency("Common", otherCommon,
                false, null)));

        var plan = new MavenDeploymentPlanner(dependencies, maven).plan(rules, DependencySource.REPOSITORY);

        assertEquals(List.of(rules, domain, common), plan.repositoryProjects());
    }

    @Test
    void refusesWhatCannotBeDownloaded() {
        when(dependencies.getDependencies(rules)).thenReturn(List.of(new ProjectDependency("Missing", null, false,
                "com.example:missing:1.0.0")));
        when(dependencies.getLibraries(rules)).thenReturn(List.of("com.example:absent:jar:1.0.0"));
        var planner = new MavenDeploymentPlanner(dependencies, maven);

        var refusal = assertThrows(ConflictException.class, () -> planner.plan(rules, DependencySource.REPOSITORY));

        assertEquals("openl.error.409.project.deploy.maven.message", refusal.getErrorCode());
        var reason = (String) refusal.getArgs()[0];
        assertTrue(reason.contains("com.example:missing:zip:1.0.0") && reason.contains("absent"), reason);
    }

    @Test
    void deploysAReadOnlyProjectAsItIsUnpackedWithTheLibrariesItCarries() {
        var root = temp.resolve("downloads");
        var local = mock(LocalRepository.class);
        when(local.getRoot()).thenReturn(root);
        when(rules.isReadOnly()).thenReturn(true);
        when(rules.getLocalRepository()).thenReturn(local);
        when(rules.getFolderPath()).thenReturn("rules-1.0.0");

        var plan = new MavenDeploymentPlanner(dependencies, maven).plan(rules, DependencySource.MAVEN);

        assertEquals(List.of("Rules", "Domain", "Shared"), List.copyOf(plan.extras().projects().keySet()));
        assertEquals(root.resolve("rules-1.0.0"), plan.extras().projects().get("Rules"));
        assertEquals(List.of(), plan.repositoryProjects());
        assertFalse(plan.extras().libraries().containsKey("Rules"), "the archive carries its libraries");
    }

    @Test
    void holdsTheProjectsOfTheDesignRepositoriesOnlyWhileNoMavenRepositoryIsSet() {
        var disabled = new MavenDependencyResolver("", null, null, temp.resolve("local").toString());
        when(dependencies.getDependencies(rules)).thenReturn(List.of(
                new ProjectDependency("Domain", domain, false, "com.example:domain:1.0.0"),
                new ProjectDependency("Missing", null, false, "com.example:missing:1.0.0")));

        var plan = new MavenDeploymentPlanner(dependencies, disabled).plan(rules, DependencySource.MAVEN);

        assertEquals(List.of(rules, domain, common), plan.repositoryProjects());
        assertEquals(Map.of(), plan.extras().projects());
        assertEquals(List.of(), plan.extras().libraries().values().stream().flatMap(List::stream).toList());
    }

    private static RulesProject project(String name) {
        var project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn(name);
        return project;
    }

    private static List<String> fileNames(List<Path> files) {
        return files.stream().map(file -> file.getFileName().toString()).toList();
    }

    private void publish(String path, Map<String, String> entries) throws IOException {
        var file = remote.resolve(path);
        Files.createDirectories(file.getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes());
                zip.closeEntry();
            }
        }
    }
}
