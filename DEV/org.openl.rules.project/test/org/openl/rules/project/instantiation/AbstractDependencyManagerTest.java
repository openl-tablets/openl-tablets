package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.ResolvedDependency;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;

class AbstractDependencyManagerTest {

    private static final Path DESCRIPTOR_ZIP = Path.of("test-resources/descriptor.zip");

    @Test
    void resetAllDeletesExtractedClasspathTempJars() throws Exception {
        try (FileSystem fs = FileSystems.newFileSystem(DESCRIPTOR_ZIP,
                Thread.currentThread().getContextClassLoader())) {
            var project = ProjectDescriptor.read(fs.getPath("/rules-clspth.xml")).expand();

            var dependencyManager = new AbstractDependencyManager(
                    Thread.currentThread().getContextClassLoader(), true, null) {
                @Override
                protected Set<IDependencyLoader> initDependencyLoaders() {
                    return Set.of();
                }
            };

            // Building the external-jars class loader extracts the nested lib jars to temp files.
            dependencyManager.getExternalJarsClassLoader(project);

            var tempJars = new ArrayList<Path>();
            for (URL url : project.getClassPathUrls()) {
                Path path = toFilePath(url);
                if (path != null && path.getFileName().toString().startsWith("tmp-")) {
                    tempJars.add(path);
                }
            }
            assertFalse(tempJars.isEmpty(), "expected nested jars to be extracted to temp files");
            tempJars.forEach(p -> assertTrue(Files.exists(p), "temp jar must exist before resetAll: " + p));

            dependencyManager.resetAll();

            tempJars.forEach(p -> assertFalse(Files.exists(p), "temp jar must be deleted after resetAll: " + p));
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Rates*      | true  | Rates Main  | true",
            "Rates*      | true  | Policies    | false",
            "Rate?       | true  | Rates       | true",
            "Rate?       | true  | Rate        | false",
            "Rates/Main  | true  | Rates / Main | true",
            "'Rates   /   Main' | true | Rates/Main | true",
            "'Rate s / Main' | true | Rates/Main | false",
            "Rates*      | false | Rates Main  | false",
            "Rates*      | false | Rates*      | true",
            "Rates.Main  | true  | RatesXMain  | false",
            "'a\\E.+'    | true  | abc         | false",
            "'a\\E.+'    | false | abc         | false",
            "'a\\E.+'    | false | 'a\\E.+'    | true"
    })
    void dependencyPatternTreatsOnlyWildcardsAsSpecial(String identifier,
                                                       boolean withWildcardSupport,
                                                       String name,
                                                       boolean expected) {
        var pattern = AbstractDependencyManager.dependencyPattern(identifier, withWildcardSupport);

        assertEquals(expected, pattern.matcher(name).matches());
    }

    @Test
    void theProjectReadsItsLibrariesAfterItsClasspathAndReportsWhatWentWrongWithItsDependencies() throws Exception {
        var project = ProjectResolver.getInstance().resolve(Path.of("test-resources/dependencies/test1/module"));
        var library = DESCRIPTOR_ZIP.toUri().toURL();
        var problem = new OpenLMessage("Cannot download 'com.example:lib:jar:1.0.0'.", Severity.ERROR);
        var dependencyManager = new SimpleDependencyManager(List.of(project),
                Thread.currentThread().getContextClassLoader(),
                false,
                Map.of()) {
            @Override
            protected List<URL> getLibraries(ProjectDescriptor p) {
                return List.of(library);
            }

            @Override
            public Collection<OpenLMessage> getDependencyMessages(ProjectDescriptor p) {
                return List.of(problem);
            }
        };

        var expectedUrls = new ArrayList<>(List.of(project.getClassPathUrls()));
        expectedUrls.add(library);
        assertEquals(expectedUrls, List.of(dependencyManager.getExternalJarsClassLoader(project).getURLs()));

        for (var dependency : List.of(AbstractDependencyManager.buildResolvedDependency(project),
                AbstractDependencyManager.buildResolvedDependency(project.getModules().getFirst()))) {
            var compiled = dependencyManager.loadDependency(dependency).getCompiledOpenClass();
            assertTrue(compiled.hasErrors(), dependency + " fails with the problem of its dependencies");
            assertTrue(compiled.getMessages().contains(problem), dependency + " reports the problem");
            assertEquals(1, compiled.getAllMessages().stream().filter(problem::equals).count(),
                    dependency + " reports the problem once");
        }
    }

    @Test
    void everyProjectReadsTheProvidedLibrariesThroughOneClassLoaderBehindTheRootOne() throws Exception {
        var module = ProjectResolver.getInstance().resolve(Path.of("test-resources/dependencies/test1/module"));
        var other = new ProjectDescriptor();
        other.setName("Other");
        var plain = new ProjectDescriptor();
        plain.setName("Plain");
        var provided = DESCRIPTOR_ZIP.toUri().toURL();
        var root = Thread.currentThread().getContextClassLoader();
        var dependencyManager = new SimpleDependencyManager(List.of(module, other, plain), root, false, Map.of()) {
            @Override
            protected List<URL> getProvidedLibraries(ProjectDescriptor p) {
                return p == plain ? List.of() : List.of(provided);
            }
        };

        var parent = dependencyManager.getExternalJarsClassLoader(module).getParent();

        assertSame(root, parent.getParent(), "the root class loader is asked first");
        assertEquals(List.of(provided), List.of(((URLClassLoader) parent).getURLs()));
        assertNotNull(parent.getResource("rules-clspth.xml"), "a provided library is read");
        assertSame(parent, dependencyManager.getExternalJarsClassLoader(other).getParent(), "one for every project");
        assertSame(parent, dependencyManager.getExternalJarsClassLoader(plain).getParent());
        assertFalse(List.of(dependencyManager.getExternalJarsClassLoader(module).getURLs()).contains(provided),
                "the project does not read it itself");

        dependencyManager.resetAll();

        assertNotSame(parent,
                dependencyManager.getExternalJarsClassLoader(module).getParent(),
                "a new one after a reset");
    }

