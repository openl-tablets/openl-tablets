package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.workspace.WorkspaceUserImpl;

/**
 * Whether a project is modified once a file of it is written past the workspace, as a restore of an earlier version of
 * a workbook writes it.
 */
class RulesProjectSettleLocalChangesTest {

    private static final String PROJECT = "Example 1";
    private static final String MAIN = "rules/Main.xlsx";
    private static final String OPENED = "as opened";
    private static final WorkspaceUserImpl USER = new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe"));

    @TempDir
    Path designRoot;

    @TempDir
    Path userDir;

    private VersionedRepository designRepository;
    private LocalRepository localRepository;

    @BeforeEach
    void init() throws IOException {
        designRepository = new VersionedRepository();
        designRepository.setRoot(designRoot);
        designRepository.setId("design");
        designRepository.initialize();
        write(designRoot.resolve(PROJECT).resolve(MAIN), OPENED);
        write(designRoot.resolve(PROJECT).resolve("rules.xml"), "<project/>");
        designRepository.firstRevision.put(PROJECT + "/" + MAIN, OPENED);
        designRepository.firstRevision.put(PROJECT + "/rules.xml", "<project/>");

        localRepository = new LocalRepository(userDir, MetainfoRegistry.open(userDir));
        localRepository.setId("design");
        localRepository.initialize();
    }

    @Test
    void aWorkbookWrittenBackAsTheRevisionOpenedLeavesNoChange() throws Exception {
        var project = openProject();
        localRepository.save(fileData(MAIN), stream("edited in OpenL Studio"));
        // A later revision of the design project holds another workbook: the one opened is compared.
        designRepository.head = "rev-2";
        write(designRoot.resolve(PROJECT).resolve(MAIN), "committed later");
        writeInWorkspace(MAIN, OPENED);

        project.settleLocalChanges();

        assertFalse(project.isModified());
        assertTrue(project.getLocalChanges().isEmpty());
        assertFalse(MetainfoRegistry.open(userDir).isDirty(PROJECT), "no change is found after a restart either");
    }

    @Test
    void aWorkbookOfTheSameSizeButAnotherContentStaysChanged() throws Exception {
        var project = openProject();
        localRepository.save(fileData(MAIN), stream("edited in OpenL Studio"));
        writeInWorkspace(MAIN, OPENED.toUpperCase());

        project.settleLocalChanges();

        assertTrue(project.isModified());
        assertEquals(List.of("/" + MAIN), project.getLocalChanges().modified());
    }

    @Test
    void aWorkbookWrittenPastTheWorkspaceMakesTheProjectModified() throws Exception {
        var project = openProject();
        writeInWorkspace(MAIN, "an earlier version");
        assertFalse(project.isModified(), "nothing told the project of the write");

        project.settleLocalChanges();

        assertTrue(project.isModified());
    }

    @Test
    void aWorkbookWrittenBackIsNoChangeThoughAnotherFileIsAdded() throws Exception {
        var project = openProject();
        localRepository.save(fileData("rules/New.xlsx"), stream("added in OpenL Studio"));
        writeInWorkspace(MAIN, OPENED);

        project.settleLocalChanges();

        assertTrue(project.isModified());
        var changes = project.getLocalChanges();
        assertEquals(List.of("/rules/New.xlsx"), changes.added());
        assertEquals(List.of(), changes.modified());
    }

    @Test
    void aWorkbookWhoseRevisionCannotBeReadStaysChanged() throws Exception {
        var project = openProject();
        writeInWorkspace(MAIN, OPENED);
        designRepository.unreadable = true;

        project.settleLocalChanges();

        assertTrue(project.isModified());
        assertEquals(List.of("/" + MAIN), project.getLocalChanges().modified());
    }

    @Test
    void aWorkbookTheRevisionDoesNotHoldStaysChanged() throws Exception {
        var project = openProject();
        designRepository.firstRevision.clear();
        designRepository.head = "rev-2";
        writeInWorkspace(MAIN, OPENED);

        project.settleLocalChanges();

        assertTrue(project.isModified());
    }

    @Test
    void aDesignRepositoryWithoutVersionsIsComparedWithItsFiles() throws Exception {
        designRepository.versions = false;
        var project = openProject();
        localRepository.save(fileData(MAIN), stream("edited in OpenL Studio"));
        writeInWorkspace(MAIN, OPENED);

        project.settleLocalChanges();

        assertFalse(project.isModified());
    }

    @Test
    void aProjectNotOpenedIsLeftAsItIs() throws Exception {
        var project = createProject();

        project.settleLocalChanges();

        assertFalse(project.isModified());
    }

    private RulesProject openProject() throws Exception {
        var project = createProject();
        project.open();
        assertFalse(project.isModified());
        return project;
    }

    private RulesProject createProject() throws IOException {
        return new RulesProject(USER,
                localRepository,
                null,
                designRepository,
                designRepository.check(PROJECT),
                new DummyLockEngine());
    }

    /** Writes a file of the project straight into the workspace, later than it was opened, as a restore writes it. */
    private void writeInWorkspace(String file, String content) throws IOException {
        var path = userDir.resolve(PROJECT).resolve(file);
        Files.writeString(path, content);
        Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis() + 2000));
    }

    private static FileData fileData(String file) {
        var data = new FileData();
        data.setName(PROJECT + "/" + file);
        return data;
    }

    private static ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    /**
     * A design repository whose files are at the revision {@code rev-1} until a later one is made, and which keeps
     * what {@code rev-1} held.
     */
    private static final class VersionedRepository extends FileSystemRepository {

        private final Map<String, String> firstRevision = new HashMap<>();
        private String head = "rev-1";
        private boolean versions = true;
        private boolean unreadable;

        @Override
        protected String getVersion(Path file) {
            return head;
        }

        @Override
        protected String getVersion(String path) {
            return head;
        }

        @Override
        public Features supports() {
            return new FeaturesBuilder(this).setVersions(versions).setFolders(true).setComments(false).build();
        }

        @Override
        public @Nullable FileItem readHistory(String name, String version) throws IOException {
            if (unreadable) {
                throw new IOException("The repository is unavailable.");
            }
            var content = firstRevision.get(name);
            if (!"rev-1".equals(version) || content == null) {
                return super.readHistory(name, version);
            }
            var data = new FileData();
            data.setName(name);
            data.setVersion(version);
            return new FileItem(data, stream(content));
        }
    }
}
