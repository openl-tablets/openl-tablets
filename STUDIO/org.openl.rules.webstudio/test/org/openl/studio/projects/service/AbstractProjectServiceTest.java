package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.ProjectInclude;
import org.openl.studio.projects.model.project.status.CompileState;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;

class AbstractProjectServiceTest {

    private RepositoryAclService designRepositoryAclService;
    private ProjectIdentifierMapper projectIdentifierMapper;
    private ProjectAccessService projectAccessService;
    private TestProjectService service;

    @BeforeEach
    void setUp() {
        designRepositoryAclService = mock(RepositoryAclService.class);
        projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        projectAccessService = mock(ProjectAccessService.class);
        service = new TestProjectService(designRepositoryAclService, projectIdentifierMapper, projectAccessService);
    }

    @Test
    void getProjectsFallsBackToEmptyCapabilitiesWhenCapabilityComputationFails() {
        var project = project("Project");
        service.projects = List.of(project);
        when(designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        when(projectIdentifierMapper.map(project)).thenReturn(ProjectIdModel.builder()
                .repository("design")
                .projectName("Project")
                .build());
        when(projectAccessService.computeCapabilities(project)).thenThrow(new IllegalStateException("Repository failed"));

        var response = service.getProjects(ProjectCriteriaQuery.builder().build(), Pageable.unpaged());

        assertEquals(1, response.getNumberOfElements());
        var view = response.getContent().iterator().next();
        assertNotNull(view.capabilities);
        assertNotNull(view.capabilities.project());
        assertNull(view.capabilities.project().canWrite());
    }

    @Test
    void getProjectsOmitsMetadataAndLockInfoWhenRepositoryReadsFail() {
        var project = project("Project");
        service.projects = List.of(project);
        grantRead(project);
        stubProjectId(project);
        when(project.getFileData()).thenThrow(new IllegalStateException("Metadata failed"));
        when(project.isOpenedForEditing()).thenReturn(false);
        when(project.getLockInfo()).thenThrow(new IllegalStateException("Lock failed"));

        var response = service.getProjects(ProjectCriteriaQuery.builder().build(), Pageable.unpaged());

        assertEquals(1, response.getNumberOfElements());
        var view = response.getContent().iterator().next();
        assertEquals("Project", view.name);
        assertNull(view.modifiedBy);
        assertNull(view.modifiedAt);
        assertNull(view.revision);
        assertNull(view.comment);
        assertNull(view.lockInfo);
    }

    @Test
    void getProjectsReturnsRepositoryAndTagCountsWhenSummaryRequested() {
        var payroll = rulesProject("Payroll", "design", "Design", Map.of("Category", "Payroll"));
        var benefits = rulesProject("Benefits", "ro", "ReadOnly", Map.of("Category", "Benefits"));
        service.projects = List.of(payroll, benefits);
        grantRead(payroll);
        grantRead(benefits);
        stubProjectId(payroll);
        stubProjectId(benefits);

        var response = service.getProjects(ProjectCriteriaQuery.builder()
                .include(ProjectInclude.SUMMARY)
                .build(), Pageable.unpaged());

        assertEquals(List.of("design", "ro"), response.getRepositoryCounts().stream()
                .map(count -> count.id())
                .sorted()
                .toList());
        assertEquals(List.of("Benefits", "Payroll"), response.getTagCounts().get(0).values().stream()
                .map(count -> count.id())
                .sorted()
                .toList());
    }

    @Test
    void getProjectsSummaryCountsIgnoreSelectedFacetValues() {
        var payroll = rulesProject("Payroll", "design", "Design", Map.of("Category", "Payroll"));
        var benefits = rulesProject("Benefits", "ro", "ReadOnly", Map.of("Category", "Benefits"));
        service.projects = List.of(payroll, benefits);
        service.projects.forEach(project -> {
            grantRead(project);
            stubProjectId(project);
        });

        var response = service.getProjects(ProjectCriteriaQuery.builder()
                .repositoryId("design")
                .tagValues(Map.of("Category", Set.of("Payroll")))
                .include(ProjectInclude.SUMMARY)
                .build(), Pageable.unpaged());

        assertEquals(List.of("Payroll"), response.getContent().stream()
                .map(project -> project.name)
                .toList());
        assertEquals(List.of("design", "ro"), response.getRepositoryCounts().stream()
                .map(count -> count.id())
                .sorted()
                .toList());
        assertEquals(List.of("Benefits", "Payroll"), response.getTagCounts().get(0).values().stream()
                .map(count -> count.id())
                .sorted()
                .toList());
    }

    @Test
    void getProjectsFiltersByAuthorAndBranch() {
        var alpha = rulesProject("Alpha", "design", "Design", Map.of());
        alpha.getFileData().setAuthor(new UserInfo("jane"));
        when(alpha.getBranch()).thenReturn("feature");
        var beta = rulesProject("Beta", "design", "Design", Map.of());
        beta.getFileData().setAuthor(new UserInfo("john"));
        when(beta.getBranch()).thenReturn("main");
        service.projects = List.of(alpha, beta);
        service.projects.forEach(project -> {
            grantRead(project);
            stubProjectId(project);
        });

        var byAuthor = service.getProjects(ProjectCriteriaQuery.builder().author("JANE").build(), Pageable.unpaged());
        assertEquals(List.of("Alpha"), byAuthor.getContent().stream().map(project -> project.name).toList());

        var byBranch = service.getProjects(ProjectCriteriaQuery.builder().branch("main").build(), Pageable.unpaged());
        assertEquals(List.of("Beta"), byBranch.getContent().stream().map(project -> project.name).toList());
    }

    @Test
    void getProjectsMatchesAnyTagValueWithinTypeAndAllSelectedTypes() {
        var payrollUs = rulesProject("Payroll US", "design", "Design", Map.of("Category", "Payroll", "Region", "US"));
        var benefitsUs = rulesProject("Benefits US", "design", "Design", Map.of("Category", "Benefits", "Region", "US"));
        var payrollEu = rulesProject("Payroll EU", "design", "Design", Map.of("Category", "Payroll", "Region", "EU"));
        service.projects = List.of(payrollUs, benefitsUs, payrollEu);
        service.projects.forEach(project -> {
            grantRead(project);
            stubProjectId(project);
        });

        var response = service.getProjects(ProjectCriteriaQuery.builder()
                .tagValues(Map.of("Category", Set.of("Payroll", "Benefits"), "Region", Set.of("US")))
                .build(), Pageable.unpaged());

        assertEquals(List.of("Benefits US", "Payroll US"), response.getContent().stream()
                .map(project -> project.name)
                .sorted()
                .toList());
    }

    @Test
    void getProjectsIncludesCompilationStatusesForReturnedPageOnly() {
        var alpha = project("Alpha");
        var beta = project("Beta");
        service.projects = List.of(alpha, beta);
        service.projects.forEach(project -> {
            grantRead(project);
            stubProjectId(project);
        });

        var response = service.getProjects(
                ProjectCriteriaQuery.builder()
                        .include(ProjectInclude.STATUS)
                        .build(),
                Page.of(1, 1));

        assertEquals(List.of("Beta"), service.statusedNames);
        assertEquals(1, response.getStatuses().size());
        assertEquals("Beta", response.getStatuses().get(0).projectId().getProjectName());
        assertEquals(CompileState.IDLE, response.getStatuses().get(0).compileState());
    }

    @Test
    void getProjectsSortsByUpdatedWithOneMetadataReadBeforeMapping() {
        var older = project("Older", new Date(1_000));
        var newer = project("Newer", new Date(2_000));
        service.projects = List.of(older, newer);
        service.projects.forEach(project -> {
            grantRead(project);
            stubProjectId(project);
        });

        var response = service.getProjects(ProjectCriteriaQuery.builder().sort("updated").build(), Pageable.unpaged());

        assertEquals(List.of("Newer", "Older"), response.getContent().stream()
                .map(project -> project.name)
                .toList());
        verify(older, times(2)).getFileData();
        verify(newer, times(2)).getFileData();
    }

    @Test
    void getProjectsReusesResolvedStatusForRowMapping() {
        var project = project("Project", "design", "Design", mock(UserWorkspaceProject.class));
        service.projects = List.of(project);
        grantRead(project);
        stubProjectId(project);
        when(project.getStatus()).thenReturn(ProjectStatus.VIEWING);

        var response = service.getProjects(ProjectCriteriaQuery.builder()
                .include(ProjectInclude.SUMMARY)
                .build(), Pageable.unpaged());

        assertEquals(ProjectStatus.VIEWING, response.getContent().iterator().next().status);
        verify(project, times(1)).getStatus();
    }

    private void grantRead(AProject project) {
        when(designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
    }

    private void stubProjectId(AProject project) {
        var repositoryId = project.getRepository().getId();
        var projectName = project.getBusinessName();
        when(projectIdentifierMapper.map(project)).thenReturn(ProjectIdModel.builder()
                .repository(repositoryId)
                .projectName(projectName)
                .build());
    }

    private AProject project(String name) {
        return project(name, null);
    }

    private AProject project(String name, Date modifiedAt) {
        return project(name, "design", "Design", mock(AProject.class), modifiedAt);
    }

    private RulesProject rulesProject(String name, String repositoryId, String repositoryName, Map<String, String> tags) {
        var project = project(name, repositoryId, repositoryName, mock(RulesProject.class), null);
        when(project.getLocalTags()).thenReturn(tags);
        return project;
    }

    private <T extends AProject> T project(String name, String repositoryId, String repositoryName, T project) {
        return project(name, repositoryId, repositoryName, project, null);
    }

    private <T extends AProject> T project(String name, String repositoryId, String repositoryName, T project,
                                           Date modifiedAt) {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn(repositoryId);
        when(repository.getName()).thenReturn(repositoryName);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());

        var fileData = new FileData();
        fileData.setName(name);
        fileData.setModifiedAt(modifiedAt);

        when(project.getBusinessName()).thenReturn(name);
        when(project.getRepository()).thenReturn(repository);
        when(project.getFileData()).thenReturn(fileData);
        if (project instanceof UserWorkspaceProject workspaceProject) {
            when(workspaceProject.getDesignRepository()).thenReturn(repository);
        }
        return project;
    }

    private static final class TestProjectService extends AbstractProjectService<AProject> {

        private List<AProject> projects = List.of();
        private List<String> statusedNames = List.of();

        private TestProjectService(RepositoryAclService designRepositoryAclService,
                                   ProjectIdentifierMapper projectIdentifierMapper,
                                   ProjectAccessService projectAccessService) {
            super(designRepositoryAclService, projectIdentifierMapper, projectAccessService);
        }

        @Override
        protected Stream<AProject> getProjects0(ProjectCriteriaQuery query) {
            return projects.stream();
        }

        @Override
        protected Predicate<AProject> buildFilterCriteria(ProjectCriteriaQuery query) {
            var filter = super.buildFilterCriteria(query);
            if (query.hasRepositoryFilter()) {
                filter = filter.and(project -> query.repositoryIds().contains(project.getRepository().getId()));
            }
            return filter;
        }

        @Override
        protected Optional<ProjectStatus> statusOf(AProject project) {
            return project instanceof UserWorkspaceProject workspaceProject
                    ? Optional.ofNullable(workspaceProject.getStatus())
                    : Optional.empty();
        }

        @Override
        protected List<ProjectStatusViewModel> projectStatuses(List<? extends AProject> pageProjects) {
            statusedNames = pageProjects.stream()
                    .map(AProject::getBusinessName)
                    .toList();
            return pageProjects.stream()
                    .map(project -> ProjectStatusViewModel.builder()
                            .projectId(ProjectIdModel.builder()
                                    .repository(project.getRepository().getId())
                                    .projectName(project.getBusinessName())
                                    .build())
                            .compileState(CompileState.IDLE)
                            .build())
                    .toList();
        }
    }
}
