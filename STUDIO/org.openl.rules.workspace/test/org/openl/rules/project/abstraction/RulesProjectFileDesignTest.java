package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

/**
 * What a project opened from a folder on the file system saves and reports as changed, when other programs write to
 * that folder while the project is open.
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
    void savesOnlyTheFilesChangedInTheWorkspace() throws Exception {
        editInTheWorkspace();
        var other = designRoot.resolve(PROJECT).resolve("rules/Other.xlsx");
        var otherWrittenAt = Files.getLastModifiedTime(other);
        changeOnDisk();

        project.save(USER);

        var folder = designRoot.resolve(PROJECT);
        assertEquals("edited in OpenL Studio", Files.readString(folder.resolve("rules/Main.xlsx")));
        assertEquals("added in OpenL Studio", Files.readString(folder.resolve("rules/New.xlsx")));
        assertFalse(Files.exists(folder.resolve("Removed.txt")), "a file deleted in the workspace is deleted");
        assertEquals("changed on disk", Files.readString(folder.resolve("pom.xml")), "a change on disk stays");
        assertTrue(Files.exists(folder.resolve("groovy/ExternalNew.groovy")), "a file added on disk stays");
        assertFalse(Files.exists(folder.resolve("i18n/zh.properties")), "a file deleted on disk stays deleted");
        assertEquals(otherWrittenAt, Files.getLastModifiedTime(other), "a file nobody changed is not written");
        assertFalse(project.isModified(), "the saved project has nothing more to save");
    }

    @Test
    void savesAFileDeletedBothInTheWorkspaceAndOnDisk() throws Exception {
        localRepository.delete(fileData("Removed.txt"));
        Files.delete(designRoot.resolve(PROJECT).resolve("Removed.txt"));

        project.save(USER);

        assertFalse(Files.exists(designRoot.resolve(PROJECT).resolve("Removed.txt")));
        assertFalse(project.isModified(), "the saved project has nothing more to save");
    }

    @Test
    void refusesASaveThatWouldOverwriteAChangeMadeOnDisk() throws Exception {
        localRepository.save(fileData("pom.xml"), IOUtils.toInputStream("edited in OpenL Studio"));
        localRepository.save(fileData("rules/New.xlsx"), IOUtils.toInputStream("added in OpenL Studio"));
        localRepository.save(fileData("rules/Main.xlsx"), IOUtils.toInputStream("edited in OpenL Studio"));
        var folder = designRoot.resolve(PROJECT);
        write(folder.resolve("pom.xml"), "changed on disk");
        write(folder.resolve("rules/New.xlsx"), "added on disk");

        var refused = assertThrows(ChangedOutsideException.class, () -> project.save(USER));

        assertEquals(List.of("/pom.xml", "/rules/New.xlsx"), refused.getPaths());
        assertEquals("changed on disk", Files.readString(folder.resolve("pom.xml")));
        assertEquals("as opened: rules/Main.xlsx", Files.readString(folder.resolve("rules/Main.xlsx")),
                "nothing is saved");
        assertTrue(project.isModified());
    }

    @Test
    void keepsTellingAChangeMadeOnDiskBeforeAnEarlierSave() throws Exception {
        changeOnDisk();
        localRepository.save(fileData("rules/Main.xlsx"), IOUtils.toInputStream("edited in OpenL Studio"));
        project.save(USER);

        // The copy of pom.xml in the workspace is still the one opened, not the one changed on disk.
        localRepository.save(fileData("pom.xml"), IOUtils.toInputStream("edited in OpenL Studio"));

        var refused = assertThrows(ChangedOutsideException.class, () -> project.save(USER));
        assertEquals(List.of("/pom.xml"), refused.getPaths());
    }

    @Test
    void writesTheWholeProjectAgainWhenItsFolderIsGone() throws Exception {
        FileUtils.deleteQuietly(designRoot.resolve(PROJECT).toFile());
        localRepository.save(fileData("rules/Main.xlsx"), IOUtils.toInputStream("edited in OpenL Studio"));

        project.save(USER);

        var folder = designRoot.resolve(PROJECT);
        assertEquals("edited in OpenL Studio", Files.readString(folder.resolve("rules/Main.xlsx")));
        assertEquals("as opened: rules/Other.xlsx", Files.readString(folder.resolve("rules/Other.xlsx")),
                "a project is not left without the files nobody changed");
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
