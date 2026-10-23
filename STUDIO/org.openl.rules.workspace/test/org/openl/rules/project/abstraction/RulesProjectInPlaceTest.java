package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.InPlaceRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.LocalRepository;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.dtr.impl.MappedRepository;

/**
 * A project edited where a design repository on the file system keeps it: opened as it is, written straight into its
 * folder, and never copied, saved, closed or renamed.
 */
class RulesProjectInPlaceTest {

    private static final WorkspaceUserImpl USER = new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe"));

    @TempDir
    Path designRoot;

    @TempDir
    Path userDir;

    private Path folder;
    private MetainfoRegistry registry;
    private InPlaceRepository inPlace;
    private RulesProject project;

    @BeforeEach
    void init() throws Exception {
        folder = designRoot.resolve("bank-rating");
        write(folder.resolve("rules.xml"), "<project><name>Bank Rating</name></project>");
        write(folder.resolve("rules/Main.xlsx"), "as on disk");
        var files = new LocalRepository();
        files.setRoot(designRoot);
        files.setId("design");
        files.initialize();
        var design = MappedRepository.create(files, "DESIGN/rules/");
        var designData = design.listFolders("DESIGN/rules/").getFirst();

        registry = MetainfoRegistry.open(userDir);
        inPlace = new InPlaceRepository(designRoot, userDir, registry);
        inPlace.setId("design");
        inPlace.initialize();
        var localData = new FileData();
        localData.setName("bank-rating");
        localData.addAdditionalData(new FileMappingData(designData.getName(), "bank-rating"));
        project = new RulesProject(USER, inPlace, localData, design, designData, new DummyLockEngine());
    }

    @Test
    void isOpenedAsItIsOnDiskAndNamedByItsDescriptor() {
        assertTrue(project.isOpened());
        assertTrue(project.isInPlace());
        assertEquals("Bank Rating", project.getName(), "named by rules.xml, not by its folder");
        assertEquals("Bank Rating", project.getBusinessName());
        assertEquals("bank-rating", project.getFolderPath());
    }

    @Test
    void isNamedByItsFolderWithoutADesignRepository() {
        var localData = new FileData();
        localData.setName("bank-rating");
        var detached = new RulesProject(USER, inPlace, localData, null, null, new DummyLockEngine());

        assertTrue(detached.isInPlace());
        assertEquals("bank-rating", detached.getName());
        assertEquals("bank-rating", detached.getMainBusinessName());
    }

    @Test
    void writesStraightIntoItsFolderAndHasNothingToSave() throws Exception {
        var main = (AProjectResource) project.getArtefact("rules/Main.xlsx");

        main.setContent(new ByteArrayInputStream("edited".getBytes(StandardCharsets.UTF_8)));

        assertEquals("edited", Files.readString(folder.resolve("rules/Main.xlsx")));
        assertFalse(project.isModified(), "the file on disk is the current version");
        assertTrue(project.getLocalChanges().added().isEmpty());
        assertTrue(project.getLocalChanges().modified().isEmpty());
        assertTrue(registry.projects().isEmpty(), "nothing is recorded about the project in the workspace");
        assertFalse(Files.exists(userDir.resolve("bank-rating")), "the project is not copied into the workspace");
    }

    @Test
    void closingOpeningAndSavingLeaveTheFolderAsItIs() throws Exception {
        project.save(USER);
        project.openVersion(null);
        project.close(USER);

        assertEquals("as on disk", Files.readString(folder.resolve("rules/Main.xlsx")));
        assertTrue(Files.exists(folder.resolve("rules.xml")), "closing never deletes the folder of the project");
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
