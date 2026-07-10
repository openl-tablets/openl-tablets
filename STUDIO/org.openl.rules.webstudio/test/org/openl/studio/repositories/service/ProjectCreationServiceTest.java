package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.repositories.model.RepositoryFolder;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTypeService;

class ProjectCreationServiceTest {

    private AclProjectsHelper aclProjectsHelper;
    private RepositoryAclServiceProvider aclServiceProvider;
    private TagTypeService tagTypeService;
    private TagService tagService;
    private Comments comments;
    private ProjectCreationService service;

    @BeforeEach
    void setUp() {
        aclProjectsHelper = mock(AclProjectsHelper.class);
        aclServiceProvider = mock(RepositoryAclServiceProvider.class);
        tagTypeService = mock(TagTypeService.class);
        tagService = mock(TagService.class);
        comments = mock(Comments.class);
        service = new ProjectCreationService(aclProjectsHelper, aclServiceProvider, tagTypeService, tagService,
                mock(PathFilter.class), mock(ZipCharsetDetector.class), "");
    }

    @Test
    void lists_predefined_templates_without_error() {
        // The custom resolver is not initialised outside a Spring context; listing must not fail.
        assertNotNull(service.listTemplates());
    }

    @Test
    void create_from_template_is_denied_without_create_permission() {
        assertThrows(ForbiddenException.class, () -> service.createFromTemplate("design", "Project", null,
                "predefined", "examples", "Example", "comment", null));
    }

    @Test
    void create_from_files_is_denied_without_create_permission() {
        List<ProjectFile> files = List.of();
        assertThrows(ForbiddenException.class, () -> service.createFromFiles("design", "Project", null,
                files, "comment", "rules/Models.xlsx", "rules/Algorithms.xlsx", "Models", "Algorithms", null));
    }

    @Test
    void upload_local_projects_is_denied_without_create_permission() {
        var names = List.of("Local");
        assertThrows(ForbiddenException.class,
                () -> service.uploadLocalProjects("design", names, null, "comment"));
    }

    @Test
    void upload_local_projects_rolls_back_previous_publishes_when_batch_fails() throws ProjectException {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var workspace = mock(UserWorkspace.class);
        var user = mock(WorkspaceUser.class);
        when(workspace.getUser()).thenReturn(user);
        var acl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);
        when(comments.createProject("First")).thenReturn("Create First");
        when(comments.createProject("Second")).thenReturn("Create Second");

        var first = publishedProject("First");
        when(workspace.uploadLocalProject("design", "First", "target/", "Create First")).thenReturn(first);
        when(workspace.uploadLocalProject("design", "Second", "target/", "Create Second"))
                .thenThrow(new ProjectException("failed"));

        service = serviceWithWorkspace(workspace);

        var names = List.of("First", "Second");
        assertThrows(ConflictException.class,
                () -> service.uploadLocalProjects("design", names, "target/", null));

