package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;

class AProjectTest {

    @TempDir
    Path sourceRoot;

    @TempDir
    Path targetRoot;

    @Test
    void requiresTheFileDataItIsReadFrom() {
        assertThrows(NullPointerException.class, () -> new AProject(null, (FileData) null));
    }

    /**
     * A file of a user workspace that was never saved has no file id, as an edit not saved yet. A copy writes it as
     * the transformer of the target makes it too, so the descriptor of the copy gets its own name (EPBDS-16232).
     */
    @Test
    void transformsTheFilesTheSourceChangedWhenItCopiesThem() throws Exception {
        var source = workspace(sourceRoot);
        save(source, "Source/rules.xml", "<project><name>Renamed, not saved</name></project>");
        save(source, "Source/rules/Main.xlsx", "workbook");
        var target = workspace(targetRoot);
        var copyData = new FileData();
        copyData.setName("Copy");
        var copy = new AProject(target, copyData);
        copy.setResourceTransformer(new RenamingTransformer("Copy"));

        copy.update(new AProject(source, "Source"), null);

        assertEquals("<project><name>Copy</name></project>", Files.readString(targetRoot.resolve("Copy/rules.xml")));
        assertEquals("workbook", Files.readString(targetRoot.resolve("Copy/rules/Main.xlsx")));
    }

    /** A user workspace: a folder repository with file ids, which has none for a file it never saved. */
    private static LocalRepository workspace(Path root) {
        var repository = new LocalRepository(root, MetainfoRegistry.open(root));
        repository.initialize();
        return repository;
    }

    private static void save(LocalRepository repository, String name, String content) throws IOException {
        var data = new FileData();
        data.setName(name);
        repository.save(data, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    /** Writes the given name into the descriptor and every other file as it is. */
    private record RenamingTransformer(String name) implements ResourceTransformer {

        @Override
        public InputStream transform(AProjectResource resource) throws ProjectException {
            if ("rules.xml".equals(resource.getInternalPath())) {
                var descriptor = "<project><name>" + name + "</name></project>";
                return new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8));
            }
            return resource.getContent();
        }

        @Override
        public List<FileItem> transformChangedFiles(String rootPath, List<FileItem> changes) {
            return changes;
        }
    }
}
