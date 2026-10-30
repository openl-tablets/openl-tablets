package org.openl.studio.projects.service.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.LockedException;
import org.openl.studio.common.exception.NotFoundException;

class ProjectHistoryServiceTest {

    private final RepositoryAclService aclService = mock(RepositoryAclService.class);
    private final ProjectHistoryService service = new ProjectHistoryService(aclService);
    private final RulesProject project = mock(RulesProject.class);
    private final AProjectArtefact moduleArtefact = mock(AProjectArtefact.class);
    private final LocalRepository repository = mock(LocalRepository.class);
    private Environment previousEnvironment;
    private Path projectFolder;
    @TempDir
    private Path workspace;

    @BeforeEach
    void setUp() throws Exception {
        previousEnvironment = Props.getEnvironment();
        Props.setEnvironment(new MockEnvironment()
                .withProperty(AdministrationSettings.USER_WORKSPACE_HOME, workspace.toString()));
        projectFolder = Files.createDirectories(workspace.resolve("TestProject"));
        Files.copy(Path.of("test-resources/openapi-import/declared-modules/rules.xml"),
                projectFolder.resolve("rules.xml"));

        when(project.isOpened()).thenReturn(true);
        when(project.getLocalRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("TestProject");
        when(project.getArtefact("rules/Bank Rating.xlsx")).thenReturn(moduleArtefact);
        when(repository.getRoot()).thenReturn(workspace);
        when(repository.getWorkspaceFolder()).thenReturn(workspace);
        when(aclService.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(true);
        when(aclService.isGranted(moduleArtefact, List.of(BasePermission.WRITE))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        Props.setEnvironment(previousEnvironment);
    }

    @Test
    void readsSelectedModuleHistory() throws Exception {
        var history = Files.createDirectories(historyFolder(workspace));
        Files.createFile(history.resolve("Revision Version"));
        Files.createFile(history.resolve("1000"));
        Files.createFile(history.resolve("2000_current"));

        var result = service.getLocalHistory(project, "Bank Rating");

        assertEquals(3, result.size());
        assertEquals("2000_current", result.getFirst().id);
        assertEquals(Boolean.TRUE, result.getFirst().current);
        assertEquals(Instant.ofEpochMilli(2000), result.getFirst().modifiedOn);
        assertEquals("1000", result.get(1).id);
        assertEquals(Instant.ofEpochMilli(1000), result.get(1).modifiedOn);
        assertEquals("Revision Version", result.get(2).id);
        // the revision the module was opened at is named by no time
        assertNull(result.get(2).modifiedOn);
    }

    @Test
    void readsTheHistoryOfAProjectEditedInPlaceFromTheWorkspace(@TempDir Path design) throws Exception {
        // The project is read from the folder of the design repository, under a folder path of its own there.
        var editedInPlace = Files.createDirectories(design.resolve("rating/bank"));
        Files.copy(projectFolder.resolve("rules.xml"), editedInPlace.resolve("rules.xml"));
        when(repository.getRoot()).thenReturn(design);
        when(repository.getId()).thenReturn("design");
        when(project.isInPlace()).thenReturn(true);
        when(project.getFolderPath()).thenReturn("rating/bank");
        var history = Files.createDirectories(workspace.resolve(
                ".history/.in-place/design/rating/bank/rules/Bank Rating.xlsx"));
        Files.createFile(history.resolve("Revision Version"));
        Files.createFile(history.resolve("1000_current"));
        // A copy in the workspace with the same folder path keeps a history of its own.
        var copyHistory = Files.createDirectories(workspace.resolve(".history/rating/bank/rules/Bank Rating.xlsx"));
        Files.createFile(copyHistory.resolve("2000_current"));

        var result = service.getLocalHistory(project, "Bank Rating");

        assertEquals(List.of("1000_current", "Revision Version"), result.stream().map(item -> item.id).toList());
        service.deleteProjectHistory(project);
        assertFalse(Files.exists(workspace.resolve(".history/.in-place/design/rating/bank")));
        assertTrue(Files.exists(copyHistory.resolve("2000_current")));
        assertFalse(Files.exists(design.resolve(".history")), "nothing is written into the design repository");
    }

    @Test
    void usesFirstModuleWhenModuleIsOmitted() throws Exception {
        var history = Files.createDirectories(historyFolder(workspace));
        Files.createFile(history.resolve("Revision Version_current"));

        assertEquals(0, service.getLocalHistory(project, null).size());
    }

    @Test
    void rejectsUnknownModule() {
        var error = assertThrows(NotFoundException.class,
                () -> service.getLocalHistory(project, "Unknown"));

        assertEquals("openl.error.404.project.module.identifier.message", error.getErrorCode());
    }

    @Test
    void rejectsClosedProject() {
        when(project.isOpened()).thenReturn(false);

        var error = assertThrows(ConflictException.class,
                () -> service.getLocalHistory(project, null));
        var deleteError = assertThrows(ConflictException.class,
                () -> service.deleteProjectHistory(project));

        assertEquals("openl.error.409.project.not.opened.message", error.getErrorCode());
        assertEquals(error.getErrorCode(), deleteError.getErrorCode());
    }

    @Test
    void rejectsProjectThatCannotBeResolved() throws Exception {
        Files.delete(projectFolder.resolve("rules.xml"));

        var error = assertThrows(NotFoundException.class,
                () -> service.getLocalHistory(project, null));

        assertEquals("openl.error.404.project.identifier.message", error.getErrorCode());
    }

    @Test
    void restoresRequestedModuleWithoutResettingAnotherSessionModule() throws Exception {
        var moduleFile = Files.createDirectories(projectFolder.resolve("rules")).resolve("Bank Rating.xlsx");
        Files.writeString(moduleFile, "current version");
        var history = Files.createDirectories(historyFolder(workspace));
        Files.writeString(history.resolve("Revision Version"), "revision version");
        Files.writeString(history.resolve("2000_current"), "current version");
        var otherModuleFile = Files.writeString(projectFolder.resolve("Other.xlsx"), "other module");
        var currentModule = mock(Module.class);
        when(currentModule.getRulesPath()).thenReturn(otherModuleFile);
        var model = mock(ProjectModel.class);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getCurrentModule()).thenReturn(currentModule);
        when(webStudio.getModel()).thenReturn(model);

        service.restore(project, "Bank Rating", "Revision Version", webStudio);

        assertEquals("revision version", Files.readString(moduleFile));
        assertTrue(Files.exists(history.resolve("Revision Version_current")));
        assertTrue(Files.exists(history.resolve("2000")));
        assertFalse(Files.exists(history.resolve("2000_current")));
        verify(model, never()).reset(any());
    }

    @Test
    void keepsTheVersionAnotherProgramWroteBeforeRestoringOverIt() throws Exception {
        var moduleFile = Files.createDirectories(projectFolder.resolve("rules")).resolve("Bank Rating.xlsx");
        Files.writeString(moduleFile, "as Excel wrote it");
        var history = Files.createDirectories(historyFolder(workspace));
        Files.writeString(history.resolve("Revision Version"), "revision version");
        Files.writeString(history.resolve("2000_current"), "saved last");

        // The version saved last was the current one, yet the file holds another: it is restored, not skipped.
        service.restore(project, "Bank Rating", "2000_current", mock(WebStudio.class));

        assertEquals("saved last", Files.readString(moduleFile));
        try (var versions = Files.list(history)) {
            var excel = versions.filter(version -> !version.getFileName().toString().startsWith("2000"))
                    .filter(version -> !version.getFileName().toString().startsWith("Revision"))
                    .toList();
            assertEquals(1, excel.size(), "the version Excel wrote is kept");
            assertEquals("as Excel wrote it", Files.readString(excel.getFirst()));
        }
        assertTrue(Files.exists(history.resolve("2000_current")));
    }

    @Test
    void keepsEveryVersionSavedWithinOneMillisecond() throws Exception {
        var moduleFile = Files.createDirectories(projectFolder.resolve("rules")).resolve("Bank Rating.xlsx");
        var history = Files.createDirectories(historyFolder(workspace));
        Files.writeString(history.resolve("Revision Version_current"), "revision version");

        for (var i = 1; i <= 5; i++) {
            Files.writeString(moduleFile, "version " + i);
            ProjectHistoryService.save(history.toString(), moduleFile.toFile());
        }

        try (var versions = Files.list(history)) {
            assertEquals(6, versions.count(), "the first version and each version saved");
        }
    }

    @Test
    void reloadsRestoredModuleWhenItIsOpenInTheSession() throws Exception {
        var moduleFile = Files.createDirectories(projectFolder.resolve("rules")).resolve("Bank Rating.xlsx");
        Files.writeString(moduleFile, "current version");
        var history = Files.createDirectories(historyFolder(workspace));
        Files.writeString(history.resolve("Revision Version"), "revision version");
        Files.writeString(history.resolve("2000_current"), "current version");
        var currentModule = mock(Module.class);
        when(currentModule.getRulesPath()).thenReturn(moduleFile);
        var model = mock(ProjectModel.class);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getCurrentModule()).thenReturn(currentModule);
        when(webStudio.getModel()).thenReturn(model);

        service.restore(project, "Bank Rating", "Revision Version", webStudio);

        verify(model).reset(ReloadType.SINGLE);
    }

    /** A module compiled with the module open reads what it holds after a restore, as the module open does. */
    @Test
    void reloadsTheProjectWhenARestoredModuleIsCompiledWithTheModuleOpen() throws Exception {
        var moduleFile = Files.createDirectories(projectFolder.resolve("rules")).resolve("Bank Rating.xlsx");
        Files.writeString(moduleFile, "current version");
        var history = Files.createDirectories(historyFolder(workspace));
        Files.writeString(history.resolve("Revision Version"), "revision version");
        Files.writeString(history.resolve("2000_current"), "current version");
        var currentModule = mock(Module.class);
        when(currentModule.getRulesPath()).thenReturn(Files.writeString(projectFolder.resolve("Other.xlsx"), "other"));
        var model = mock(ProjectModel.class);
        when(model.isCompiledFrom(moduleFile)).thenReturn(true);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getCurrentModule()).thenReturn(currentModule);
        when(webStudio.getModel()).thenReturn(model);

        service.restore(project, "Bank Rating", "Revision Version", webStudio);

        assertEquals("revision version", Files.readString(moduleFile));
        verify(model).reset(ReloadType.RELOAD);
    }

