package org.openl.studio.projects.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

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
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
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
public class ProjectIdentityConverterTest {

    private static final String ID_SEPARATOR = ":";

    @Autowired
    private ProjectIdentityConverter projectConverter;

    @Autowired
    private RepositoryAclService designRepositoryAclService;

    @Autowired
    private UserWorkspace userWorkspace;

    @BeforeEach
    public void setUp() {
        reset(designRepositoryAclService, userWorkspace);
    }

    @Test
    public void test_convert() throws ProjectException {
        String repoId = "qwerty";
        String projectName = "projectName";
        String projectId = encode(repoId, projectName);

        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProject(repoId, projectName)).thenReturn(rulesProject);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(rulesProject, projectConverter.convert(projectId));
        verify(designRepositoryAclService).isGranted(rulesProject, List.of(BasePermission.READ));
    }

    @Test
    public void test_convert_securityError() throws ProjectException {
        String repoId = "qwerty";
        String projectName = "projectName";
        String projectId = encode(repoId, projectName);

        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProject(repoId, projectName)).thenReturn(rulesProject);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(false);

        assertThrows(SecurityException.class, () -> projectConverter.convert(projectId));
    }

    @Test
    public void test_convert_invalidId_fallsBackToNameLookup() throws ProjectException {
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

    @Test
    public void test_convert_notFound() throws ProjectException {
        String repositoryId = "design-repo";
        String projectName = "projectName";
        String projectId = encode(repositoryId, projectName);

        when(userWorkspace.getProject(repositoryId, projectName)).thenThrow(new ProjectException("Not found"));
        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        var repo = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        when(designRepo.getRepository(repositoryId)).thenReturn(repo);
        when(repo.supports()).thenReturn(new FeaturesBuilder(repo).setMappedFolders(true).build());
        when(((FolderMapper) repo).getBusinessName(projectName)).thenReturn(projectName);
        when(designRepo.getRepositories()).thenReturn(List.of());
        when(userWorkspace.getProjectsByName(projectName)).thenReturn(List.of());

        var ex = assertThrows(NotFoundException.class, () -> projectConverter.convert(projectId));
        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
    }

    @Test
    public void test_convert_mappedName() throws ProjectException {
        String repoId = "design-repo";
        String projectBusinessName = "projectName";
        String projectMappedName = projectBusinessName + ":123456789";
        String projectId = encode(repoId, projectMappedName);

        when(userWorkspace.getProject(repoId, projectMappedName)).thenThrow(new ProjectException("Not found"));
        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        var repo = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        when(designRepo.getRepository(repoId)).thenReturn(repo);
        when(repo.supports()).thenReturn(new FeaturesBuilder(repo).setMappedFolders(true).build());
        when(((FolderMapper) repo).getBusinessName(projectMappedName)).thenReturn(projectBusinessName);

        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProject(repoId, projectBusinessName)).thenReturn(rulesProject);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(rulesProject, projectConverter.convert(projectId));
        verify(designRepositoryAclService).isGranted(rulesProject, List.of(BasePermission.READ));
    }

    @Test
    public void test_convert_mappedName_fallsBackToNameLookup() throws ProjectException {
        String repoId = "design-repo";
        String projectBusinessName = "projectName";
        String projectMappedName = projectBusinessName + ":123456789";
        String projectId = encode(repoId, projectMappedName);

        when(userWorkspace.getProject(repoId, projectMappedName)).thenThrow(new ProjectException("Not found"));
        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        var repo = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        when(designRepo.getRepository(repoId)).thenReturn(repo);
        when(repo.supports()).thenReturn(new FeaturesBuilder(repo).setMappedFolders(true).build());
        when(((FolderMapper) repo).getBusinessName(projectMappedName)).thenReturn(projectBusinessName);
        when(userWorkspace.getProject(repoId, projectBusinessName)).thenThrow(new ProjectException("Not found 2"));
        when(designRepo.getRepositories()).thenReturn(List.of());
        when(userWorkspace.getProjectsByName(projectMappedName)).thenReturn(List.of());

        var ex = assertThrows(NotFoundException.class, () -> projectConverter.convert(projectId));
        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
    }

    @Test
    public void test_convert_byName_singleMatch() throws ProjectException {
        String name = "MyProject";
        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of(rulesProject));
        when(designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))).thenReturn(true);

        assertSame(rulesProject, projectConverter.convert(name));
        verify(designRepositoryAclService).isGranted(rulesProject, List.of(BasePermission.READ));
    }

    @Test
    public void test_convert_byName_ambiguous() throws ProjectException {
        String name = "MyProject";
        var p1 = mock(RulesProject.class);
        var p2 = mock(RulesProject.class);
        var repo1 = mock(Repository.class);
        var repo2 = mock(Repository.class);
        when(repo1.getId()).thenReturn("repo-1");
        when(repo2.getId()).thenReturn("repo-2");
        when(p1.getRepository()).thenReturn(repo1);
        when(p2.getRepository()).thenReturn(repo2);
        when(p1.getName()).thenReturn(name);
        when(p2.getName()).thenReturn(name);
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of(p1, p2));

        var ex = assertThrows(ConflictException.class, () -> projectConverter.convert(name));
        assertEquals("openl.error.409.project.identifier.ambiguous.message", ex.getErrorCode());
        assertEquals(name, ex.getArgs()[0]);
        var encoded1 = encode("repo-1", name);
        var encoded2 = encode("repo-2", name);
        assertEquals(encoded1 + ", " + encoded2, ex.getArgs()[1]);
    }

    @Test
    public void test_convert_byName_fallbackToRepoScan() throws ProjectException {
        String name = "MyProject";
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
    public void test_convert_byName_fallbackToRepoScan_ambiguous() throws ProjectException {
        String name = "MyProject";
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
    public void test_convert_byName_fallbackToRepoScan_mappedFolders() throws ProjectException {
        String businessName = "MyProject";
        String mappedName = businessName + ":hash";
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
    public void test_convert_byName_notFound() throws ProjectException {
        String name = "MyProject";
        when(userWorkspace.getProjectsByName(name)).thenReturn(List.of());

        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        when(designRepo.getRepositories()).thenReturn(List.of());

        var ex = assertThrows(NotFoundException.class, () -> projectConverter.convert(name));
        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
    }

    private String encode(String repoId, String projectName) {
        var projectIdentifier = repoId + ID_SEPARATOR + projectName;
        return Base64.getEncoder().encodeToString(projectIdentifier.getBytes(StandardCharsets.UTF_8));
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public ProjectIdentityConverter projectConverter(RepositoryAclService designRepositoryAclService,
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
        public Base64ProjectResolveStrategy base64ProjectResolveStrategy() {
            return new Base64ProjectResolveStrategy();
        }

        @Bean
        public ProjectNameResolveStrategy projectNameResolveStrategy() {
            return new ProjectNameResolveStrategy();
        }

        @Bean
        public ProjectIdentifierMapper projectIdentifierMapper() {
            return new ProjectIdentifierMapperImpl();
        }

        @Bean
        public RepositoryAclService designRepositoryAclService() {
            return mock(RepositoryAclService.class);
        }

        @Bean
        public UserWorkspace userWorkspace() {
            return mock(UserWorkspace.class);
        }
    }
}