        verify(first).delete(user, "Rollback project upload.");
        verify(acl).deleteAcl(first);
        verify(workspace).refresh();
    }

    @Test
    void list_importable_folders_is_denied_without_create_permission() {
        assertThrows(ForbiddenException.class, () -> service.listImportableFolders("design", "folder"));
    }

    @Test
    void list_importable_folders_is_denied_without_read_permission() {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var acl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);
        var workspace = mock(UserWorkspace.class);
        service = serviceWithWorkspace(workspace);

        assertThrows(ForbiddenException.class, () -> service.listImportableFolders("design", "folder"));

        verify(workspace, never()).getDesignTimeRepository();
    }

    @Test
    void list_importable_folders_filters_unreadable_children() throws IOException {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var acl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);
        when(acl.isGranted("design", null, List.of(BasePermission.READ))).thenReturn(true);
        when(acl.isGranted("design", "Allowed", List.of(BasePermission.READ))).thenReturn(true);

        var workspace = mock(UserWorkspace.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);

        var repository = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        var mapper = (FolderMapper) repository;
        var delegate = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setMappedFolders(true).build());
        when(designTimeRepository.getRepository("design")).thenReturn(repository);
        when(mapper.getDelegate()).thenReturn(delegate);
        when(delegate.listFolders("")).thenReturn(List.of(folder("Allowed/"), folder("Denied/")));
        when(delegate.list("Allowed/")).thenReturn(List.of());

        service = serviceWithWorkspace(workspace);

        List<RepositoryFolder> result = service.listImportableFolders("design", null);

        assertEquals(List.of("Allowed"), result.stream().map(RepositoryFolder::path).toList());
        verify(delegate, never()).list("Denied/");
    }

    @Test
    void import_from_repository_is_denied_without_create_permission() {
        assertThrows(ForbiddenException.class, () -> service.importFromRepository("design", "folder"));
    }

    @Test
    void import_from_repository_is_denied_without_read_permission() {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var acl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);
        var workspace = mock(UserWorkspace.class);
        service = serviceWithWorkspace(workspace);

        assertThrows(ForbiddenException.class, () -> service.importFromRepository("design", "folder"));

        verify(workspace, never()).getDesignTimeRepository();
    }

    @Test
    void import_from_repository_rolls_back_mapping_when_project_cannot_be_resolved() throws IOException {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var acl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);
        when(acl.isGranted("design", "folder", List.of(BasePermission.READ))).thenReturn(true);
        var workspace = mock(UserWorkspace.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);

        var repository = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        var mapper = (FolderMapper) repository;
        var delegate = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setMappedFolders(true).build());
        when(designTimeRepository.getRepository("design")).thenReturn(repository);
        when(mapper.getDelegate()).thenReturn(delegate);
        when(delegate.check("folder")).thenReturn(new FileData());
        when(mapper.findMappedName("folder")).thenReturn("Imported__hash");
        when(workspace.getProjectByPath("design", "folder")).thenReturn(Optional.empty());

        service = serviceWithWorkspace(workspace);

        assertThrows(ConflictException.class, () -> service.importFromRepository("design", "folder/"));

        verify(mapper).addMapping("folder");
        verify(workspace).refresh();
        verify(mapper).removeMapping("Imported__hash");
    }

    @Test
    void copy_project_is_denied_without_create_permission() {
        assertThrows(ForbiddenException.class,
                () -> service.copyProject("design", "Copy", null, "design", "Source", "comment"));
    }

    @Test
    void copy_internal_path_normalizes_target_folder() {
        assertEquals("Rules", ProjectCreationService.copyInternalPath(null, "Rules"));
        assertEquals("folder/Rules", ProjectCreationService.copyInternalPath("folder", "Rules"));
        assertEquals("folder/Rules", ProjectCreationService.copyInternalPath("folder/", "Rules"));
        assertEquals("folder/Rules", ProjectCreationService.copyInternalPath("/folder", "Rules"));
        assertEquals("folder/nested/Rules", ProjectCreationService.copyInternalPath("\\folder\\nested", "Rules"));
    }

    @Test
    void registers_only_new_values_of_extensible_tag_types() {
        var extensible = new TagType();
        extensible.setId(1L);
        extensible.setName("Environment");
        extensible.setExtensible(true);
        var fixed = new TagType();
        fixed.setId(2L);
        fixed.setName("Team");
        fixed.setExtensible(false);
        when(tagTypeService.getByName("Environment")).thenReturn(extensible);
        when(tagTypeService.getByName("Team")).thenReturn(fixed);
        when(tagTypeService.getByName("Unknown")).thenReturn(null);
        when(tagService.getByName(1L, "prod")).thenReturn(null); // extensible + not yet in the catalog

        var project = mock(RulesProject.class);
        when(project.getDesignTags()).thenReturn(Map.of());
        when(project.getLocalTags()).thenReturn(Map.of("Environment", "prod", "Team", "Payroll", "Unknown", "x"));

        service.registerExtensibleTags(project);

        var saved = ArgumentCaptor.forClass(Tag.class);
        verify(tagService).save(saved.capture());
        assertEquals("prod", saved.getValue().getName());
        assertEquals("Environment", saved.getValue().getType().getName());
    }

    @Test
    void does_not_re_register_an_existing_extensible_value() {
        var extensible = new TagType();
        extensible.setId(1L);
        extensible.setName("Environment");
        extensible.setExtensible(true);
        when(tagTypeService.getByName("Environment")).thenReturn(extensible);
        when(tagService.getByName(1L, "prod")).thenReturn(new Tag()); // already present

        var project = mock(RulesProject.class);
        when(project.getDesignTags()).thenReturn(Map.of());
        when(project.getLocalTags()).thenReturn(Map.of("Environment", "prod"));

        service.registerExtensibleTags(project);

        verify(tagService, never()).save(any());
    }

    @Test
    void registers_extensible_values_from_design_tags() {
        var extensible = new TagType();
        extensible.setId(1L);
        extensible.setName("Environment");
        extensible.setExtensible(true);
        when(tagTypeService.getByName("Environment")).thenReturn(extensible);
        when(tagService.getByName(1L, "prod")).thenReturn(null);

        var project = mock(RulesProject.class);
        when(project.getDesignTags()).thenReturn(Map.of("Environment", "prod")); // archive/copy exposes tags via design
        when(project.getLocalTags()).thenReturn(Map.of());

        service.registerExtensibleTags(project);

        var saved = ArgumentCaptor.forClass(Tag.class);
        verify(tagService).save(saved.capture());
        assertEquals("prod", saved.getValue().getName());
    }

    @Test
    void registerExtensibleTagsAfterDesignChange_refreshes_workspace() {
        var workspace = mock(UserWorkspace.class);
        service = serviceWithWorkspace(workspace);

        service.registerExtensibleTagsAfterDesignChange(mock(AProject.class));

        verify(workspace).refresh();
        verifyNoInteractions(tagTypeService, tagService);
    }

    @Test
    void registers_extensible_values_from_design_project_tags() throws ProjectException {
        var extensible = new TagType();
        extensible.setId(1L);
        extensible.setName("Environment");
        extensible.setExtensible(true);
        when(tagTypeService.getByName("Environment")).thenReturn(extensible);

        var project = mock(AProject.class);
        var tags = mock(AProjectResource.class);
        when(project.hasArtefact(ProjectTags.TAGS_FILE_NAME)).thenReturn(true);
        when(project.getArtefact(ProjectTags.TAGS_FILE_NAME)).thenReturn(tags);
        when(tags.getContent()).thenReturn(new ByteArrayInputStream(
                "Environment=prod\n".getBytes(StandardCharsets.UTF_8)));

        service.registerExtensibleTags(project);

        var saved = ArgumentCaptor.forClass(Tag.class);
        verify(tagService).save(saved.capture());
        assertEquals("prod", saved.getValue().getName());
        assertEquals("Environment", saved.getValue().getType().getName());
    }

    @Test
    void skips_design_project_tags_when_tags_file_is_absent() {
        service.registerExtensibleTags(mock(AProject.class));

        verifyNoInteractions(tagTypeService, tagService);
    }

    private ProjectCreationService serviceWithWorkspace(UserWorkspace workspace) {
        return new TestProjectCreationService(aclProjectsHelper, aclServiceProvider,
                tagTypeService, tagService, mock(PathFilter.class), mock(ZipCharsetDetector.class), "", workspace,
                comments);
    }

    private static RulesProject publishedProject(String name) {
        var project = mock(RulesProject.class);
        when(project.getName()).thenReturn(name);
        when(project.getDesignTags()).thenReturn(Map.of());
        when(project.getLocalTags()).thenReturn(Map.of());
        return project;
    }

    private static FileData folder(String name) {
        var folder = new FileData();
        folder.setName(name);
        return folder;
    }

    private static class TestProjectCreationService extends ProjectCreationService {

        private final UserWorkspace workspace;
        private final Comments comments;

        TestProjectCreationService(AclProjectsHelper aclProjectsHelper,
                                   RepositoryAclServiceProvider aclServiceProvider,
                                   TagTypeService tagTypeService,
                                   TagService tagService,
                                   PathFilter zipFilter,
                                   ZipCharsetDetector zipCharsetDetector,
                                   String openlHome,
                                   UserWorkspace workspace,
                                   Comments comments) {
            super(aclProjectsHelper, aclServiceProvider, tagTypeService, tagService, zipFilter, zipCharsetDetector,
                    openlHome);
            this.workspace = workspace;
            this.comments = comments;
        }

        @Override
        public UserWorkspace getUserWorkspace() {
            return workspace;
        }

        @Override
        protected Comments getCommentsService(String repoId) {
            return comments;
        }
    }
}
