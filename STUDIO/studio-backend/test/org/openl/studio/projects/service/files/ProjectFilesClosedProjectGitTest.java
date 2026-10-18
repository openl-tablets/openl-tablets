package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.GitRepositoryFactory;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.MappedRepository;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.service.tables.TableFormatting;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.UploadFormatting;
import org.openl.studio.projects.validator.ProjectStateValidatorImpl;
import org.openl.util.IOUtils;

/**
 * Single-file writes into a closed project of a Git design repository.
 *
 * <p>A closed project is committed straight to the design repository. Every commit a write makes is the
 * current user's and carries a message naming the write, whoever wrote the file before. The project stays
 * closed, and nobody holds it afterwards.
 *
 * @author Yury Molchan
 */
class ProjectFilesClosedProjectGitTest {

    private static final String PROJECT = "Policy-Auto-Rating";
    private static final String CURRENT_USER = "Administrator <admin@example.com>: ";
    private static final String DESCRIPTOR = """
            <project>
                <name>Policy-Auto-Rating</name>
                <modules>
                    <module>
                        <name>Rules</name>
                        <rules-root path="Rules.xlsx"/>
                    </module>
                </modules>
            </project>
            """;

    @TempDir
    File remoteRoot;
    @TempDir
    File localRepositoriesFolder;
    @TempDir
    Path userDir;
    @TempDir
    File workspacesRoot;

    private Repository design;
    private LocalRepository localRepository;
    private LockEngine lockEngine;
    private AclProjectsHelper acl;
    private DesignTimeRepository designTimeRepository;
    private RulesProject project;
    private ProjectFileRoot root;
    private ProjectFilesServiceImpl service;
    private String seedCommit;

