package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.tables.TableFormatting;
import org.openl.studio.projects.service.tables.UploadFormatting;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * A file at a path the repositories ignore is never written: a write naming it is refused, and an upload leaves it
 * out. The path is matched as the repository holds it.
 */
class ProjectFilesIgnoredPathsTest {

    private ProjectFilesServiceImpl service;
    private FileRoot root;

    @BeforeEach
    void init() {
        var acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), mock(ProjectDescriptorCleaner.class),
                new BeanValidationProvider(List.of()), new UploadFormatting(mock(TableFormatting.class)),
                IgnoredPaths.of("**/.git*/**, **/pom.xml"));
        root = mock(FileRoot.class);
        when(root.repositoryPathOf(anyString())).thenAnswer(call -> call.getArgument(0));
        var project = mock(AProject.class);
        var data = new FileData();
        data.setName("");
        when(project.getFileData()).thenReturn(data);
        when(root.readFolder(null)).thenReturn(new AProjectFolder(new HashMap<>(), project, null, ""));
    }

    private static ByteArrayInputStream content() {
        return new ByteArrayInputStream("text".getBytes(StandardCharsets.UTF_8));
    }

    private static void assertRefused(Runnable write) {
        var e = assertThrows(ForbiddenException.class, write::run);
        assertEquals("openl.error.403.file.path.ignored.message", e.getErrorCode());
    }

    @Test
    void refusesAWriteThatNamesAnIgnoredPath() {
        assertRefused(() -> service.createResource(root, ".github/workflows/build.yml", content(), true));
        assertRefused(() -> service.createResource(root, ".GIT/hooks/pre-commit", content(), true));
        assertRefused(() -> service.writeResource(root, "P/pom.xml", content(), true, ConflictPolicy.OVERWRITE));
        assertRefused(() -> service.createFolder(root, "P/.git", true));
        assertRefused(() -> service.copyResource(root, "P/rules.xml", "P/pom.xml"));
        assertRefused(() -> service.moveResource(root, "P/rules.xml", "P/pom.xml"));
        var written = Map.of("P/rules.xml", new byte[0], "P/pom.xml", new byte[0]);
        assertRefused(() -> service.writeFiles(root, written, List.of(), "Save"));

        verify(root, never()).writeFolder();
        verify(root, never()).writeBatch(anyString(), any(), any(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void leavesIgnoredPathsOutOfAnUpload() {
        var files = List.of(new ProjectFilesService.UploadedFile("P/rules.xml", "<project/>".getBytes()),
                new ProjectFilesService.UploadedFile("P/pom.xml", "<project/>".getBytes()),
                new ProjectFilesService.UploadedFile(".github/workflows/build.yml", new byte[0]));

        var uploaded = service.uploadFiles(root, "", files, ConflictPolicy.OVERWRITE);

        assertEquals(List.of("P/rules.xml"), uploaded);
        ArgumentCaptor<List<FileItem>> items = ArgumentCaptor.forClass(List.class);
        verify(root).writeBatch(eq(""), items.capture(), eq(ChangesetType.DIFF), anyString());
        assertEquals(List.of("P/rules.xml"), items.getValue().stream().map(item -> item.getData().getName()).toList());
    }

    @Test
    void refusesToReplaceAFolderWithIgnoredFilesOnly() {
        var files = List.of(new ProjectFilesService.UploadedFile("pom.xml", new byte[0]));

        assertThrows(BadRequestException.class, () -> service.uploadFiles(root, "", files, ConflictPolicy.REPLACE));

        verify(root, never()).writeBatch(anyString(), any(), any(), anyString());
    }

    @Test
    void matchesAProjectPathAsTheRepositoryHoldsIt() {
        var project = mock(RulesProject.class);
        when(project.getDesignFolderName()).thenReturn("rules/Bank");
        var mount = new ProjectFileRoot(project, mock(AclProjectsHelper.class), mock(ProjectStateValidator.class),
                mock(ProjectFileLookupService.class), () -> null, mock(DesignTimeRepository.class));

        assertEquals("rules/Bank/pom.xml", mount.repositoryPathOf("pom.xml"));
        when(project.getDesignFolderName()).thenReturn(null);
        assertEquals("pom.xml", mount.repositoryPathOf("pom.xml"));
    }
}
