package org.openl.studio.projects.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.impl.local.ProjectMetainfo;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectIdentifierMapperImpl;

/**
 * @author Vladyslav Pikus
 */
@SpringJUnitConfig(classes = ProjectIdentityConverterTest.TestConfig.class)
class ProjectIdentityConverterTest {

    private static final String ID_SEPARATOR = ":";
    private static final String RULES_LOCATION = "DESIGN/rules/";

    @Autowired
    private ProjectIdentityConverter projectConverter;

    @Autowired
    private RepositoryAclService designRepositoryAclService;

    @Autowired
    private UserWorkspace userWorkspace;

    @BeforeEach
    void setUp() {
        reset(designRepositoryAclService, userWorkspace);
    }

    @Test
    void convert_byId() throws ProjectException {
        var repoId = "qwerty";
        var projectName = "projectName";
        var projectId = encode(repoId, projectName);

        var rulesProject = mock(RulesProject.class);
        var designRepository = mock(DesignTimeRepository.class);
        var localWorkspace = mock(LocalWorkspace.class);
        var metainfoRegistry = mock(MetainfoRegistry.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepository);
        when(userWorkspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(localWorkspace.getMetainfoRegistry()).thenReturn(metainfoRegistry);
        when(userWorkspace.getProject(repoId, projectName, false)).thenReturn(rulesProject);
        when(userWorkspace.getProject(repoId, projectName)).thenReturn(rulesProject);
        when(rulesProject.getDesignProjectName()).thenReturn(projectName);
        when(rulesProject.getBusinessName()).thenReturn(projectName);
        when(designRepository.hasProject(repoId, projectName)).thenReturn(true);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(rulesProject, projectConverter.convert(projectId));
        verify(userWorkspace, never()).getProject(repoId, projectName);
        verify(designRepositoryAclService).isGranted(rulesProject, List.of(BasePermission.READ));
    }

    @Test
    void convert_byId_refreshes_a_stale_cached_project() throws ProjectException {
        var repoId = "qwerty";
        var projectName = "projectName";
        var projectId = encode(repoId, projectName);
        var staleProject = mock(RulesProject.class);
        var currentProject = mock(RulesProject.class);
        var designRepository = mock(DesignTimeRepository.class);
        var localWorkspace = mock(LocalWorkspace.class);
        var metainfoRegistry = mock(MetainfoRegistry.class);

        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepository);
        when(userWorkspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(localWorkspace.getMetainfoRegistry()).thenReturn(metainfoRegistry);
        when(userWorkspace.getProject(repoId, projectName, false)).thenReturn(staleProject);
        when(staleProject.getDesignProjectName()).thenReturn(projectName);
        when(designRepository.hasProject(repoId, projectName)).thenReturn(false);
        when(userWorkspace.getProject(repoId, projectName)).thenReturn(currentProject);
        when(designRepositoryAclService.isGranted(currentProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(currentProject, projectConverter.convert(projectId));
        verify(userWorkspace).getProject(repoId, projectName);
    }

    @Test
    void convert_byId_refreshes_when_the_local_copy_state_changed() throws ProjectException {
        var repoId = "qwerty";
        var projectName = "projectName";
        var projectId = encode(repoId, projectName);
        var staleProject = mock(RulesProject.class);
        var currentProject = mock(RulesProject.class);
        var designRepository = mock(DesignTimeRepository.class);
        var localWorkspace = mock(LocalWorkspace.class);
        var metainfoRegistry = mock(MetainfoRegistry.class);

        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepository);
        when(userWorkspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(localWorkspace.getMetainfoRegistry()).thenReturn(metainfoRegistry);
        when(userWorkspace.getProject(repoId, projectName, false)).thenReturn(staleProject);
        when(staleProject.getDesignProjectName()).thenReturn(projectName);
        when(staleProject.getBusinessName()).thenReturn(projectName);
        when(designRepository.hasProject(repoId, projectName)).thenReturn(true);
        when(metainfoRegistry.get(projectName)).thenReturn(new ProjectMetainfo(
                repoId, projectName, "master", "1", "admin", 1L, 1L, null, Map.of()));
        when(userWorkspace.getProject(repoId, projectName)).thenReturn(currentProject);
        when(designRepositoryAclService.isGranted(currentProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(currentProject, projectConverter.convert(projectId));
        verify(userWorkspace).getProject(repoId, projectName);
    }

    @Test
    void convert_securityError() throws ProjectException {
        var repoId = "qwerty";
        var projectName = "projectName";
        var projectId = encode(repoId, projectName);

        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProject(repoId, projectName)).thenReturn(rulesProject);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(false);

        assertThrows(SecurityException.class, () -> projectConverter.convert(projectId));
    }

    @Test
    void convert_invalidId_fallsBackToNameLookup() {
        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        when(designRepo.getRepositories()).thenReturn(List.of());

        when(userWorkspace.getProjectsByName("fooBar:asasas")).thenReturn(List.of());
        var ex1 = assertThrows(NotFoundException.class, () -> projectConverter.convert("fooBar:asasas"));
        assertEquals("openl.error.404.project.identifier.message", ex1.getErrorCode());

        var rawName = Base64.getEncoder().encodeToString("fooBar".getBytes(StandardCharsets.UTF_8));
        when(userWorkspace.getProjectsByName(rawName)).thenReturn(List.of());
        var ex2 = assertThrows(NotFoundException.class, () -> projectConverter.convert(rawName));
        assertEquals("openl.error.404.project.identifier.message", ex2.getErrorCode());
    }

    /**
     * Stubs a design repository that maps folders: the id's mapped name resolves back to the business name and to
     * the folder the design repository holds.
     */
    private DesignTimeRepository mockMappedRepository(String repoId,
                                                      String mappedName,
                                                      String businessName,
                                                      String realPath) {
        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        when(designRepo.getRulesLocation()).thenReturn(RULES_LOCATION);
        var repo = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        when(designRepo.getRepository(repoId)).thenReturn(repo);
        when(repo.supports()).thenReturn(new FeaturesBuilder(repo).setMappedFolders(true).build());
        when(((FolderMapper) repo).getBusinessName(mappedName)).thenReturn(businessName);
        when(((FolderMapper) repo).getRealPath(RULES_LOCATION + mappedName)).thenReturn(realPath);
        return designRepo;
    }

    @Test
    void convert_notFound() throws ProjectException {
        var repositoryId = "design-repo";
        var projectName = "projectName";
        var projectId = encode(repositoryId, projectName);

        when(userWorkspace.getProject(repositoryId, projectName)).thenThrow(new ProjectException("Not found"));
        var designRepo = mockMappedRepository(repositoryId, projectName, projectName, projectName);
        when(designRepo.getRepositories()).thenReturn(List.of());
        when(userWorkspace.getProjectsByName(projectName)).thenReturn(List.of());

        var ex = assertThrows(NotFoundException.class, () -> projectConverter.convert(projectId));
        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
    }

    @Test
    void convert_mappedName() throws ProjectException {
        var repoId = "design-repo";
        var projectBusinessName = "projectName";
        var projectMappedName = projectBusinessName + ":123456789";
        var projectId = encode(repoId, projectMappedName);

        when(userWorkspace.getProject(repoId, projectMappedName)).thenThrow(new ProjectException("Not found"));
        mockMappedRepository(repoId, projectMappedName, projectBusinessName, "path/to/projectName");

        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProject(repoId, projectBusinessName)).thenReturn(rulesProject);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(rulesProject, projectConverter.convert(projectId));
        verify(designRepositoryAclService).isGranted(rulesProject, List.of(BasePermission.READ));
    }

    @Test
    void convert_mappedName_fallsBackToNameLookup() throws ProjectException {
        var repoId = "design-repo";
        var projectBusinessName = "projectName";
        var projectMappedName = projectBusinessName + ":123456789";
        var projectId = encode(repoId, projectMappedName);

        when(userWorkspace.getProject(repoId, projectMappedName)).thenThrow(new ProjectException("Not found"));
        var designRepo = mockMappedRepository(repoId, projectMappedName, projectBusinessName, "path/to/projectName");
        when(userWorkspace.getProject(repoId, projectBusinessName)).thenThrow(new ProjectException("Not found 2"));
        when(designRepo.getRepositories()).thenReturn(List.of());
        when(userWorkspace.getProjectsByName(projectMappedName)).thenReturn(List.of());

        var ex = assertThrows(NotFoundException.class, () -> projectConverter.convert(projectId));
        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
    }

    @Test
    void convert_mappedName_unsavedRename_resolvesByDesignFolder() throws ProjectException {
        var repoId = "design-repo";
        var projectBusinessName = "projectName";
        var projectMappedName = projectBusinessName + ":123456789";
        var projectRealPath = "path/to/projectName";
        var projectId = encode(repoId, projectMappedName);

        when(userWorkspace.getProject(repoId, projectMappedName)).thenThrow(new ProjectException("Not found"));
        mockMappedRepository(repoId, projectMappedName, projectBusinessName, projectRealPath);

        // The rename in rules.xml is not saved yet: the workspace copy left the business-name folder, and another
        // project of that business name occupies it now. The folder the id names must win.
        var renamedProject = mock(RulesProject.class);
        when(userWorkspace.getProjectByPath(repoId, projectRealPath)).thenReturn(Optional.of(renamedProject));
        when(userWorkspace.getProject(repoId, projectBusinessName)).thenReturn(mock(RulesProject.class));
        when(designRepositoryAclService.isGranted(renamedProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(renamedProject, projectConverter.convert(projectId));
        verify(userWorkspace, never()).getProject(repoId, projectBusinessName);
        verify(userWorkspace, never()).getProject(repoId, projectBusinessName, false);
    }

    @Test
    void convert_byName_singleMatch() {
        var name = "MyProject";
        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of(rulesProject));
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(rulesProject, projectConverter.convert(name));
        verify(designRepositoryAclService).isGranted(rulesProject, List.of(BasePermission.READ));
    }

    @Test
    void convert_byName_ambiguous() {
        var name = "MyProject";
        var p1 = projectIn("repo-1", name);
        var p2 = projectIn("repo-2", name);
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of(p1, p2));

        var ex = assertThrows(ConflictException.class, () -> projectConverter.convert(name));
        assertEquals("openl.error.409.project.identifier.ambiguous.message", ex.getErrorCode());
        assertEquals(name, ex.getArgs()[0]);
        var encoded1 = encode("repo-1", name);
        var encoded2 = encode("repo-2", name);
        assertEquals(encoded1 + ", " + encoded2, ex.getArgs()[1]);
    }

    @Test
    void resolve_byName_narrowedToOneRepository() {
        var name = "MyProject";
        var wanted = projectIn("repo-1", name);
        var sameNameElsewhere = projectIn("repo-2", name);
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of(wanted, sameNameElsewhere));

        assertSame(wanted, projectConverter.resolveProjectIdentity(name, "repo-1"));
    }

    @Test
    void resolve_byName_narrowingCannotSettleOneRepositoryHoldingTheNameTwice() {
        // A non-flat repository tells its projects apart by the folder they live in, so it may carry one name
        // in several folders. Only an id picks one of those; the answer names them.
        var name = "MyProject";
        var first = projectIn("repo-1", name);
        var second = projectIn("repo-1", name);
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of(first, second));

        var ex = assertThrows(ConflictException.class,
                () -> projectConverter.resolveProjectIdentity(name, "repo-1"));
        assertEquals("openl.error.409.project.identifier.ambiguous.message", ex.getErrorCode());
    }

    @Test
    void resolve_isNotHandedOnWhenTheAnsweringStrategyNamesAnotherRepository() {
        // The strategy that answers stays authoritative: the identity names no project of this repository, so
        // the next strategy must not be asked to read it as something else.
        var name = "MyProject";
        var elsewhere = projectIn("repo-2", name);
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of(elsewhere));

        assertNull(projectConverter.resolveProjectIdentity(name, "repo-1"));
        verify(userWorkspace, never()).getDesignTimeRepository();
    }