    @BeforeEach
    void setUp() throws Exception {
        try (var git = Git.init().setDirectory(remoteRoot).call()) {
            seed("rules.xml", DESCRIPTOR);
            seed("data.txt", "Previous content");
            seed("Rules.xlsx", "A workbook the test never opens");
            seed("docs/guide/intro.txt", "Previous introduction");
            git.add().addFilepattern(".").call();
            seedCommit = git.commit()
                    .setMessage("Add the project")
                    .setCommitter("Previous Author", "previous@example.com")
                    .call()
                    .getName();
        }
        design = new GitRepositoryFactory().create(key -> switch (key) {
            case "id" -> "design";
            case "uri" -> remoteRoot.toURI().toString();
            case "local-repositories-folder" -> localRepositoriesFolder.getAbsolutePath();
            default -> null;
        });
        localRepository = new LocalRepository(userDir, MetainfoRegistry.open(userDir));
        localRepository.setId("design");
        localRepository.initialize();
        lockEngine = LockEngineImpl.create(workspacesRoot, "rules");

        acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProject.class), any())).thenReturn(true);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.refreshBranch(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        project = closedProject(design, design.check(PROJECT));
        root = rootOf(project);
        service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), new ProjectDescriptorCleaner(acl), new BeanValidationProvider(List.of()), new UploadFormatting(mock(TableFormatting.class)));
    }

    @AfterEach
    void tearDown() {
        IOUtils.closeQuietly(design);
    }

    /** The project as the admin sees it while it is closed: read straight from the design repository. */
    private RulesProject closedProject(Repository repository, FileData folder) {
        return new RulesProject(new WorkspaceUserImpl("admin", UserInfo::new), localRepository, null, repository,
                folder, lockEngine);
    }

    private ProjectFileRoot rootOf(RulesProject closedProject) {
        return new ProjectFileRoot(closedProject, acl,
                new ProjectStateValidatorImpl(mock(ProtectedBranchBypassService.class)),
                mock(ProjectFileLookupService.class),
                () -> new UserInfo("admin", "admin@example.com", "Administrator"), designTimeRepository);
    }

    @Test
    void createdFileIsCommittedByTheCurrentUser() throws Exception {
        service.createResource(root, "notes/readme.txt", IOUtils.toInputStream("Hello"), true);

        assertEquals(List.of(CURRENT_USER + "Save readme.txt"), newCommits());
        assertEquals("Hello", read("notes/readme.txt"));
        // The mount that wrote the file reads it back.
        try (var written = service.getResource(root, "notes/readme.txt", null).getContent()) {
            assertEquals("Hello", new String(written.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void overwrittenFileIsCommittedByTheCurrentUserNotByItsPreviousAuthor() throws Exception {
        service.updateResource(root, "data.txt", IOUtils.toInputStream("New content"));

        assertEquals(List.of(CURRENT_USER + "Save data.txt"), newCommits());
        assertEquals("New content", read("data.txt"));
        // The overwritten revision stays in the history of the file.
        try (var previous = service.getResource(root, "data.txt", seedCommit).getContent()) {
            assertEquals("Previous content", new String(previous.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void writeCreatesAFileThatIsNotThereWhateverTheConflictPolicy() throws Exception {
        var outcome = service.writeResource(root, "notes/readme.txt", IOUtils.toInputStream("Hello"), true,
                ConflictPolicy.FAIL);

        assertEquals(FileWriteOutcome.CREATED, outcome);
        assertEquals(List.of(CURRENT_USER + "Save readme.txt"), newCommits());
        assertEquals("Hello", read("notes/readme.txt"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void writeReplacesAFileThatIsThereForOverwriteAndReplace() throws Exception {
        var overwritten = service.writeResource(root, "docs/guide/intro.txt",
                IOUtils.toInputStream("New introduction"), false, ConflictPolicy.OVERWRITE);
        var replaced = service.writeResource(root, "data.txt", IOUtils.toInputStream("New content"), false,
                ConflictPolicy.REPLACE);

        assertEquals(FileWriteOutcome.REPLACED, overwritten);
        assertEquals(FileWriteOutcome.REPLACED, replaced);
        assertEquals(List.of(CURRENT_USER + "Save data.txt", CURRENT_USER + "Save intro.txt"), newCommits());
        assertEquals("New introduction", read("docs/guide/intro.txt"));
        assertEquals("New content", read("data.txt"));
        // REPLACE of a single file replaces the file alone, not the folder it is in.
        assertEquals(DESCRIPTOR, read("rules.xml"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void writeLeavesAFileThatIsThereAsItIsForSkip() throws Exception {
        var outcome = service.writeResource(root, "data.txt", IOUtils.toInputStream("New content"), false,
                ConflictPolicy.SKIP);

        assertEquals(FileWriteOutcome.KEPT, outcome);
        assertEquals(List.of(), newCommits());
        assertEquals("Previous content", read("data.txt"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void writeRefusesAFileThatIsThereForFail() throws Exception {
        var content = IOUtils.toInputStream("New content");

        var refused = assertThrows(ConflictException.class,
                () -> service.writeResource(root, "data.txt", content, false, ConflictPolicy.FAIL));

        assertEquals("openl.error.409.file.archive.entry.exists.message", refused.getErrorCode());
        assertEquals(List.of(), newCommits());
        assertEquals("Previous content", read("data.txt"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void writeRefusesAFolderAtThePathWhateverTheConflictPolicy() throws Exception {
        for (var policy : ConflictPolicy.values()) {
            var content = IOUtils.toInputStream("New content");

            var refused = assertThrows(ConflictException.class,
                    () -> service.writeResource(root, "docs/guide", content, false, policy));

            assertEquals("openl.error.409.file.path.is.folder.message", refused.getErrorCode(), policy.name());
        }
        assertEquals(List.of(), newCommits());
        assertEquals("Previous introduction", read("docs/guide/intro.txt"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void deletedFileIsCommittedByTheCurrentUser() throws Exception {
        service.deleteResource(root, "data.txt");

        assertEquals(List.of(CURRENT_USER + "Delete data.txt"), newCommits());
        assertNull(design.check(PROJECT + "/data.txt"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void deletedModuleLeavesTheDescriptorInACommitOfTheCurrentUser() throws Exception {
        service.deleteResource(root, "Rules.xlsx");

        assertEquals(List.of(CURRENT_USER + "Delete Rules.xlsx", CURRENT_USER + "Save rules.xml"), newCommits());
        assertFalse(read("rules.xml").contains("Rules.xlsx"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void copiedFileIsCommittedByTheCurrentUser() throws Exception {
        service.copyResource(root, "data.txt", "copy.txt");

        assertEquals(List.of(CURRENT_USER + "Save copy.txt"), newCommits());
        assertEquals("Previous content", read("copy.txt"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void movedFileIsCommittedByTheCurrentUser() throws Exception {
        service.moveResource(root, "data.txt", "archive/data.txt");

        assertEquals(List.of(CURRENT_USER + "Delete data.txt", CURRENT_USER + "Save data.txt"), newCommits());
        assertEquals("Previous content", read("archive/data.txt"));
        assertNull(design.check(PROJECT + "/data.txt"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void fileDeepInTheProjectIsWrittenByTheCurrentUser() throws Exception {
        service.updateResource(root, "docs/guide/intro.txt", IOUtils.toInputStream("New introduction"));
        service.deleteResource(root, "docs");

        assertEquals(List.of(CURRENT_USER + "Delete intro.txt", CURRENT_USER + "Save intro.txt"), newCommits());
        assertNull(design.check(PROJECT + "/docs/guide/intro.txt"));
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void projectOfARepositoryWithMappedFoldersIsCommittedByTheCurrentUser() throws Exception {
        // The repository names the project after its descriptor, in a folder of its own, and maps the files there.
        try (var mapped = MappedRepository.create(design, "DESIGN/")) {
            var mappedProject = closedProject(mapped, mapped.listFolders("DESIGN/").getFirst());
            var mappedRoot = rootOf(mappedProject);

            service.createResource(mappedRoot, "notes/readme.txt", IOUtils.toInputStream("Hello"), true);
            service.updateResource(mappedRoot, "data.txt", IOUtils.toInputStream("New content"));

            assertEquals(List.of(CURRENT_USER + "Save data.txt", CURRENT_USER + "Save readme.txt"), newCommits());
            assertEquals("Hello", read("notes/readme.txt"));
            assertEquals("New content", read("data.txt"));
            assertLeftClosedAndUnlocked(mappedProject);
        }
    }

    @Test
    void moduleDeletedFromAProjectInABaseFolderIsDroppedFromTheDescriptor() throws Exception {
        // The repository keeps its projects in a base folder, as a design repository does.
        try (var mapped = MappedRepository.create(design, "DESIGN/")) {
            var mappedProject = closedProject(mapped, mapped.listFolders("DESIGN/").getFirst());

            service.deleteResource(rootOf(mappedProject), "Rules.xlsx");

            assertEquals(List.of(CURRENT_USER + "Delete Rules.xlsx", CURRENT_USER + "Save rules.xml"), newCommits());
            assertFalse(read("rules.xml").contains("Rules.xlsx"));
            assertLeftClosedAndUnlocked(mappedProject);
        }
    }

    @Test
    void uploadedWorkbookIsCommittedFormattedInOneCommit() throws Exception {
        formattingService().updateResource(root, "Rules.xlsx", new ByteArrayInputStream(personWorkbook()));

        assertEquals(List.of(CURRENT_USER + "Save Rules.xlsx"), newCommits());
        assertEquals("Title", headerStyle());
        assertLeftClosedAndUnlocked(project);
    }

    @Test
    void archiveReplacingTheProjectIsCommittedFormattedInOneCommit() throws Exception {
        var archive = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(archive)) {
            zip.putNextEntry(new ZipEntry("rules.xml"));
            zip.write(DESCRIPTOR.getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new ZipEntry("Rules.xlsx"));
            zip.write(personWorkbook());
        }

        formattingService().uploadArchive(root, "", new ByteArrayInputStream(archive.toByteArray()), true,
                ConflictPolicy.REPLACE);

        assertEquals(List.of(CURRENT_USER + "Upload archive to repository root"), newCommits());
        assertEquals("Title", headerStyle());
        assertNull(design.check(PROJECT + "/data.txt"), "The archive replaces the project");
        assertLeftClosedAndUnlocked(project);
    }

    /** The service of a session that formats tables on save. */
    private ProjectFilesServiceImpl formattingService() {
        var formatting = mock(TableFormatting.class);
        when(formatting.onSave()).thenReturn(true);
        var studio = mock(WebStudio.class);
        var uploads = new UploadFormatting(formatting) {
            @Override
            public WebStudio getWebStudio() {
                return studio;
            }
        };
        return new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), new ProjectDescriptorCleaner(acl), new BeanValidationProvider(List.of()),
                uploads);
    }

    /** A workbook of one Datatype, at B2 of the sheet the module is named after. */
    private static byte[] personWorkbook() throws IOException {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Rules");
            TableTestProjects.row(sheet, 1, 1, "Datatype Person");
            TableTestProjects.row(sheet, 2, 1, "String", "name");
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /** The style the header of the Datatype has in the workbook the design repository holds. */
    private String headerStyle() throws IOException {
        var committed = userDir.resolve("committed.xlsx");
        try (var item = design.read(PROJECT + "/Rules.xlsx")) {
            Files.write(committed, item.getStream().readAllBytes());
        }
        return TableTestProjects.styleNameAt(committed, "Rules", 1, 1);
    }

    private void seed(String path, String text) throws IOException {
        var file = remoteRoot.toPath().resolve(PROJECT).resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private String read(String path) throws IOException {
        try (var item = design.read(PROJECT + "/" + path)) {
            return new String(item.getStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * The commits the design repository received after it was seeded, newest first, as "author: message". A
     * committer other than the author is named too.
     */
    private List<String> newCommits() throws Exception {
        var commits = new ArrayList<String>();
        try (var git = Git.open(remoteRoot)) {
            for (RevCommit commit : git.log().call()) {
                if (commit.getName().equals(seedCommit)) {
                    break;
                }
                var author = person(commit.getAuthorIdent());
                var committer = person(commit.getCommitterIdent());
                var who = author.equals(committer) ? author : author + ", committed by " + committer;
                commits.add(who + ": " + commit.getFullMessage());
            }
        }
        return commits;
    }

    private static String person(PersonIdent ident) {
        return "%s <%s>".formatted(ident.getName(), ident.getEmailAddress());
    }

    private static void assertLeftClosedAndUnlocked(RulesProject project) {
        assertFalse(project.isOpened());
        assertFalse(project.isLocked());
    }
}