    @Test
    void aWorkbookTheFileSystemRefusesToReplaceIsToldToBeClosed() throws Exception {
        var moduleFile = Files.createDirectories(projectFolder.resolve("rules")).resolve("Bank Rating.xlsx");
        Files.writeString(moduleFile, "current version");
        var history = Files.createDirectories(historyFolder(workspace));
        Files.writeString(history.resolve("Revision Version"), "revision version");
        Files.writeString(history.resolve("2000_current"), "current version");
        assertTrue(moduleFile.toFile().setWritable(false));
        try {
            // An administrator writes a read-only file all the same: there is nothing to refuse then.
            assumeFalse(Files.isWritable(moduleFile));
            var webStudio = mock(WebStudio.class);

            var refused = assertThrows(LockedException.class,
                    () -> service.restore(project, "Bank Rating", "Revision Version", webStudio));

            assertEquals("openl.error.423.restore.xls-file.message", refused.getErrorCode());
            assertEquals("current version", Files.readString(moduleFile));
        } finally {
            assertTrue(moduleFile.toFile().setWritable(true));
        }
    }

    @Test
    void restoresWithModuleWritePermissionWhenProjectWritePermissionIsDenied() throws Exception {
        var moduleFile = Files.createDirectories(projectFolder.resolve("rules")).resolve("Bank Rating.xlsx");
        Files.writeString(moduleFile, "current version");
        var history = Files.createDirectories(historyFolder(workspace));
        Files.writeString(history.resolve("Revision Version"), "revision version");
        Files.writeString(history.resolve("2000_current"), "current version");
        when(aclService.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(false);

        service.restore(project, "Bank Rating", "Revision Version", mock(WebStudio.class));

        assertEquals("revision version", Files.readString(moduleFile));
        verify(aclService).isGranted(moduleArtefact, List.of(BasePermission.WRITE));
        verify(aclService, never()).isGranted(project, List.of(BasePermission.WRITE));
    }

    @Test
    void rejectsRestoreWithoutModuleWritePermission() {
        when(aclService.isGranted(moduleArtefact, List.of(BasePermission.WRITE))).thenReturn(false);

        var webStudio = mock(WebStudio.class);

        var error = assertThrows(ForbiddenException.class,
                () -> service.restore(project, "Bank Rating", "Revision Version", webStudio));

        assertEquals("openl.error.403.default.message", error.getErrorCode());
    }

    @Test
    void rejectsVersionOutsideModuleHistory() throws Exception {
        var history = Files.createDirectories(historyFolder(workspace));
        var outside = Files.writeString(workspace.resolve("outside"), "outside history");
        Files.createSymbolicLink(history.resolve("linked"), outside);
        var outsideVersions = List.of("../../outside");
        var rootVersions = List.of("/");
        var linkedVersions = List.of("linked");

        var error = assertThrows(NotFoundException.class,
                () -> service.getHistoryVersions(project, "Bank Rating", outsideVersions));
        var rootError = assertThrows(NotFoundException.class,
                () -> service.getHistoryVersions(project, "Bank Rating", rootVersions));
        var linkedError = assertThrows(NotFoundException.class,
                () -> service.getHistoryVersions(project, "Bank Rating", linkedVersions));

        assertEquals("openl.error.404.file.version.not.found.message", error.getErrorCode());
        assertEquals(error.getErrorCode(), rootError.getErrorCode());
        assertEquals(error.getErrorCode(), linkedError.getErrorCode());
    }

    @Test
    void resolvesVersionsFromRequestedModuleHistory() throws Exception {
        var history = Files.createDirectories(historyFolder(workspace));
        Files.createFile(history.resolve("first"));
        Files.createFile(history.resolve("second"));

        var versions = service.getHistoryVersions(project, "Bank Rating", List.of("first", "second"));

        assertEquals(List.of("first", "second"), versions.stream().map(File::getName).toList());
    }

    @Test
    void deletesOnlyRequestedProjectHistory() throws Exception {
        var projectHistory = Files.createDirectories(historyFolder(workspace));
        Files.createFile(projectHistory.resolve("Revision Version_current"));
        var otherHistory = Files.createDirectories(workspace.resolve(FolderHelper.HISTORY_FOLDER)
                .resolve("OtherProject")
                .resolve("Main.xlsx"));
        Files.createFile(otherHistory.resolve("Revision Version_current"));

        service.deleteProjectHistory(project);

        assertFalse(Files.exists(workspace.resolve(FolderHelper.HISTORY_FOLDER).resolve("TestProject")));
        assertTrue(Files.exists(otherHistory));
    }

    @Test
    void deletesTheNamedProjectHistoryOfTheGivenWorkspace() throws Exception {
        var projectHistory = Files.createDirectories(historyFolder(workspace));
        Files.createFile(projectHistory.resolve("Revision Version_current"));
        var otherHistory = Files.createDirectories(workspace.resolve(FolderHelper.HISTORY_FOLDER)
                .resolve("OtherProject")
                .resolve("Main.xlsx"));
        Files.createFile(otherHistory.resolve("Revision Version_current"));

        ProjectHistoryService.deleteHistory(userWorkspace(), "TestProject");

        assertFalse(Files.exists(workspace.resolve(FolderHelper.HISTORY_FOLDER).resolve("TestProject")));
        assertTrue(Files.exists(otherHistory));
    }

    @Test
    void deletingTheHistoryOfAProjectWithoutOneChangesNothing() throws Exception {
        ProjectHistoryService.deleteHistory(userWorkspace(), "TestProject");

        assertFalse(Files.exists(workspace.resolve(FolderHelper.HISTORY_FOLDER)));
        assertTrue(Files.exists(projectFolder.resolve("rules.xml")));
    }

    @Test
    void deletesAllUsersHistoryFolders() throws Exception {
        var currentUserHistory = Files.createDirectories(workspace.resolve(FolderHelper.HISTORY_FOLDER)
                .resolve("TestProject"));
        var otherUserHistory = Files.createDirectories(workspace.resolve("other-user")
                .resolve(FolderHelper.HISTORY_FOLDER)
                .resolve("OtherProject"));
        var retainedFolder = Files.createDirectories(workspace.resolve("other-user").resolve("projects"));
        Files.createFile(currentUserHistory.resolve("version"));
        Files.createFile(otherUserHistory.resolve("version"));

        service.deleteAllHistory();

        assertFalse(Files.exists(currentUserHistory));
        assertFalse(Files.exists(otherUserHistory));
        assertTrue(Files.exists(retainedFolder));
    }

    /** The workspace of the user the project is opened by, kept in the test's folder. */
    private UserWorkspace userWorkspace() {
        var localWorkspace = mock(LocalWorkspace.class);
        when(localWorkspace.getLocation()).thenReturn(workspace.toFile());
        var userWorkspace = mock(UserWorkspace.class);
        when(userWorkspace.getLocalWorkspace()).thenReturn(localWorkspace);
        return userWorkspace;
    }

    private static Path historyFolder(Path workspace) {
        return workspace.resolve(FolderHelper.HISTORY_FOLDER)
                .resolve("TestProject")
                .resolve("rules/Bank Rating.xlsx");
    }
}
