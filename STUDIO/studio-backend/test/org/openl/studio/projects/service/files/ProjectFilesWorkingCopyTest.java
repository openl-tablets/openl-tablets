package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.service.tables.TableFormatting;
import org.openl.studio.projects.service.tables.UploadFormatting;
import org.openl.studio.projects.validator.ProjectStateValidatorImpl;

/**
 * The files of a project opened in a working copy, written and deleted through the files of the project: each read
 * finds the files as they are now, with no file written or deleted before left out or kept.
 */
class ProjectFilesWorkingCopyTest {

    private static final String PROJECT = "Rating";

    @TempDir
    private Path userDir;
    @TempDir
    private File workspacesRoot;
    private ProjectFileRoot root;
    private ProjectFilesServiceImpl service;

    @BeforeEach
    void openTheProject() throws IOException {
        var local = new LocalRepository(userDir, MetainfoRegistry.open(userDir));
        local.setId("design");
        local.initialize();
        save(local, PROJECT + "/rules.xml", "<project><name>Rating</name></project>");
        save(local, PROJECT + "/rules/Main.xlsx", "workbook");
        var acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProject.class), any())).thenReturn(true);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        var project = new RulesProject(new WorkspaceUserImpl("admin", UserInfo::new), local, local.check(PROJECT),
                null, null, LockEngineImpl.create(workspacesRoot, "rules"));
        project.getArtefacts();
        root = new ProjectFileRoot(project, acl, new ProjectStateValidatorImpl(mock(ProtectedBranchBypassService.class)),
                mock(ProjectFileLookupService.class), () -> new UserInfo("admin", "admin@example.com", "Admin"),
                mock(DesignTimeRepository.class));
        service = new ProjectFilesServiceImpl(acl, new FileNodeMapperImpl(), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), new ProjectDescriptorCleaner(acl), new BeanValidationProvider(List.of()),
                new UploadFormatting(mock(TableFormatting.class)), IgnoredPaths.of(null));
    }

    @Test
    void aFileOfANewFolderIsFoundUnderItsFolder() {
        service.createResource(root, "rules/Model/Driver.md", content("driver"), true);
        service.createResource(root, "rules/Rating/Environment.md", content("environment"), true);

        assertEquals(List.of("Environment.md"), namesUnder("rules/Rating"));
        assertEquals(List.of("Driver.md"), namesUnder("rules/Model"));
    }

    @Test
    void aDeletedFileIsFoundNoMore() {
        service.createResource(root, "rules/Rating/Archive/rate.md", content("rate"), true);
        service.createResource(root, "rules/Rating/premium.md", content("premium"), true);

        service.deleteResource(root, "rules/Rating/Archive/rate.md", null);

        assertThrows(NotFoundException.class, () -> service.getResource(root, "rules/Rating/Archive/rate.md", null));
        assertEquals(List.of("premium.md"), namesUnder("rules/Rating"));
    }

    @Test
    void aMovedFileIsFoundAtItsNewPathAlone() {
        service.createResource(root, "rules/Rating/rate.md", content("rate"), true);

        service.moveResource(root, "rules/Rating/rate.md", "rules/Rating/Factors/rate.md");

        assertThrows(NotFoundException.class, () -> service.getResource(root, "rules/Rating/rate.md", null));
        assertEquals(List.of("rate.md"), namesUnder("rules/Rating/Factors"));
        assertEquals(List.of("rate.md"), namesUnder("rules/Rating"));
    }

    /** The names of the files under a folder of the project, at any depth. */
    private List<String> namesUnder(String folder) {
        var query = FileCriteriaQuery.builder().basePath(folder).build();
        return service.getResources(root, query, true, FileViewMode.FLAT, null)
                .stream()
                .filter(node -> "file".equals(node.getType()))
                .map(FsNode::getName)
                .sorted()
                .toList();
    }

    private static ByteArrayInputStream content(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void save(LocalRepository repository, String name, String text) throws IOException {
        var data = new FileData();
        data.setName(name);
        repository.save(data, content(text));
    }
}
