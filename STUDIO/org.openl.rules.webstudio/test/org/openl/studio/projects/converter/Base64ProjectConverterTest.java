package org.openl.studio.projects.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.NotFoundException;

/**
 * @author Vladyslav Pikus
 */
@SpringJUnitConfig(classes = Base64ProjectConverterTest.TestConfig.class)
public class Base64ProjectConverterTest {

    @Autowired
    private Base64ProjectConverter projectConverter;

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
    public void test_convert_incorrectId() {
        assertThrows(IllegalArgumentException.class, () -> projectConverter.convert("fooBar:asasas"));
        var encoder = Base64.getEncoder();
        var id = encoder.encodeToString("fooBar".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> projectConverter.convert(id));
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
    public void test_convert_mappedName_notFound() throws ProjectException {
        String repoId = "design-repo";
        String projectBusinessName = "projectName";
        String projectMappedName = projectBusinessName + ":123456789";
        String projectId = encode(repoId, projectMappedName);

        var ex1 = new ProjectException("Not found");
        when(userWorkspace.getProject(repoId, projectMappedName)).thenThrow(ex1);
        var designRepo = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designRepo);
        var repo = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        when(designRepo.getRepository(repoId)).thenReturn(repo);
        when(repo.supports()).thenReturn(new FeaturesBuilder(repo).setMappedFolders(true).build());
        when(((FolderMapper) repo).getBusinessName(projectMappedName)).thenReturn(projectBusinessName);
        var ex2 = new ProjectException("Not found 2");
        when(userWorkspace.getProject(repoId, projectBusinessName)).thenThrow(ex2);

        var ex = assertThrows(NotFoundException.class, () -> projectConverter.convert(projectId));
        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
        assertSame(ex1, ex.getCause());
        assertSame(ex2, ex.getCause().getSuppressed()[0]);
    }

    private String encode(String repoId, String projectName) {
        var projectIdentifier = repoId + Base64ProjectConverter.SEPARATOR + projectName;
        return Base64.getEncoder().encodeToString(projectIdentifier.getBytes(StandardCharsets.UTF_8));
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public Base64ProjectConverter projectConverter(RepositoryAclService designRepositoryAclService) {
            return new Base64ProjectConverter(designRepositoryAclService) {
                // just a wokraround for @Lookup
                @Override
                public UserWorkspace getUserWorkspace() {
                    return userWorkspace();
                }
            };
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
