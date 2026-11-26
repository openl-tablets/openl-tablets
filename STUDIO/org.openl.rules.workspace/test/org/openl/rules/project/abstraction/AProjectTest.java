package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;

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

    /**
     * A project opened at an older revision stays at its latest version while that revision holds the project as the
     * latest one does, as an older head of a Git branch that only other projects changed since.
     */
    @Test
    void anOlderRevisionThatHoldsTheProjectAlikeIsTheLatestVersion() throws Exception {
        var repository = versioned(folder("head", "tree-1"), folder("opened", "tree-1"));

        assertTrue(new AProject(repository, "Project", "opened").isLastVersion());
    }

    /** The answer is kept for the two revisions it compared: the repository is asked once. */
    @Test
    void theAnswerIsKeptForTheRevisionsItCompared() throws Exception {
        var repository = versioned(folder("head", "tree-1"), folder("opened", "tree-1"));
        var project = new AProject(repository, "Project", "opened");

        assertTrue(project.isLastVersion());
        assertTrue(project.isLastVersion());

        verify(repository, times(1)).checkHistory("Project", "opened");
    }

    /** A commit that changes the project makes the revision it was opened at an older one. */
    @Test
    void anOlderRevisionThatHoldsTheProjectOtherwiseIsNotTheLatestVersion() throws Exception {
        var repository = versioned(folder("head", "tree-2"), folder("opened", "tree-1"));

        assertFalse(new AProject(repository, "Project", "opened").isLastVersion());
    }

    /** A repository that gives a folder no unique id tells the latest version by the revision alone. */
    @Test
    void aFolderWithoutAUniqueIdIsTheLatestVersionAtTheLatestRevisionOnly() throws Exception {
        var repository = versioned(folder("head", null), folder("opened", null));

        assertFalse(new AProject(repository, "Project", "opened").isLastVersion());
        assertTrue(new AProject(repository, "Project", "head").isLastVersion());
    }

    /** A versioned repository whose project folder is at the given revisions: the latest, and the one opened. */
    private static Repository versioned(FileData latest, FileData opened) throws IOException {
        var repository = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setVersions(true).build());
        when(repository.check("Project")).thenReturn(latest);
        when(repository.checkHistory("Project", opened.getVersion())).thenReturn(opened);
        when(repository.checkHistory("Project", latest.getVersion())).thenReturn(latest);
        return repository;
    }

    private static FileData folder(String version, String uniqueId) {
        var data = new FileData();
        data.setName("Project");
        data.setVersion(version);
        data.setUniqueId(uniqueId);
        return data;
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