    /** A project without provided libraries built first still reads those of the others, as the order is no matter. */
    @Test
    void everyProjectHasTheProvidedClassLoaderAsParentWhateverTheOrder() throws Exception {
        var module = ProjectResolver.getInstance().resolve(Path.of("test-resources/dependencies/test1/module"));
        var plain = new ProjectDescriptor();
        plain.setName("Plain");
        var provided = DESCRIPTOR_ZIP.toUri().toURL();
        var root = Thread.currentThread().getContextClassLoader();
        var dependencyManager = new SimpleDependencyManager(List.of(module, plain), root, false, Map.of()) {
            @Override
            protected List<URL> getProvidedLibraries(ProjectDescriptor p) {
                return p == plain ? List.of() : List.of(provided);
            }
        };

        var parent = dependencyManager.getExternalJarsClassLoader(plain).getParent();

        assertNotSame(root, parent, "the provided libraries of the other project are read too");
        assertEquals(List.of(provided), List.of(((URLClassLoader) parent).getURLs()));
        assertSame(parent, dependencyManager.getExternalJarsClassLoader(module).getParent());
    }

    @Test
    void aProjectReadsNoProvidedLibraryByDefault() {
        var dependencyManager = managerOf();
        var project = new ProjectDescriptor();

        assertEquals(List.of(), dependencyManager.getProvidedLibraries(project));
        assertSame(Thread.currentThread().getContextClassLoader(),
                dependencyManager.getExternalJarsClassLoader(project).getParent());
    }

    @Test
    void aProjectHasNoLibrariesAndNoDependencyMessagesByDefault() {
        var dependencyManager = managerOf();
        var project = new ProjectDescriptor();

        assertEquals(List.of(), dependencyManager.getLibraries(project));
        assertEquals(List.of(), dependencyManager.getDependencyMessages(project));
    }

    /**
     * A module the manager does not load is asked to be reset when the session opens a module it wrote to
     * after having compiled another project in between: there is nothing to drop, and the manager must say
     * so quietly rather than fail the opening.
     */
    @Test
    void resetOfADependencyTheManagerDoesNotLoadIsNothingToDo() {
        var loaded = new StubLoader(AbstractDependencyManager.buildResolvedDependency("Rates", "Main"));
        var dependencyManager = managerOf(loaded);
        var unknown = AbstractDependencyManager.buildResolvedDependency("Policies", "Main");

        assertDoesNotThrow(() -> dependencyManager.reset(unknown));

        assertEquals(0, loaded.resets, "what the manager loads is left as it is");
    }

    @Test
    void resetOthersKeepsOnlyWhatTheManagerLoadsAmongTheDependenciesNamed() {
        var kept = new StubLoader(AbstractDependencyManager.buildResolvedDependency("Rates", "Main"));
        var dropped = new StubLoader(AbstractDependencyManager.buildResolvedDependency("Rates", "Tests"));
        var dependencyManager = managerOf(kept, dropped);
        var unknown = AbstractDependencyManager.buildResolvedDependency("Policies", "Main");

        assertDoesNotThrow(() -> dependencyManager.resetOthers(kept.getDependency(), unknown));

        assertEquals(0, kept.resets);
        assertEquals(1, dropped.resets);
    }

    private static AbstractDependencyManager managerOf(IDependencyLoader... loaders) {
        return new AbstractDependencyManager(Thread.currentThread().getContextClassLoader(), true, null) {
            @Override
            protected Set<IDependencyLoader> initDependencyLoaders() {
                return Set.of(loaders);
            }
        };
    }

    /** A loader that only counts how often it is reset. */
    private static final class StubLoader implements IDependencyLoader {

        private final ResolvedDependency dependency;
        private int resets;

        private StubLoader(ResolvedDependency dependency) {
            this.dependency = dependency;
        }

        @Override
        public CompiledDependency getCompiledDependency() {
            return null;
        }

        @Override
        public CompiledDependency getRefToCompiledDependency() {
            return null;
        }

        @Override
        public ResolvedDependency getDependency() {
            return dependency;
        }

        @Override
        public ProjectDescriptor getProject() {
            return null;
        }

        @Override
        public Module getModule() {
            return null;
        }

        @Override
        public boolean isProjectLoader() {
            return false;
        }

        @Override
        public void reset() {
            resets++;
        }
    }

    private static Path toFilePath(URL url) {
        try {
            return "file".equals(url.getProtocol()) ? Path.of(url.toURI()) : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
