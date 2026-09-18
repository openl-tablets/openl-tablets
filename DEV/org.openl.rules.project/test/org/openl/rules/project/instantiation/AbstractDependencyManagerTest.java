package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.ResolvedDependency;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

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
