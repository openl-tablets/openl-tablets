package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.util.IOUtils;

/**
 * What a project opened from a folder on the file system reports as changed, when other programs write to that folder
 * while the project is open.
 *
 * @author Yury Molchan
 */
class RulesProjectFileDesignTest {

    private static final String PROJECT = "Example 1";
    private static final WorkspaceUserImpl USER = new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe"));

    @TempDir
    Path designRoot;

    @TempDir
    Path userDir;

    private org.openl.rules.repository.file.LocalRepository designRepository;
    private LocalRepository localRepository;
    private RulesProject project;

    @BeforeEach
    void init() throws Exception {
        designRepository = new org.openl.rules.repository.file.LocalRepository();
        designRepository.setRoot(designRoot);
        designRepository.setId("design");
        designRepository.initialize();
        for (var file : List.of("rules/Main.xlsx", "rules/Other.xlsx", "pom.xml", "i18n/zh.properties", "Removed.txt")) {
            write(designRoot.resolve(PROJECT).resolve(file), "as opened: " + file);
        }

        localRepository = new LocalRepository(userDir, MetainfoRegistry.open(userDir));
        localRepository.setId("design");
        localRepository.initialize();
        project = new RulesProject(USER,
                localRepository,
                null,
                designRepository,
                designRepository.check(PROJECT),
                new DummyLockEngine());
        project.open();
    }

    @Test
    void listsOnlyTheChangesMadeInTheWorkspace() throws Exception {
        editInTheWorkspace();
        changeOnDisk();

        var changes = project.getLocalChanges();

        assertEquals(List.of("/rules/New.xlsx"), changes.added());
        assertEquals(List.of("/rules/Main.xlsx"), changes.modified());
        assertEquals(List.of("/Removed.txt"), changes.deleted());
    }

    @Test
    void listsNoChangeOfAProjectJustOpened() throws Exception {
        changeOnDisk();

        assertTrue(project.getLocalChanges().isEmpty());
    }

    @Test
    void listsNoChangeOfAProjectNotOpened() throws Exception {
        var notOpened = new RulesProject(USER,
                localRepository,
                null,
                designRepository,
                designRepository.check(PROJECT),
                new DummyLockEngine());

        assertTrue(notOpened.getLocalChanges().isEmpty());
    }

    private void editInTheWorkspace() throws IOException {
        localRepository.save(fileData("rules/Main.xlsx"), IOUtils.toInputStream("edited in OpenL Studio"));
        localRepository.save(fileData("rules/New.xlsx"), IOUtils.toInputStream("added in OpenL Studio"));
        localRepository.delete(fileData("Removed.txt"));
    }

    /** What an IDE or a build writes into the project folder while the project is open in OpenL Studio. */
    private void changeOnDisk() throws IOException {
        var folder = designRoot.resolve(PROJECT);
        write(folder.resolve("pom.xml"), "changed on disk");
        write(folder.resolve("groovy/ExternalNew.groovy"), "added on disk");
        Files.delete(folder.resolve("i18n/zh.properties"));
    }

    private static FileData fileData(String file) {
        var data = new FileData();
        data.setName(PROJECT + "/" + file);
        return data;
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
