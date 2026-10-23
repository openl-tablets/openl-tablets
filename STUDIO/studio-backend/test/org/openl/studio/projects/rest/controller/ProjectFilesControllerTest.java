package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.files.ConflictPolicy;
import org.openl.studio.projects.service.files.FileViewMode;
import org.openl.studio.projects.service.files.FileWriteOutcome;
import org.openl.studio.projects.service.files.ProjectFileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.validator.file.FileCriteriaQueryValidator;

class ProjectFilesControllerTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();
    private static final ZoneId HELSINKI = ZoneId.of("Europe/Helsinki");
    private static final String FOLDER = "EPBDS";

    @BeforeAll
    static void setUp() {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone(HELSINKI));
    }

    @AfterAll
    static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    @Test
    void namesTheArchiveAfterTheProjectAndTheMomentItWasWritten() {
        var actual = ProjectFilesController.getProjectArchiveName(
                "Rules Service", writtenAt(at(2020, Calendar.AUGUST, 17, 11, 12, 13)), HELSINKI);

        assertEquals("Rules Service-John Smith-2020-08-17_11-12-13.zip", actual);
    }

    @Test
    void namesTheArchiveAfterTheProjectAloneWhenNoMomentIsRecorded() {
        // A project whose folder has gone is reported as existing, with nothing but its name filled in.
        var nameless = new FileData();
        nameless.setName(FOLDER);

        assertEquals("Rules Service.zip",
                ProjectFilesController.getProjectArchiveName("Rules Service", nameless, HELSINKI));
        assertEquals("Rules Service.zip",
                ProjectFilesController.getProjectArchiveName("Rules Service", null, HELSINKI));
    }

    @Test
    void namesTheArchiveAfterTheRevisionItHolds() throws IOException {
        // The project stands on a later revision than the one asked for; the archive is named after what
        // it holds, not after where the project has moved on to.
        var design = repositoryHolding("older", at(2020, Calendar.AUGUST, 16, 9, 8, 7));
        var project = projectStandingOn(at(2020, Calendar.AUGUST, 17, 11, 12, 13), design);

        var actual = ProjectFilesController.getProjectArchiveName(project, "older", HELSINKI);

        assertEquals("Rules Service-John Smith-2020-08-16_09-08-07.zip", actual);
    }

    @Test
    void namesTheArchiveAfterTheProjectWhenNoRevisionIsAskedFor() throws IOException {
        var project = projectStandingOn(at(2020, Calendar.AUGUST, 17, 11, 12, 13), null);

        var actual = ProjectFilesController.getProjectArchiveName(project, null, HELSINKI);

        assertEquals("Rules Service-John Smith-2020-08-17_11-12-13.zip", actual);
    }

    @Test
    void namesTheArchiveAfterTheProjectAloneWhenNothingIsRecordedForTheRevision() throws IOException {
        // The download itself refuses a revision the repository holds nothing for; the name says nothing
        // about it either.
        var project = projectStandingOn(at(2020, Calendar.AUGUST, 17, 11, 12, 13), mock(Repository.class));

        var actual = ProjectFilesController.getProjectArchiveName(project, "gone", HELSINKI);

        assertEquals("Rules Service.zip", actual);
    }

    private static Date at(int year, int month, int day, int hour, int minute, int second) {
        var calendar = new GregorianCalendar();
        calendar.set(year, month, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    @Test
    void formatsTheFilesOfEveryUploadIntoAnOpenedProjectOnceTheSessionIsRefreshed() throws IOException {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(true);
        var files = mock(ProjectFilesService.class);
        when(files.uploadFiles(any(), eq("rules"), any(), eq(ConflictPolicy.FAIL)))
                .thenReturn(List.of("rules/Main.xlsx"));
        when(files.uploadArchive(any(), eq(""), any(), eq(true), eq(ConflictPolicy.REPLACE)))
                .thenReturn(List.of("Main.xlsx", "rules.xml"));
        when(files.writeResource(any(), any(), any(), anyBoolean(), any())).thenReturn(FileWriteOutcome.CREATED);
        var workspace = mock(WorkspaceProjectService.class);
        var studio = mock(WebStudio.class);
        var controller = controller(files, project, workspace, studio);
        var workbook = new MockMultipartFile("file", "Main.xlsx", null, new byte[]{1});

        controller.createResource(project, "/rules/", List.of(workbook), false, ConflictPolicy.FAIL, null);
        controller.createResource(project, "/Main.xlsx", List.of(workbook), false, ConflictPolicy.FAIL, null);
        controller.createResourceRaw(project, "/Raw.xlsx", false, ConflictPolicy.FAIL, null,
                new ByteArrayInputStream(new byte[]{1}));
        controller.uploadArchive(project, "/", true, ConflictPolicy.REPLACE, null,
                new ByteArrayInputStream(new byte[]{1}));
        controller.updateResource(project, "/Main.xlsx", workbook, null, null);
        controller.updateResourceRaw(project, "/Raw.xlsx", null, new ByteArrayInputStream(new byte[]{1}), null);

        var order = inOrder(studio, workspace);
        order.verify(studio).reset();
        order.verify(workspace).formatUploaded(project, List.of("rules/Main.xlsx"));
        order.verify(studio).reset();
        order.verify(workspace).formatUploaded(project, List.of("Main.xlsx"));
        order.verify(studio).reset();
        order.verify(workspace).formatUploaded(project, List.of("Raw.xlsx"));
        order.verify(studio).reset();
        order.verify(workspace).formatUploaded(project, List.of("Main.xlsx", "rules.xml"));
        order.verify(studio).reset();
        order.verify(workspace).formatUploaded(project, List.of("Main.xlsx"));
        order.verify(studio).reset();
        order.verify(workspace).formatUploaded(project, List.of("Raw.xlsx"));
    }

    @Test
    void leavesAnUploadIntoAClosedProjectFormattedBeforeItsCommit() {
        var project = mock(RulesProject.class);
        var workspace = mock(WorkspaceProjectService.class);
        var files = mock(ProjectFilesService.class);
        when(files.writeResource(any(), any(), any(), anyBoolean(), any())).thenReturn(FileWriteOutcome.CREATED);
        var controller = controller(files, project, workspace, mock(WebStudio.class));

        controller.createResourceRaw(project, "/Raw.xlsx", false, ConflictPolicy.FAIL, null,
                new ByteArrayInputStream(new byte[]{1}));

        verify(workspace, never()).formatUploaded(any(), any());
    }

    @Test
    void answersWhetherTheFileWasCreatedOrWasAlreadyThere() throws IOException {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(true);
        var files = mock(ProjectFilesService.class);
        when(files.writeResource(any(), eq("New.xlsx"), any(), eq(true), eq(ConflictPolicy.SKIP)))
                .thenReturn(FileWriteOutcome.CREATED);
        when(files.writeResource(any(), eq("Main.xlsx"), any(), eq(false), eq(ConflictPolicy.OVERWRITE)))
                .thenReturn(FileWriteOutcome.REPLACED);
        when(files.writeResource(any(), eq("Kept.xlsx"), any(), eq(false), eq(ConflictPolicy.SKIP)))
                .thenReturn(FileWriteOutcome.KEPT);
        var workspace = mock(WorkspaceProjectService.class);
        var controller = controller(files, project, workspace, mock(WebStudio.class));
        var workbook = new MockMultipartFile("file", "Main.xlsx", null, new byte[]{1});

        var created = controller.createResourceRaw(project, "/New.xlsx", true, ConflictPolicy.SKIP, null,
                new ByteArrayInputStream(new byte[]{1}));
        var replaced = controller.createResource(project, "/Main.xlsx", List.of(workbook), false,
                ConflictPolicy.OVERWRITE, null);
        var kept = controller.createResourceRaw(project, "/Kept.xlsx", false, ConflictPolicy.SKIP, null,
                new ByteArrayInputStream(new byte[]{1}));

        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, replaced.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, kept.getStatusCode());
        // A file left as it is was not uploaded, so it is not formatted either.
        verify(workspace).formatUploaded(project, List.of("New.xlsx"));
        verify(workspace).formatUploaded(project, List.of("Main.xlsx"));
        verify(workspace).formatUploaded(project, List.of());
    }

    @Test
    void formatsNothingOfAnUploadThatFails() {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(true);
        var files = mock(ProjectFilesService.class);
        doThrow(new ConflictException("file.archive.entry.exists.message", "Raw.xlsx"))
                .when(files).writeResource(any(), any(), any(), anyBoolean(), any());
        var workspace = mock(WorkspaceProjectService.class);
        var studio = mock(WebStudio.class);
        var controller = controller(files, project, workspace, studio);
        var content = new ByteArrayInputStream(new byte[]{1});

        assertThrows(ConflictException.class,
                () -> controller.createResourceRaw(project, "/Raw.xlsx", false, ConflictPolicy.FAIL, null, content));

        verify(studio).reset();
        verify(workspace, never()).formatUploaded(any(), any());
    }

    /** A file read tells its version, and a write and a deletion of the file name it back. */
    @Test
    void aFileReadTellsItsVersionAndAWriteNamesItBack() throws Exception {
        var project = mock(RulesProject.class);
        var files = mock(ProjectFilesService.class);
        var resource = mock(AProjectResource.class);
        when(resource.getName()).thenReturn("rules.xml");
        when(resource.getContent()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(files.getResource(any(), eq("rules.xml"), any())).thenReturn(resource);
        when(files.getVersion(resource)).thenReturn("v1");
        var controller = controller(files, project, mock(WorkspaceProjectService.class), mock(WebStudio.class));
        var response = new MockHttpServletResponse();

        controller.getFile(project, "/rules.xml", null, null, null, null, false, false, FileViewMode.FLAT, null, null,
                null, response);

        assertEquals("\"v1\"", response.getHeader(HttpHeaders.ETAG));
        var content = new ByteArrayInputStream(new byte[]{2});
        controller.updateResourceRaw(project, "/rules.xml", null, content, "\"v1\"");
        verify(files).updateResource(any(), eq("rules.xml"), eq(content), eq("v1"));
        controller.deleteResource(project, "/rules.xml", null, "\"v1\"");
        verify(files).deleteResource(any(), eq("rules.xml"), eq("v1"));
    }

    /** The metadata of a file tell its version as a read of the file does, without the content. */
    @Test
    void theMetadataOfAFileTellItsVersion() throws Exception {
        var project = mock(RulesProject.class);
        var files = mock(ProjectFilesService.class);
        var resource = mock(AProjectResource.class);
        when(files.getResource(any(), eq("rules.xml"), any())).thenReturn(resource);
        when(files.getVersion(resource)).thenReturn("v1");
        var controller = controller(files, project, mock(WorkspaceProjectService.class), mock(WebStudio.class));

        var meta = controller.getFile(project, "/rules.xml", "meta", null, null, null, false, false,
                FileViewMode.FLAT, null, null, null, new MockHttpServletResponse());

        assertEquals("\"v1\"", meta.getHeaders().getETag());
    }

    /** The controller of a session, mounting the files of the project. */
    private static ProjectFilesController controller(ProjectFilesService files, RulesProject project,
                                                     WorkspaceProjectService workspace, WebStudio studio) {
        var root = mock(ProjectFileRoot.class);
        when(root.getProject()).thenReturn(project);
        var roots = mock(ProjectFileRootFactory.class);
        when(roots.of(project)).thenReturn(root);
        return new ProjectFilesController(files, roots, mock(BeanValidationProvider.class),
                mock(FileCriteriaQueryValidator.class), workspace) {
            @Override
            public WebStudio getWebStudio() {
                return studio;
            }
        };
    }

    private static FileData writtenAt(Date modifiedAt) {
        var fileData = mock(FileData.class);
        when(fileData.getModifiedAt()).thenReturn(modifiedAt);
        when(fileData.getAuthor()).thenReturn(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        return fileData;
    }

    private static RulesProject projectStandingOn(Date modifiedAt, @Nullable Repository design) {
        // The file data is built before the stubbing starts: a mock made inside when(...) leaves that
        // stubbing unfinished.
        var fileData = writtenAt(modifiedAt);
        var project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn("Rules Service");
        when(project.getFileData()).thenReturn(fileData);
        when(project.getDesignRepository()).thenReturn(design);
        when(project.getDesignFolderName()).thenReturn(FOLDER);
        return project;
    }

    /** A repository holding that revision of the project's folder. */
    private static Repository repositoryHolding(String version, Date modifiedAt) throws IOException {
        var older = writtenAt(modifiedAt);
        var repository = mock(Repository.class);
        when(repository.checkHistory(FOLDER, version)).thenReturn(older);
        return repository;
    }
}