    @Test
    void convert_byName_fallbackToRepoScan() throws ProjectException {
        var name = "MyProject";
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of());

        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        var repo1 = mock(Repository.class);
        var repo2 = mock(Repository.class);
        when(repo1.getId()).thenReturn("repo-1");
        when(repo2.getId()).thenReturn("repo-2");
        when(repo1.supports()).thenReturn(new FeaturesBuilder(repo1).build());
        when(repo2.supports()).thenReturn(new FeaturesBuilder(repo2).build());
        when(designRepo.getRepositories()).thenReturn(List.of(repo1, repo2));

        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProject("repo-1", name)).thenThrow(new ProjectException("Not found"));
        when(userWorkspace.getProject("repo-2", name)).thenReturn(rulesProject);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(rulesProject, projectConverter.convert(name));
    }

    @Test
    void convert_byName_fallbackToRepoScan_ambiguous() throws ProjectException {
        var name = "MyProject";
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of());

        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        var repo1 = mock(Repository.class);
        var repo2 = mock(Repository.class);
        lenient().when(repo1.getId()).thenReturn("repo-1");
        lenient().when(repo2.getId()).thenReturn("repo-2");
        lenient().when(repo1.supports()).thenReturn(new FeaturesBuilder(repo1).build());
        lenient().when(repo2.supports()).thenReturn(new FeaturesBuilder(repo2).build());
        when(designRepo.getRepositories()).thenReturn(List.of(repo1, repo2));

        var p1 = mock(RulesProject.class);
        var p2 = mock(RulesProject.class);
        when(p1.getRepository()).thenReturn(repo1);
        when(p2.getRepository()).thenReturn(repo2);
        when(p1.getName()).thenReturn(name);
        when(p2.getName()).thenReturn(name);
        when(userWorkspace.getProject("repo-1", name)).thenReturn(p1);
        when(userWorkspace.getProject("repo-2", name)).thenReturn(p2);

        var ex = assertThrows(ConflictException.class, () -> projectConverter.convert(name));
        assertEquals("openl.error.409.project.identifier.ambiguous.message", ex.getErrorCode());
    }

    @Test
    void convert_byName_fallbackToRepoScan_mappedFolders() throws ProjectException {
        var businessName = "MyProject";
        var mappedName = businessName + ":hash";
        when(userWorkspace.getProjectsByName(businessName)).thenReturn(List.of());

        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        var repo = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        when(repo.getId()).thenReturn("design-repo");
        when(repo.supports()).thenReturn(new FeaturesBuilder(repo).setMappedFolders(true).build());
        when(designRepo.getRepositories()).thenReturn(List.of(repo));

        when(userWorkspace.getProject("design-repo", businessName)).thenThrow(new ProjectException("Not found"));

        var aproj = mock(AProject.class);
        when(aproj.getBusinessName()).thenReturn(businessName);
        when(aproj.getName()).thenReturn(mappedName);
        doReturn(List.of(aproj)).when(designRepo).getProjects("design-repo");

        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProject("design-repo", mappedName)).thenReturn(rulesProject);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(rulesProject, projectConverter.convert(businessName));
    }

    @Test
    void convert_byName_notFound() {
        var name = "MyProject";
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of());

        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        when(designRepo.getRepositories()).thenReturn(List.of());

        var ex = assertThrows(NotFoundException.class, () -> projectConverter.convert(name));
        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
    }

    /** A workspace project of one repository, named as the workspace and the id mapper report it. */
    private static RulesProject projectIn(String repositoryId, String name) {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn(repositoryId);
        // The id mapper asks the design repository whether it maps folders before it names the project.
        lenient().when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        var project = mock(RulesProject.class);
        lenient().when(project.getRepository()).thenReturn(repository);
        when(project.getDesignRepository()).thenReturn(repository);
        lenient().when(project.getName()).thenReturn(name);
        return project;
    }

    private String encode(String repoId, String projectName) {
        var projectIdentifier = repoId + ID_SEPARATOR + projectName;
        return Base64.getEncoder().encodeToString(projectIdentifier.getBytes(StandardCharsets.UTF_8));
    }

    @Configuration
    static class TestConfig {

        @Bean
        ProjectIdentityConverter projectConverter(RepositoryAclService designRepositoryAclService,
                                                  List<ProjectResolveStrategy> strategies,
                                                  ProjectIdentifierMapper projectIdentifierMapper) {
            return new ProjectIdentityConverter(designRepositoryAclService, strategies, projectIdentifierMapper) {
                // just a workaround for @Lookup
                @Override
                public UserWorkspace getUserWorkspace() {
                    return userWorkspace();
                }
            };
        }

        @Bean
        Base64ProjectResolveStrategy base64ProjectResolveStrategy() {
            return new Base64ProjectResolveStrategy();
        }

        @Bean
        ProjectNameResolveStrategy projectNameResolveStrategy() {
            return new ProjectNameResolveStrategy();
        }

        @Bean
        ProjectIdentifierMapper projectIdentifierMapper() {
            return new ProjectIdentifierMapperImpl();
        }

        @Bean
        RepositoryAclService designRepositoryAclService() {
            return mock(RepositoryAclService.class);
        }

        @Bean
        UserWorkspace userWorkspace() {
            return mock(UserWorkspace.class);
        }
    }
}
