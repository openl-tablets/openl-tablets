package org.openl.studio.projects.service.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
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
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
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
        Props.setEnvironment(new MockEnvironment().withProperty(AdministrationSettings.DATETIME_PATTERN,
                "yyyy-MM-dd HH:mm:ss")
                .withProperty(AdministrationSettings.USER_WORKSPACE_HOME, workspace.toString()));
        projectFolder = Files.createDirectories(workspace.resolve("TestProject"));
        Files.copy(Path.of("test-resources/openapi-import/declared-modules/rules.xml"),
                projectFolder.resolve("rules.xml"));

        when(project.isOpened()).thenReturn(true);
        when(project.getLocalRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("TestProject");
        when(project.getArtefact("rules/Bank Rating.xlsx")).thenReturn(moduleArtefact);
        when(repository.getRoot()).thenReturn(workspace);
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
        assertEquals("2000_current", result.get(0).id);
        assertEquals(Boolean.TRUE, result.get(0).current);
        assertEquals("1000", result.get(1).id);
        assertEquals("Revision Version", result.get(2).id);
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

    @Test
    void restoresWithModuleWritePermissionWhenProjectWritePermissionIsDenied() throws Exception {
        var moduleFile = Files.createDirectories(projectFolder.resolve("rules")).resolve("Bank Rating.xlsx");
        Files.writeString(moduleFile, "current version");
        var history = Files.createDirectories(historyFolder(workspace));
        Files.writeString(history.resolve("Revision Version"), "revision version");
        Files.writeString(history.resolve("2000_current"), "current version");
        when(aclService.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(false);

        service.restore(project, "Bank Rating", "Revision Version", null);

        assertEquals("revision version", Files.readString(moduleFile));
        verify(aclService).isGranted(moduleArtefact, List.of(BasePermission.WRITE));
        verify(aclService, never()).isGranted(project, List.of(BasePermission.WRITE));
    }

    @Test
    void rejectsRestoreWithoutModuleWritePermission() {
        when(aclService.isGranted(moduleArtefact, List.of(BasePermission.WRITE))).thenReturn(false);

        var error = assertThrows(ForbiddenException.class,
                () -> service.restore(project, "Bank Rating", "Revision Version", null));

        assertEquals("openl.error.403.default.message", error.getErrorCode());
    }

    @Test
    void rejectsVersionOutsideModuleHistory() throws Exception {
        var history = Files.createDirectories(historyFolder(workspace));
        var outside = Files.writeString(workspace.resolve("outside"), "outside history");
        Files.createSymbolicLink(history.resolve("linked"), outside);

        var error = assertThrows(NotFoundException.class,
                () -> service.getHistoryVersions(project, "Bank Rating", List.of("../../outside")));
        var rootError = assertThrows(NotFoundException.class,
                () -> service.getHistoryVersions(project, "Bank Rating", List.of("/")));
        var linkedError = assertThrows(NotFoundException.class,
                () -> service.getHistoryVersions(project, "Bank Rating", List.of("linked")));

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

        assertEquals(List.of("first", "second"), versions.stream().map(file -> file.getName()).toList());
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

    private static Path historyFolder(Path workspace) {
        return workspace.resolve(FolderHelper.HISTORY_FOLDER)
                .resolve("TestProject")
                .resolve("rules/Bank Rating.xlsx");
    }
}
