package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.FileData;

/**
 * The files of a project in one tree of folders, as a project read once and written to since keeps them: the files it
 * read under their paths, and the folders added since with the files written into them.
 */
class AProjectFolderTest {

    @TempDir
    private Path root;
    private LocalRepository repository;
    private AProject project;

    @BeforeEach
    void readTheProject() throws IOException {
        repository = new LocalRepository(root, MetainfoRegistry.open(root));
        repository.initialize();
        save("Project/rules/Main.xlsx");
        save("Project/rules/Model/Driver.md");
        project = new AProject(repository, "Project");
        project.getArtefacts();
    }

    @Test
    void aFileWrittenIntoANewFolderStandsBesideTheFilesRead() throws ProjectException {
        folder(project, "rules").addFolder("Rating").addResource("Environment.md", content());

        var tree = AProjectFolder.treeOf(project);

        var rules = (AProjectFolder) tree.getArtefact("rules");
        assertEquals(List.of("Main.xlsx", "Model", "Rating"), namesIn(rules));
        assertEquals(List.of("Driver.md"), namesIn((AProjectFolder) rules.getArtefact("Model")));
        assertEquals(List.of("Environment.md"), namesIn((AProjectFolder) rules.getArtefact("Rating")));
    }

    @Test
    void aNewFolderWithoutFilesIsInTheTree() throws ProjectException {
        folder(project, "rules").addFolder("Empty");

        var rules = (AProjectFolder) AProjectFolder.treeOf(project).getArtefact("rules");

        assertEquals(List.of("Empty", "Main.xlsx", "Model"), namesIn(rules));
        assertEquals(List.of(), namesIn((AProjectFolder) rules.getArtefact("Empty")));
    }

    @Test
    void theFoldersOfTheProjectAreNotChanged() throws ProjectException {
        var rules = folder(project, "rules");
        rules.addFolder("Rating").addResource("Environment.md", content());
        var before = namesIn(rules);

        AProjectFolder.treeOf(project);

        assertEquals(before, namesIn(rules));
    }

    /** The folder of a project the files of the project write into, added when the project holds none of the name. */
    private static AProjectFolder folder(AProjectFolder parent, String name) throws ProjectException {
        return parent.hasArtefact(name) ? (AProjectFolder) parent.getArtefact(name) : parent.addFolder(name);
    }

    private static List<String> namesIn(AProjectFolder folder) {
        var names = new TreeSet<String>();
        folder.getArtefacts().forEach(artefact -> names.add(artefact.getName()));
        return List.copyOf(names);
    }

    private void save(String name) throws IOException {
        var data = new FileData();
        data.setName(name);
        repository.save(data, content());
    }

    private static ByteArrayInputStream content() {
        return new ByteArrayInputStream("text".getBytes(StandardCharsets.UTF_8));
    }
}
