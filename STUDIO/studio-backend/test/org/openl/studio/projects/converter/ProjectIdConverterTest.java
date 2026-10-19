package org.openl.studio.projects.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.impl.local.ProjectMetainfo;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.ProjectIds;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.NotFoundException;

class ProjectIdConverterTest {

    private static final String REPOSITORY_ID = "design";
    private static final String BUSINESS_NAME = "Bank";
    private static final ProjectId ID = ProjectIds.of(REPOSITORY_ID, "rules/Bank");
    private static final String ID_TEXT = ID.value();

    private final RepositoryAclService aclService = mock(RepositoryAclService.class);
    private final UserWorkspace workspace = mock(UserWorkspace.class);
    private final DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);
    private final MetainfoRegistry metainfoRegistry = mock(MetainfoRegistry.class);
    private final ProjectIdConverter converter = new ProjectIdConverter(aclService) {
        @Override
        public UserWorkspace getUserWorkspace() {
            return workspace;
        }
    };

    @BeforeEach
    void setUp() {
        var localWorkspace = mock(LocalWorkspace.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        when(workspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(localWorkspace.getMetainfoRegistry()).thenReturn(metainfoRegistry);
        when(workspace.getProjectById(any(), anyBoolean())).thenReturn(Optional.empty());
    }

    @Test
    void aListedProjectIsReusedWhileTheRepositoryStillHoldsIt() {
        var project = closedProject();
        listed(project);
        when(designTimeRepository.getProjectById(ID)).thenReturn(Optional.of(mock(AProject.class)));
        readable(project);

        assertSame(project, converter.convert(ID.value()));
        verify(workspace, never()).getProjectById(ID, true);
    }

    @Test
    void aProjectTheRepositoryNoLongerHoldsIsListedAgain() {
        var stale = closedProject();
        var current = closedProject();
        listed(stale);
        when(workspace.getProjectById(ID, true)).thenReturn(Optional.of(current));
        readable(current);

        assertSame(current, converter.convert(ID.value()));
    }

    @Test
    void aProjectOpenedSinceTheListingIsListedAgain() {
        var stale = closedProject();
        var current = closedProject();
        listed(stale);
        when(designTimeRepository.getProjectById(ID)).thenReturn(Optional.of(mock(AProject.class)));
        when(metainfoRegistry.get(BUSINESS_NAME)).thenReturn(
                new ProjectMetainfo(REPOSITORY_ID, "rules/Bank", null, null, null, null, null, null, Map.of()));
        when(workspace.getProjectById(ID, true)).thenReturn(Optional.of(current));
        readable(current);

        assertSame(current, converter.convert(ID.value()));
    }

    @Test
    void aLocalProjectIsReusedAsListed() {
        var project = mock(RulesProject.class);
        when(project.isLocalOnly()).thenReturn(true);
        listed(project);
        readable(project);

        assertSame(project, converter.resolve(ID));
        verify(designTimeRepository, never()).getProjectById(any());
    }

    @Test
    void anIdNoProjectHasAnswersNotFound() {
        var ex = assertThrows(NotFoundException.class, () -> converter.convert(ID_TEXT));

        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
        verify(workspace).getProjectById(ID, true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ZGVzaWduOkJhbms=", "ZGVzaWduOlByb2plY3Q6OTg1OTU5", "Bank", "Bank Rating", ""})
    void anOldIdOrANameNamesNoProjectAndListsNothingAgain(String text) {
        var ex = assertThrows(NotFoundException.class, () -> converter.convert(text));

        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
        verify(workspace, never()).getProjectById(any(), anyBoolean());
    }

    @Test
    void aProjectTheUserMayNotReadIsRefused() {
        var project = closedProject();
        listed(project);
        when(designTimeRepository.getProjectById(ID)).thenReturn(Optional.of(mock(AProject.class)));

        assertThrows(SecurityException.class, () -> converter.convert(ID_TEXT));
    }

    @Test
    void findAnswersNothingForAnUnknownId() {
        assertNull(converter.find(ID));
    }

    private void listed(RulesProject project) {
        when(workspace.getProjectById(ID, false)).thenReturn(Optional.of(project));
    }

    private void readable(RulesProject project) {
        when(aclService.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
    }

    private static RulesProject closedProject() {
        var project = mock(RulesProject.class);
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn(REPOSITORY_ID);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getBusinessName()).thenReturn(BUSINESS_NAME);
        return project;
    }
}
