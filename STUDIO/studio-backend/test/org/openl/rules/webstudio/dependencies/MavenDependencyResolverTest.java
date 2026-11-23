package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.studio.dependencies.service.MavenCoordinates;

class MavenDependencyResolverTest {

    @TempDir
    private Path temp;

    private Path remote;
    private Path local;
    private final MutableClock clock = new MutableClock();
    private MavenDependencyResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        remote = Files.createDirectories(temp.resolve("remote"));
        local = temp.resolve("local");
        resolver = new MavenDependencyResolver(remote.toUri().toString(), null, null, local, clock);
    }

    @AfterEach
    void tearDown() {
        resolver.destroy();
    }

    @Test
    void downloadsNothingWhileNoRepositoryIsSet() throws IOException {
        var disabled = new MavenDependencyResolver(" ", null, null, local, clock);

        assertFalse(disabled.isEnabled());
        assertSame(MavenDependencyResolver.Libraries.NONE,
                librariesOf(disabled, project("Rules", library("com.example:lib:jar:1.0.0"))));
        assertSame(MavenDependencyResolver.DownloadedProject.NONE,
                disabled.projectOf(projectDependency("Domain", "com.example:domain:1.0.0")));
        assertSame(MavenDependencyResolver.DownloadedProject.NONE, disabled.projectAt("com.example:domain:1.0.0"));
        disabled.destroy();
    }

    @Test
    void downloadsNoLibraryForAProjectWithoutOne() throws IOException {
        assertTrue(resolver.isEnabled());
        assertSame(MavenDependencyResolver.Libraries.NONE, librariesOf(resolver, project("Rules")));
        assertSame(MavenDependencyResolver.Libraries.NONE,
                librariesOf(resolver, project("Rules", projectDependency("Domain", "com.example:domain:1.0.0"))));
    }

    @Test
    void downloadsALibraryWithTheLibrariesItNeedsAtRuntime() throws Exception {
        publish("com.example", "transitive", "1.0.0", "jar", null);
        publish("com.example", "testing", "1.0.0", "jar", null);
        publish("com.example", "lib", "1.0.0", "jar", """
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>transitive</artifactId>
                        <version>1.0.0</version>
                        <scope>runtime</scope>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>testing</artifactId>
                        <version>1.0.0</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                """);

        var libraries = librariesOf(resolver, project("Rules",
                library("com.example:lib:jar:1.0.0"),
                projectDependency("Domain", null)));

        assertEquals(List.of(), libraries.messages());
        assertEquals(List.of("lib-1.0.0.jar", "transitive-1.0.0.jar"), fileNames(libraries.files()));
        for (Path file : libraries.files()) {
            assertTrue(file.startsWith(local), "kept in the local repository: " + file);
        }
    }

    @Test
    void reportsALibraryItCannotDownloadAndKeepsTheOthers() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);

        var libraries = librariesOf(resolver, project("Rules",
                library("com.example:lib:jar:1.0.0"),
                library("com.example:missing:jar:2.0.0")));

        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
        var message = single(libraries.messages());
        assertEquals(Severity.ERROR, message.getSeverity());
        assertTrue(message.getSummary().startsWith("Cannot download a Maven dependency: "), message.getSummary());
        assertTrue(message.getSummary().contains("com.example:missing:jar:2.0.0"), message.getSummary());
    }

    @Test
    void reportsCoordinatesThatAreNotValid() throws IOException {
        var libraries = librariesOf(resolver, project("Rules", library("not-coordinates")));

        assertEquals(List.of(), libraries.files());
        assertEquals("The Maven coordinates 'not-coordinates' are not valid.",
                single(libraries.messages()).getSummary());
    }

    @Test
    void readsTheLibrariesOfThePomAsMavenDoes() throws Exception {
        publishPom("com.example", "bom", "1.0.0", """
                <packaging>pom</packaging>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>managed</artifactId>
                            <version>2.0.0</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                """);
        publishPom("com.example", "parent", "1.0.0", """
                <packaging>pom</packaging>
                <properties>
                    <lib.version>1.0.0</lib.version>
                </properties>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>bom</artifactId>
                            <version>1.0.0</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>transitive</artifactId>
                            <version>3.0.0</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                """);
        publish("com.example", "transitive", "3.0.0", "jar", null);
        publish("com.example", "lib", "1.0.0", "jar", """
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>transitive</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
                """);
        publish("com.example", "managed", "2.0.0", "jar", null);
        publish("com.example", "packed", "1.0.0", "jar", null);
        publishProject("com.example", "domain", "1.0.0", "Domain");
        Files.writeString(artifactPath("com.example", "domain", "1.0.0", null, "pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>domain</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>packed</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """);
        var rules = projectWithPom("""
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>lib</artifactId>
                        <version>${lib.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>managed</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>domain</artifactId>
                        <version>1.0.0</version>
                        <type>zip</type>
                    </dependency>
                    <dependency>
                        <groupId>org.openl.rules</groupId>
                        <artifactId>org.openl.rules</artifactId>
                        <version>${openl.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>testing</artifactId>
                        <version>1.0.0</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                """);

        var libraries = librariesOf(resolver, rules);

        assertEquals(List.of(), libraries.messages());
        assertEquals(List.of("lib-1.0.0.jar", "managed-2.0.0.jar", "transitive-3.0.0.jar"),
                fileNames(libraries.files()),
                "the archive of an OpenL project brings its own libraries, and OpenL is no library");
    }

    @Test
    void downloadsTheProvidedLibrariesOpenLStudioDoesNotShip() throws Exception {
        publish("com.example", "helper", "1.0.0", "jar", null);
        publish("com.example", "embedded", "1.0.0", "jar", """
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>helper</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
                """);
        publish("com.example", "lib", "1.0.0", "jar", null);
        var rules = projectWithPom("""
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>lib</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>embedded</artifactId>
                        <version>1.0.0</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-lang3</artifactId>
                        <version>0.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>0.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
                """);

        var libraries = librariesOf(resolver, rules);

        assertEquals(List.of(), libraries.messages(), "what OpenL Studio ships is not downloaded, a jar without a "
                + "Maven descriptor told by its name");
        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
        assertEquals(List.of("embedded-1.0.0.jar", "helper-1.0.0.jar"), fileNames(libraries.provided()));
    }

    @Test
    void findsTheOpenLProjectsOfThePomByTheirNamesAsThePluginDoes() throws Exception {
        publishProject("com.example", "domain", "1.0.0", "Domain");
        Files.writeString(artifactPath("com.example", "domain", "1.0.0", null, "pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>domain</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>common-rules</artifactId>
                            <version>2.0.0</version>
                            <type>zip</type>
                        </dependency>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """);
        publishProject("com.example", "common-rules", "2.0.0", "Common");
        writeZip(artifactPath("com.example", "assets", "1.0.0", null, "zip"), Map.of("logo.png", "png"));
        var rules = projectWithPom("""
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>domain</artifactId>
                        <version>1.0.0</version>
                        <type>zip</type>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>assets</artifactId>
                        <version>1.0.0</version>
                        <type>zip</type>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>absent</artifactId>
                        <version>1.0.0</version>
                        <type>zip</type>
                    </dependency>
                </dependencies>
                """);

        var projects = resolver.projectsOfPom(ProjectPoms.inFolder(rules.getProjectFolder(), temp));

        assertEquals(Map.of("Domain", "com.example:domain:1.0.0", "Common", "com.example:common-rules:2.0.0"),
                projects.coordinatesByName(), "an archive that is no OpenL project is no project");
        assertTrue(single(projects.messages()).getSummary().contains("com.example:absent:zip:1.0.0"),
                projects.messages()::toString);
        assertSame(MavenDependencyResolver.PomProjects.NONE, resolver.projectsOfPom((ProjectPom) null));
        var dependencies = MavenDependencyResolver.namedMavenDependenciesOf(List.of(projectDependency("Domain", null),
                projectDependency("Common", "com.example:common-rules:1.0.0"),
                projectDependency("Other", null),
                library("com.example:lib:jar:1.0.0")), projects.coordinatesByName());
        assertEquals(List.of("Domain=com.example:domain:1.0.0", "Common=com.example:common-rules:2.0.0"),
                dependencies.stream().map(dependency -> dependency.getName() + "=" + dependency.getMavenArtifact())
                        .toList(), "the pom.xml decides");
    }

    @Test
    void takesWhatAPomDependencyBringsButNotItsPomNorATypeOfABuildExtension() throws Exception {
        publish("com.example", "bundled", "1.0.0", "jar", null);
        publishPom("com.example", "bundle", "1.0.0", """
                <packaging>pom</packaging>
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>bundled</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
                """);

        var libraries = resolver.librariesNamed(List.of(), ProjectPom.of(pom("""
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>bundle</artifactId>
                        <version>1.0.0</version>
                        <type>pom</type>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>message-bundle-plugin</artifactId>
                        <version>1.0.0</version>
                        <classifier>extension</classifier>
                        <type>tile</type>
                    </dependency>
                </dependencies>
                """)));

        assertEquals(List.of(), libraries.messages(), "a tile is for the build that loads its extension");
        assertEquals(List.of("bundled-1.0.0.jar"), fileNames(libraries.files()));
    }

    @Test
    void readsTheParentOfABuildOfSeveralModulesBesideThePom() throws Exception {
        publishPom("com.example", "corporate", "1.0.0", """
                <packaging>pom</packaging>
                <properties>
                    <lib.version>1.0.0</lib.version>
                </properties>
                """);
        publish("com.example", "lib", "1.0.0", "jar", null);
        Files.createDirectories(temp.resolve("workspace"));
        Files.writeString(temp.resolve("workspace/pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>corporate</artifactId>
                        <version>1.0.0</version>
                        <relativePath/>
                    </parent>
                    <groupId>com.example</groupId>
                    <artifactId>modules</artifactId>
                    <version>2.0.0-SNAPSHOT</version>
                    <packaging>pom</packaging>
                </project>
                """);
        var rules = projectWithPom("""
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>modules</artifactId>
                    <version>2.0.0-SNAPSHOT</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>lib</artifactId>
                        <version>${lib.version}</version>
                    </dependency>
                </dependencies>
                """);

        var libraries = librariesOf(resolver, rules);

        assertEquals(List.of(), libraries.messages(), "the Maven repository has no such parent");
        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
    }

    /** A module of the same build is built with it, as Maven takes it from the reactor: it is not downloaded. */
    @Test
    void leavesTheModulesOfItsBuildToTheBuild() throws Exception {
        publishProject("com.example", "domain", "1.0.0", "Domain");
        Files.createDirectories(temp.resolve("workspace/Rules"));
        Files.createDirectories(temp.resolve("workspace/group/tables"));
        Files.createDirectories(temp.resolve("workspace/group/common"));
        Files.writeString(temp.resolve("workspace/group/pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>modules</artifactId>
                        <version>2.0.0-SNAPSHOT</version>
                    </parent>
                    <artifactId>group</artifactId>
                    <packaging>pom</packaging>
                    <modules>
                        <module>tables</module>
                        <module>common</module>
                    </modules>
                </project>
                """);
        Files.writeString(temp.resolve("workspace/pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>modules</artifactId>
                    <version>2.0.0-SNAPSHOT</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>Rules</module>
                        <module>group</module>
                        <module>gone</module>
                    </modules>
                </project>
                """);
        for (var module : Map.of("tables", "tables", "common", "common-rules").entrySet()) {
            Files.writeString(temp.resolve("workspace/group").resolve(module.getKey()).resolve("pom.xml"), """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>com.example</groupId>
                            <artifactId>group</artifactId>
                            <version>2.0.0-SNAPSHOT</version>
                        </parent>
                        <artifactId>%s</artifactId>
                    </project>
                    """.formatted(module.getValue()));
        }
        Files.writeString(temp.resolve("workspace/Rules/pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>modules</artifactId>
                        <version>2.0.0-SNAPSHOT</version>
                    </parent>
                    <artifactId>rules</artifactId>
                    <dependencies>
                        %s
                        %s
                        %s
                        %s
                    </dependencies>
                </project>
                """.formatted(zip("tables", "${project.version}"), zip("common-rules", "${project.version}"),
                zip("domain", "1.0.0"), zip("absent", "${project.version}")));

        var found = resolver.projectsOfPom(ProjectPoms.inFolder(temp.resolve("workspace/Rules"), temp));

        assertEquals(Map.of("Domain", "com.example:domain:1.0.0"), found.coordinatesByName());
        var messages = found.messages().stream().map(OpenLMessage::getSummary).toList();
        assertEquals(1, messages.size(), messages.toString());
        assertTrue(messages.getFirst().contains("absent"), "no module of the build is that one");
    }

    private static String zip(String artifactId, String version) {
        return """
                <dependency>
                    <groupId>com.example</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    <type>zip</type>
                </dependency>
                """.formatted(artifactId, version);
    }

    /** What the repository missed is not asked for by another resolution, until the answers kept are renewed. */
    @Test
    void asksForWhatTheRepositoryMissedOnceAWhile() throws Exception {
        assertEquals(1, resolver.librariesNamed(List.of("com.example:lib:jar:1.0.0"), null).messages().size());
        publish("com.example", "lib", "1.0.0", "jar", null);
        publish("com.example", "other", "1.0.0", "jar", null);
        var both = List.of("com.example:lib:jar:1.0.0", "com.example:other:jar:1.0.0");

        var meanwhile = resolver.librariesNamed(both, null);
        assertEquals(List.of("other-1.0.0.jar"), fileNames(meanwhile.files()));
        assertEquals(1, meanwhile.messages().size(), "the missing one is not asked for again");

        clock.advance(MavenDependencyResolver.CACHE_PERIOD);
        var later = resolver.librariesNamed(both, null);
        assertEquals(List.of(), later.messages());
        assertEquals(List.of("lib-1.0.0.jar", "other-1.0.0.jar"), fileNames(later.files()));
    }

    @Test
    void forgetsWhatTheRepositoryMissedWhenTheLocalRepositoryIsEmptied() throws Exception {
        assertEquals(1, resolver.librariesNamed(List.of("com.example:lib:jar:1.0.0"), null).messages().size());
        publish("com.example", "lib", "1.0.0", "jar", null);

        resolver.clearLocalRepository();

        assertEquals(List.of("lib-1.0.0.jar"),
                fileNames(resolver.librariesNamed(List.of("com.example:lib:jar:1.0.0"), null).files()));
    }

    /** A provided library takes what it needs with it, even what a library of the project needs too. */
    @Test
    void readsWhatAProvidedLibraryNeedsWithIt() throws Exception {
        publish("com.example", "model", "1.0.0", "jar", null);
        publish("com.example", "common", "1.0.0", "jar", null);
        publish("com.example", "api", "1.0.0", "jar", dependencies(dependency("model", "1.0.0", null),
                dependency("common", "0.9.0", null)));
        var rules = projectWithPom("""
                <dependencyManagement>
                    <dependencies>
                        %s
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    %s
                    %s
                </dependencies>
                """.formatted(dependency("common", "1.0.0", null), dependency("model", "1.0.0", null),
                dependency("api", "1.0.0", "provided")));

        var libraries = librariesOf(resolver, rules);

        assertEquals(List.of(), libraries.messages());
        assertEquals(List.of("model-1.0.0.jar"), fileNames(libraries.files()));
        assertEquals(List.of("api-1.0.0.jar", "common-1.0.0.jar", "model-1.0.0.jar"), fileNames(libraries.provided()),
                "a library the dependency management sets the version of keeps the scope of the provided one");
    }

    /** A deployment carries no provided library, so one the repository lacks does not stop it. */
    @Test
    void deploysTheLibrariesButTheProvidedOnes() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);
        var rules = projectWithPom("""
                <dependencies>
                    %s
                    %s
                </dependencies>
                """.formatted(dependency("lib", "1.0.0", null), dependency("runtime-ext", "1.0.0", "provided")));
        var pom = ProjectPoms.inFolder(rules.getProjectFolder(), temp);

        var deployed = resolver.librariesToDeploy(List.of(), pom);

        assertEquals(List.of(), deployed.messages());
        assertEquals(List.of("lib-1.0.0.jar"), fileNames(deployed.files()));
        assertEquals(List.of(), deployed.provided());
        assertEquals(1, resolver.librariesNamed(List.of(), pom).messages().size(), "compiled, it is missing");
    }

    /** Only the repository set is read, not those the POMs of the dependencies declare. */
    @Test
    void readsOnlyTheRepositorySet() throws Exception {
        var other = Files.createDirectories(temp.resolve("other/com/example/hidden/1.0.0"));
        writeZip(other.resolve("hidden-1.0.0.jar"), Map.of("META-INF/MANIFEST.MF", ""));
        Files.writeString(other.resolve("hidden-1.0.0.pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>hidden</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        publish("com.example", "lib", "1.0.0", "jar", """
                <repositories>
                    <repository>
                        <id>other</id>
                        <url>%s</url>
                    </repository>
                </repositories>
                %s
                """.formatted(temp.resolve("other").toUri(), dependencies(dependency("hidden", "1.0.0", null))));

        var libraries = resolver.librariesNamed(List.of("com.example:lib:jar:1.0.0"), null);

        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
        assertEquals(1, libraries.messages().size(), "the repository the POM declares is not read");
    }

    /** What a dependency manager or a session holds stays, though no answer in memory names it any more. */
    @Test
    void keepsWhatIsHeldFromACleanup() throws Exception {
        publish("com.example", "held", "1.0.0", "jar", null);
        publish("com.example", "other", "1.0.0", "jar", null);
        var holder = new Object();
        resolver.hold(holder, librariesOf(resolver, project("Old", library("com.example:held:jar:1.0.0"))).files());
        clock.advance(MavenDependencyResolver.CACHE_PERIOD.plusSeconds(1));
        librariesOf(resolver, project("New", library("com.example:other:jar:1.0.0")));

        resolver.cleanLocalRepository(null, 1);

        assertTrue(Files.isDirectory(local.resolve("com/example/held/1.0.0")));
        java.lang.ref.Reference.reachabilityFence(holder);
    }

    /** A jar named after the artifact is the library only when it holds classes of its group. */
    @Test
    void tellsAShippedJarByTheClassesOfTheGroup() throws Exception {
        var utils = temp.resolve("utils-2.57.9.jar");
        writeZip(utils, Map.of("software/amazon/awssdk/utils/Validate.class", ""));
        var beans = temp.resolve("spring-beans-6.2.0.jar");
        writeZip(beans, Map.of("org/springframework/beans/BeanUtils.class", ""));

        assertTrue(MavenDependencyResolver.ShippedJars.holdsClassesOf(utils, "software.amazon.awssdk"));
        assertFalse(MavenDependencyResolver.ShippedJars.holdsClassesOf(utils, "com.acme"));
        assertTrue(MavenDependencyResolver.ShippedJars.holdsClassesOf(beans, "org.springframework"));
        assertTrue(MavenDependencyResolver.ShippedJars.holdsClassesOf(beans, "org.springframework.boot"),
                "a group of the company holds classes in its package");
        assertFalse(MavenDependencyResolver.ShippedJars.holdsClassesOf(temp.resolve("absent.jar"), "com.acme"));
    }

    private static String dependencies(String... dependencies) {
        return "<dependencies>" + String.join("", dependencies) + "</dependencies>";
    }

    private static String dependency(String artifactId, String version, String scope) {
        return """
                <dependency>
                    <groupId>com.example</groupId>
                    <artifactId>%s</artifactId>
                    %s
                    %s
                </dependency>
                """.formatted(artifactId,
                version == null ? "" : "<version>" + version + "</version>",
                scope == null ? "" : "<scope>" + scope + "</scope>");
    }

    @Test
    void thePomDecidesOfALibraryBothName() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);
        publish("com.example", "other", "2.0.0", "jar", null);
        var rules = projectWithPom("""
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>other</artifactId>
                            <version>2.0.0</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>lib</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
                """, library("com.example:lib:jar:9.0.0"), library("com.example:other:jar:0.1.0"));

        var libraries = librariesOf(resolver, rules);

        assertEquals(List.of(), libraries.messages());
        assertEquals(List.of("lib-1.0.0.jar", "other-2.0.0.jar"), fileNames(libraries.files()));
    }

    @Test
    void readsTheHighestParentOfARangeAndOnlyTheRepositoryItIsSetTo() throws Exception {
        for (var version : List.of("1.0.0", "1.1.0", "2.0.0")) {
            publishPom("com.example", "parent", version, """
                    <packaging>pom</packaging>
                    <properties>
                        <lib.version>%s</lib.version>
                    </properties>
                    """.formatted(version));
            publish("com.example", "lib", version, "jar", null);
        }
        Files.writeString(remote.resolve("com/example/parent/maven-metadata.xml"), """
                <metadata>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <versioning>
                        <versions>
                            <version>1.0.0</version>
                            <version>1.1.0</version>
                            <version>2.0.0</version>
                        </versions>
                    </versioning>
                </metadata>
                """);
        var rules = projectWithPom("""
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>[1.0.0,2.0.0)</version>
                </parent>
                <repositories>
                    <repository>
                        <id>elsewhere</id>
                        <url>https://repository.invalid/maven</url>
                    </repository>
                </repositories>
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>lib</artifactId>
                        <version>${lib.version}</version>
                    </dependency>
                </dependencies>
                """);

        var libraries = librariesOf(resolver, rules);

        assertEquals(List.of(), libraries.messages());
        assertEquals(List.of("lib-1.1.0.jar"), fileNames(libraries.files()));
    }

    @Test
    void reportsAPomItCannotReadAndDownloadsTheOtherLibraries() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);
        var rules = projectWithPom("""
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>absent</artifactId>
                    <version>1.0.0</version>
                </parent>
                """, library("com.example:lib:jar:1.0.0"));

        var libraries = librariesOf(resolver, rules);

        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
        var message = single(libraries.messages()).getSummary();
        assertTrue(message.startsWith("Cannot read the pom.xml of the project: "), message);
        assertTrue(message.contains("com.example:absent"), message);
    }

    @Test
    void leavesOutTheLibrariesOpenLBringsItself() throws Exception {
        publish("org.slf4j", "slf4j-api", "2.0.0", "jar", null);
        publish("com.example", "logging", "1.0.0", "jar", """
                <dependencies>
                    <dependency>
                        <groupId>org.slf4j</groupId>
                        <artifactId>slf4j-api</artifactId>
                        <version>2.0.0</version>
                    </dependency>
                </dependencies>
                """);
        publish("org.openl.rules", "org.openl.rules.util", "6.0.0", "jar", """
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>logging</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
                """);

        var libraries = librariesOf(resolver, project("Rules",
                library("org.openl.rules:org.openl.rules.util:jar:6.0.0"),
                library("com.example:logging:jar:1.0.0")));

        assertEquals(List.of(), libraries.messages());
        assertEquals(List.of("logging-1.0.0.jar"), fileNames(libraries.files()));
    }

    @Test
    void tellsTheFolderOfADownloadedProject() throws Exception {
        var folder = Files.createDirectories(local.resolve("com/example/domain/1.0.0/domain-1.0.0-1"));

        assertTrue(resolver.isDownloaded(folder), "the OpenL Maven Plugin packed its libraries into it");
        assertFalse(resolver.isDownloaded(Files.createDirectories(temp.resolve("workspace/Rules"))));
    }

    @Test
    void asksTheRepositoryAgainOnlyAfterAWhile() throws Exception {
        var project = project("Rules", library("com.example:lib:jar:1.0.0"));
        assertEquals(1, librariesOf(resolver, project).messages().size());

        publish("com.example", "lib", "1.0.0", "jar", null);
        clock.advance(MavenDependencyResolver.CACHE_PERIOD.minusSeconds(1));
        assertEquals(1, librariesOf(resolver, project).messages().size(), "the failure is kept for a while");

        clock.advance(MavenDependencyResolver.CACHE_PERIOD);
        var libraries = librariesOf(resolver, project);
        assertEquals(List.of(), libraries.messages());
        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
    }

    @Test
    void forgetsWhatNoProjectAsksForAnyMore() throws Exception {
        publish("com.example", "old", "1.0.0", "jar", null);
        publish("com.example", "new", "1.0.0", "jar", null);
        librariesOf(resolver, project("Rules", library("com.example:old:jar:1.0.0")));
        assertEquals(1, resolver.cachedAnswers());

        // rules.xml names another library now, and the old answer outlives the period unasked
        clock.advance(MavenDependencyResolver.CACHE_PERIOD.plusSeconds(1));
        librariesOf(resolver, project("Rules", library("com.example:new:jar:1.0.0")));

        assertEquals(1, resolver.cachedAnswers());
    }

    @Test
    void recordsTheUseOfWhatItResolves() throws Exception {
        publishParent("com.example", "parent", "3");
        publishWithParent("com.example", "lib", "1.0.0", "parent", "3");
        publishProject("com.example", "domain", "1.0.0", "Domain");

        librariesOf(resolver, project("Rules", library("com.example:lib:jar:1.0.0")));
        resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        // The parent POM is read to work the dependencies out, so it is used as much as the jar
        for (var version : List.of("com/example/lib/1.0.0", "com/example/parent/3", "com/example/domain/1.0.0")) {
            var marker = local.resolve(version).resolve(LocalMavenRepository.LAST_USED);
            assertEquals(clock.instant(), Files.getLastModifiedTime(marker).toInstant(), version);
        }
    }

    @Test
    void emptiesALocalRepositoryGivenByARelativePath() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);
        var relative = Path.of("").toAbsolutePath().relativize(local);
        var relativeResolver = new MavenDependencyResolver(remote.toUri().toString(), null, null, relative, clock);
        try {
            librariesOf(relativeResolver, project("Rules", library("com.example:lib:jar:1.0.0")));

            assertTrue(Files.exists(local.resolve("com/example/lib/1.0.0").resolve(LocalMavenRepository.LAST_USED)));
            assertEquals(1, relativeResolver.clearLocalRepository().versions());
            assertFalse(Files.exists(local.resolve("com/example/lib/1.0.0")));
        } finally {
            relativeResolver.destroy();
        }
    }

    @Test
    void cleansWhatNoAnswerInMemoryNames() throws Exception {
        publish("com.example", "kept", "1.0.0", "jar", null);
        publish("com.example", "dropped", "1.0.0", "jar", null);
        librariesOf(resolver, project("Old", library("com.example:dropped:jar:1.0.0")));
        clock.advance(MavenDependencyResolver.CACHE_PERIOD.plusSeconds(1));
        librariesOf(resolver, project("New", library("com.example:kept:jar:1.0.0")));

        // Any size is too large: only what the projects read now stays
        var removal = resolver.cleanLocalRepository(null, 1);

        assertEquals(1, removal.versions());
        assertFalse(Files.exists(local.resolve("com/example/dropped")));
        assertTrue(Files.isDirectory(local.resolve("com/example/kept/1.0.0")));
        assertTrue(resolver.localRepositorySize() > 0);
    }

    @Test
    void emptiesTheLocalRepositoryAndForgetsEveryAnswer() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);
        var project = project("Rules", library("com.example:lib:jar:1.0.0"));
        var first = librariesOf(resolver, project);

        var removal = resolver.clearLocalRepository();

        assertEquals(1, removal.versions());
        assertEquals(0, resolver.cachedAnswers());
        assertFalse(Files.exists(local.resolve("com/example/lib/1.0.0")));
        var again = librariesOf(resolver, project);
        assertEquals(fileNames(first.files()), fileNames(again.files()), "downloaded again when asked");
    }

    @Test
    void asksForAGroupThePrefixesOfTheRepositoryLeaveOut() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);
        Files.createDirectories(remote.resolve(".meta"));
        Files.writeString(remote.resolve(".meta/prefixes.txt"), "## repository-prefixes/2.0\n/org\n");

        var libraries = librariesOf(resolver, project("Rules", library("com.example:lib:jar:1.0.0")));

        assertEquals(List.of(), libraries.messages(), "a manager may list the prefixes of a proxy alone");
        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
    }

    @Test
    void downloadsAndUnpacksAnOpenLProject() throws Exception {
        publishProject("com.example", "domain", "1.0.0", "Domain");

        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.problem());
        var project = downloaded.project();
        assertNotNull(project);
        assertEquals("Domain", project.getName());
        assertTrue(project.getProjectFolder().startsWith(local));
        assertTrue(Files.isRegularFile(project.getProjectFolder().resolve("rules.xml")));
        assertSame(downloaded, resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0")),
                "the same download is answered while it is kept");
    }

    @Test
    void unpacksADownloadedProjectOnce() throws Exception {
        publishProject("com.example", "domain", "1.0.0", "Domain");
        var first = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0")).project();
        clock.advance(MavenDependencyResolver.CACHE_PERIOD.plusSeconds(1));

        var second = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0")).project();

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getProjectFolder(), second.getProjectFolder());
    }

    @Test
    void refusesAProjectOfAnotherName() throws Exception {
        publishProject("com.example", "domain", "1.0.0", "Other");

        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.project());
        assertEquals("'com.example:domain:1.0.0' is the project 'Other', not the dependency 'Domain'.",
                downloaded.problem().getSummary());
    }

    @Test
    void downloadsAProjectByItsCoordinatesWhateverItsName() throws Exception {
        publishProject("com.example", "domain", "1.0.0", "Other");

        var downloaded = resolver.projectAt("com.example:domain:1.0.0");

        assertNull(downloaded.problem());
        assertEquals("Other", downloaded.project().getName());
        assertSame(downloaded, resolver.projectAt("com.example:domain:1.0.0"), "kept as a dependency is");
        assertEquals("The Maven coordinates 'com.example:domain' are not valid.",
                resolver.projectAt("com.example:domain").problem().getSummary());
        writeZip(artifactPath("com.example", "none", "1.0.0", null, "zip"), Map.of("readme.txt", "Nothing"));
        assertEquals("'com.example:none:1.0.0' is not an OpenL project.",
                resolver.projectAt("com.example:none:1.0.0").problem().getSummary());
    }

    @Test
    void refusesAnArchiveThatIsNoOpenLProject() throws Exception {
        writeZip(artifactPath("com.example", "domain", "1.0.0", null, "zip"), Map.of("readme.txt", "Nothing"));

        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.project());
        assertEquals("'com.example:domain:1.0.0' of the dependency 'Domain' is not an OpenL project.",
                downloaded.problem().getSummary());
    }

    @Test
    void reportsAProjectItCannotDownload() {
        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.project());
        assertEquals(Severity.ERROR, downloaded.problem().getSeverity());
        assertTrue(downloaded.problem().getSummary().contains("com.example:domain:zip:1.0.0"),
                downloaded.problem().getSummary());
    }

    @Test
    void reportsProjectCoordinatesThatAreNotValid() {
        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain"));

        assertEquals("The Maven coordinates 'com.example:domain' of the dependency 'Domain' are not valid.",
                downloaded.problem().getSummary());
    }

    @Test
    void readsCoordinatesAsTheOpenLMavenPluginDoes() {
        var archive = MavenDependencyResolver.artifactOf("com.example:domain:1.0.0");
        assertEquals("com.example:domain:zip:1.0.0", archive.toString());

        var jar = MavenDependencyResolver.artifactOf(" com.example:lib:jar:2.0.0 ");
        assertEquals("com.example:lib:jar:2.0.0", jar.toString());

        var classified = MavenDependencyResolver.artifactOf("com.example:lib:jar:tests:2.0.0");
        assertEquals("tests", classified.getClassifier());
        assertEquals("jar", classified.getExtension());

        assertNull(MavenDependencyResolver.artifactOf("com.example:lib"));
        assertNull(MavenDependencyResolver.artifactOf("a:b:c:d:e:f"));
        assertNull(MavenDependencyResolver.artifactOf("com.example::1.0.0"));
    }

    @Test
    void tellsTheCoordinatesOfADownloadedProjectByItsFolder() throws Exception {
        publishProject("com.example.rules", "domain", "1.0.0", "Domain");
        var project = resolver.projectOf(projectDependency("Domain", "com.example.rules:domain:1.0.0")).project();

        assertNotNull(project);
        assertEquals(new MavenCoordinates("com.example.rules", "domain", "1.0.0"),
                resolver.coordinatesOf(project.getProjectFolder()));
        assertNull(resolver.coordinatesOf(local.resolve("com/example/rules/domain/1.0.0")),
                "the version is no project");
        assertNull(resolver.coordinatesOf(temp.resolve("elsewhere/domain-1.0.0-1")));
        assertNull(resolver.coordinatesOf(local.resolve("com/example/rules/domain/1.0.0/domain-1.0.0-rules-1")),
                "an archive with a classifier is no project the coordinates open");
        assertNull(resolver.coordinatesOf(null));
    }

    @Test
    void refusesCoordinatesThatNameAFileOutsideTheRepository() {
        for (var coordinates : List.of("com.example:..:1.0.0", "com.example:lib:..", "com.example:lib:jar:.:1.0",
                "com/example:lib:1.0.0", "com.example:lib:1.0\\..\\..", ".opt.openl:lib:1.0.0", "..com:lib:1.0.0")) {
            assertNull(MavenDependencyResolver.artifactOf(coordinates), coordinates);
        }
        assertNotNull(MavenDependencyResolver.artifactOf("com.example:lib_x:jar:1.0.0-beta+2"));
    }

    @Test
    void leavesNothingOfAnArchiveItCannotUnpack() throws Exception {
        var archive = artifactPath("com.example", "domain", "1.0.0", null, "zip");
        var content = new byte[64 * 1024];
        new Random(1).nextBytes(content);
        writeZip(archive, Map.of("rules.xml", new String(content, StandardCharsets.ISO_8859_1)));
        // Cut in the middle of the entry
        Files.write(archive, Arrays.copyOf(Files.readAllBytes(archive), (int) Files.size(archive) / 2));

        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.project());
        assertTrue(downloaded.problem().getSummary().startsWith("Cannot read the project of the dependency 'Domain'"),
                downloaded.problem().getSummary());
        try (var files = Files.list(local.resolve("com/example/domain/1.0.0"))) {
            assertEquals(List.of(), files.filter(Files::isDirectory).toList(), "no unpacked or temporary folder");
        }
    }

    @Test
    void resolvesOnceForTheCallersAskingMeanwhileWithoutHoldingUpOthers() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var resolutions = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> resolver.cached("slow", () -> {
                started.countDown();
                await(release);
                return "answer " + resolutions.incrementAndGet();
            }));
            started.await();
            var second = executor.submit(() -> resolver.cached("slow",
                    () -> "answer " + resolutions.incrementAndGet()));

            assertEquals("other", resolver.cached("other", () -> "other"), "another key is not held up");
            release.countDown();

            assertEquals("answer 1", first.get());
            assertEquals("answer 1", second.get());
            assertEquals(1, resolutions.get());
        }
    }

    @Test
    void resolvesAgainAfterAResolutionFails() {
        assertThrows(IllegalStateException.class, () -> resolver.cached("key", () -> {
            throw new IllegalStateException("failed");
        }));

        assertEquals("found", resolver.cached("key", () -> "found"));
    }

    @Test
    void resolvesAgainAfterAResolutionBreaksWithAnError() {
        assertThrows(StackOverflowError.class, () -> resolver.cached("key", () -> {
            throw new StackOverflowError("deep");
        }));

        assertEquals("found", resolver.cached("key", () -> "found"), "no caller waits for the failed answer");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** The libraries of the project, with those of the pom.xml its folder holds. */
    private static MavenDependencyResolver.Libraries librariesOf(MavenDependencyResolver resolver,
                                                                 ProjectDescriptor project) throws IOException {
        var folder = project.getProjectFolder();
        return resolver.librariesOf(project, folder == null ? null : ProjectPoms.inFolder(folder, folder.getParent()));
    }

    private static ProjectDescriptor project(String name, ProjectDependencyDescriptor... dependencies) {
        var project = new ProjectDescriptor();
        project.setName(name);
        project.setDependencies(dependencies.length == 0 ? null : List.of(dependencies));
        return project;
    }

    private static ProjectDependencyDescriptor library(String coordinates) {
        return projectDependency(null, coordinates);
    }

    private static ProjectDependencyDescriptor projectDependency(String name, String coordinates) {
        var dependency = new ProjectDependencyDescriptor();
        dependency.setName(name);
        dependency.setMavenArtifact(coordinates);
        return dependency;
    }

    private static List<String> fileNames(List<Path> files) {
        return files.stream().map(file -> file.getFileName().toString()).sorted().toList();
    }

    private static OpenLMessage single(java.util.Collection<OpenLMessage> messages) {
        assertEquals(1, messages.size(), messages::toString);
        return messages.iterator().next();
    }

    /** Puts an artifact with its POM into the remote repository. */
    private void publish(String groupId, String artifactId, String version, String extension, String dependencies)
            throws IOException {
        writeZip(artifactPath(groupId, artifactId, version, null, extension), Map.of("META-INF/MANIFEST.MF", ""));
        Files.writeString(artifactPath(groupId, artifactId, version, null, "pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    %s
                </project>
                """.formatted(groupId, artifactId, version, dependencies == null ? "" : dependencies));
    }

    /** A project whose folder holds a {@code pom.xml} with the given content. */
    private ProjectDescriptor projectWithPom(String content, ProjectDependencyDescriptor... dependencies)
            throws IOException {
        var folder = Files.createDirectories(temp.resolve("workspace/Rules"));
        Files.writeString(folder.resolve("pom.xml"), pom(content));
        var rules = project("Rules", dependencies);
        rules.setProjectFolder(folder);
        return rules;
    }

    private static String pom(String content) {
        return """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>rules</artifactId>
                    <version>1.0.0</version>
                    <packaging>openl</packaging>
                    %s
                </project>
                """.formatted(content);
    }

    /** Puts a POM of the given content into the remote repository. */
    private void publishPom(String groupId, String artifactId, String version, String content) throws IOException {
        Files.writeString(artifactPath(groupId, artifactId, version, null, "pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    %s
                </project>
                """.formatted(groupId, artifactId, version, content));
    }

    /** Puts a parent POM into the remote repository. */
    private void publishParent(String groupId, String artifactId, String version) throws IOException {
        Files.writeString(artifactPath(groupId, artifactId, version, null, "pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    <packaging>pom</packaging>
                </project>
                """.formatted(groupId, artifactId, version));
    }

    /** Puts a jar whose POM inherits the given parent into the remote repository. */
    private void publishWithParent(String groupId, String artifactId, String version, String parent,
                                   String parentVersion) throws IOException {
        writeZip(artifactPath(groupId, artifactId, version, null, "jar"), Map.of("META-INF/MANIFEST.MF", ""));
        Files.writeString(artifactPath(groupId, artifactId, version, null, "pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>%s</groupId>
                        <artifactId>%s</artifactId>
                        <version>%s</version>
                    </parent>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                </project>
                """.formatted(groupId, parent, parentVersion, artifactId, version));
    }

    /** Puts an archive of an OpenL project into the remote repository, as the OpenL Maven Plugin packages it. */
    private void publishProject(String groupId, String artifactId, String version, String name) throws IOException {
        writeZip(artifactPath(groupId, artifactId, version, null, "zip"),
                Map.of("rules.xml", "<project><name>%s</name></project>".formatted(name)));
    }

    private Path artifactPath(String groupId, String artifactId, String version, String classifier, String extension)
            throws IOException {
        var folder = remote.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
        Files.createDirectories(folder);
        var suffix = classifier == null ? "" : "-" + classifier;
        return folder.resolve(artifactId + "-" + version + suffix + "." + extension);
    }

    private static void writeZip(Path file, Map<String, String> entries) throws IOException {
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes());
                zip.closeEntry();
            }
        }
    }

    /** A clock the test moves forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-19T12:00:00Z");

        void advance(java.time.Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
