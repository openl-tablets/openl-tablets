package org.openl.rules.project.instantiation;

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

    private static Path toFilePath(URL url) {
        try {
            return "file".equals(url.getProtocol()) ? Path.of(url.toURI()) : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
