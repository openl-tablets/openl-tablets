package org.openl.rules.webstudio.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.BranchedProject;
import org.openl.rules.workspace.dtr.BranchedProject.BranchEntry;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;

class SecureDesignTimeRepositoryImplTest {

    @Test
    void choosesAReadableHomeAndHidesDeniedBranchEntries() throws Exception {
        var main = project("main", "DESIGN/Denied/Rates");
        var feature = project("feature/rates", "DESIGN/Readable/Rates");
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("main", entry(main, Instant.parse("2026-07-29T09:00:00Z")));
        entries.put("feature/rates", entry(feature, Instant.parse("2026-07-29T10:00:00Z")));
        var branched = BranchedProject.create("Rates", "main", entries);

        var delegate = mock(DesignTimeRepository.class);
        when(delegate.getProjects()).thenAnswer(invocation -> List.of(main));
        when(delegate.getProjects("design")).thenAnswer(invocation -> List.of(main));
        when(delegate.getRepositories()).thenReturn(List.of(main.getRepository()));
        when(delegate.getBranchedProject("design", "Rates")).thenReturn(Optional.of(branched));
        when(delegate.hasProject("design", "Rates")).thenReturn(true);
        when(delegate.hasProjectInAnyBranch("design", "DeniedOnly")).thenReturn(true);
        var version = mock(CommonVersion.class);
        when(delegate.getProject("design", "Rates", version)).thenReturn(feature);
        when(delegate.getProjectByPath("design", "feature/rates", "DESIGN/Readable/Rates", "revision"))
                .thenReturn(feature);
        var health = new BranchedProjectIndexService.IndexHealth(
                BranchedProjectIndexService.IndexState.READY,
                Set.of(),
                null);
        when(delegate.getProjectIndexHealth("design")).thenReturn(Optional.of(health));
        var refresh = CompletableFuture.<Void>completedFuture(null);
        when(delegate.refreshBranch("design", "feature/rates")).thenReturn(refresh);
        var aclService = mock(RepositoryAclService.class);
        when(aclService.isGranted(any(AProject.class), anyList()))
                .thenAnswer(invocation -> ((AProject) invocation.getArgument(0)).getRealPath().contains("Readable"));
        when(aclService.isGranted("design", null, List.of(BasePermission.READ))).thenReturn(true);
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        var visible = secured.getBranchedProject("design", "Rates").orElseThrow();

        assertEquals("feature/rates", visible.homeBranch());
        assertEquals(List.of("feature/rates"), List.copyOf(visible.entries().keySet()));
        assertEquals("feature/rates",
                ((BranchRepository) secured.getProjects().iterator().next().getRepository()).getBranch());
        assertNotSame(feature.getRepository(), visible.homeEntry().project().getRepository());
        assertEquals(1, secured.getRepositories().size());
        assertEquals("feature/rates",
                ((BranchRepository) secured.getProject("design", "Rates").getRepository()).getBranch());
        assertEquals(1, secured.getProjects("design").size());
        assertEquals(1, secured.getManageableProjects().size());
        assertEquals("feature/rates",
                ((BranchRepository) secured.getProject("design", "Rates", version).getRepository()).getBranch());
        assertEquals("feature/rates",
                ((BranchRepository) secured
                        .getProjectByPath("design", "feature/rates", "DESIGN/Readable/Rates", "revision")
                        .getRepository()).getBranch());
        assertEquals(health, secured.getProjectIndexHealth("design").orElseThrow());
        assertSame(refresh, secured.refreshBranch("design", "feature/rates"));
        assertTrue(secured.hasProject("design", "Rates"));
        assertFalse(secured.hasProject("design", "Missing"));
        assertFalse(secured.hasProject("design", "DeniedOnly"));
        assertTrue(secured.hasProjectInAnyBranch("design", "DeniedOnly"));
    }

    @Test
    void countsBranchesThatHoldAProjectWithoutHidingTheUnreadableOnes() {
        var delegate = mock(DesignTimeRepository.class);
        var aclService = mock(RepositoryAclService.class);
        var denied = project("main", "DESIGN/Denied/Rates");
        when(aclService.isGranted(any(AProject.class), anyList())).thenReturn(false);
        when(delegate.isLastProjectBranch("design", "Rates", "feature/rates")).thenReturn(false);
        when(delegate.getProjectsHeldOnlyBy("design", "feature/rates")).thenReturn(List.of(denied));
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        // Deleting a branch removes content the caller may not read, so the guard must see every holder:
        // filtering them out would report the last branch where there is none and let the deletion through.
        assertFalse(secured.isLastProjectBranch("design", "Rates", "feature/rates"));
        assertEquals(List.of(denied), secured.getProjectsHeldOnlyBy("design", "feature/rates"));
    }

    @Test
    void hidesRepositoryHealthWithoutRepositoryReadPermission() {
        var delegate = mock(DesignTimeRepository.class);
        var aclService = mock(RepositoryAclService.class);
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        assertTrue(secured.getProjectIndexHealth("design").isEmpty());

        verify(delegate, never()).getProjectIndexHealth("design");
    }

    private static AProject project(String branch, String path) {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn(branch);
        when(repository.getBaseBranch()).thenReturn("main");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository)
                .setVersions(true)
                .setBranches(true)
                .build());
        var fileData = new FileData();
        fileData.setName(path);
        return new AProject(repository, fileData);
    }

    private static BranchEntry entry(AProject project, Instant time) {
        return new BranchEntry(project,
                new BranchStatus(new UserInfo("author"), time, "message", project.getFolderPath(), false));
    }
}
