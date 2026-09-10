package org.openl.rules.webstudio.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
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
import org.openl.rules.workspace.dtr.DesignProject;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SecureBranchRepository;
import org.openl.security.acl.repository.SecuredRepositoryFactory;

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
        grantPathsContaining(aclService, "Readable");
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
        grantPathsContaining(aclService, "Readable");
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

    @Test
    void asksTheAclServiceOncePerProjectWhateverTheNumberOfBranches() {
        var branches = 300;
        var entries = new LinkedHashMap<String, BranchEntry>();
        for (var i = 0; i < branches; i++) {
            // Every branch keeps the project in the same folder, so all of them share one ACL identity.
            var branch = i == 0 ? "main" : "feature/" + i;
            entries.put(branch, entry(project(branch, "DESIGN/Readable/Rates"),
                    Instant.parse("2026-07-29T09:00:00Z").plusSeconds(i)));
        }
        var branched = BranchedProject.create("Rates", "main", entries);

        var delegate = mock(DesignTimeRepository.class);
        when(delegate.getBranchedProject("design", "Rates")).thenReturn(Optional.of(branched));
        var aclService = mock(RepositoryAclService.class);
        grantPathsContaining(aclService, "Readable");
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        var visible = secured.getBranchedProject("design", "Rates").orElseThrow();

        assertEquals(branches, visible.entries().size());
        // One batch for the project, and no per-branch question behind it.
        verify(aclService, times(1)).filterGranted(anyCollection(), anyList());
        verify(aclService, never()).isGranted(any(AProject.class), anyList());
    }

    @Test
    void listingAProjectBuildsASecuredViewOfItsHomeAlone() {
        var branches = 300;
        var entries = new LinkedHashMap<String, BranchEntry>();
        for (var i = 0; i < branches; i++) {
            var branch = i == 0 ? "main" : "feature/" + i;
            entries.put(branch, entry(project(branch, "DESIGN/Readable/Rates"),
                    Instant.parse("2026-07-29T09:00:00Z").plusSeconds(i)));
        }
        var branched = BranchedProject.create("Rates", "main", entries);
        var home = branched.homeEntry().project();

        var delegate = mock(DesignTimeRepository.class);
        when(delegate.getProjects()).thenAnswer(invocation -> List.of(home));
        when(delegate.getBranchedProject("design", "Rates")).thenReturn(Optional.of(branched));
        var aclService = mock(RepositoryAclService.class);
        grantPathsContaining(aclService, "Readable");
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        try (var factory = mockStatic(SecuredRepositoryFactory.class)) {
            factory.when(() -> SecuredRepositoryFactory.wrapToSecureRepo(any(), any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            secured.getProjects();

            // The listing reads the home and nothing else, so the other 299 entries are never built into a view.
            factory.verify(() -> SecuredRepositoryFactory.wrapToSecureRepo(any(), any()), times(1));
        }
    }

    @Test
    void aBranchAwareReadBuildsASecuredViewOfEveryEntryItHandsOut() {
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("main", entry(project("main", "DESIGN/Readable/Rates"),
                Instant.parse("2026-07-29T09:00:00Z")));
        entries.put("feature/rates", entry(project("feature/rates", "DESIGN/Readable/Rates"),
                Instant.parse("2026-07-29T10:00:00Z")));
        var branched = BranchedProject.create("Rates", "main", entries);

        var delegate = mock(DesignTimeRepository.class);
        when(delegate.getBranchedProject("design", "Rates")).thenReturn(Optional.of(branched));
        var aclService = mock(RepositoryAclService.class);
        grantPathsContaining(aclService, "Readable");
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        var visible = secured.getBranchedProject("design", "Rates").orElseThrow();

        // Whoever asks for the branches gets a secured view of each of them, not just of the home.
        visible.entries().values()
                .forEach(entry -> assertInstanceOf(SecureBranchRepository.class,
                        entry.project().getRepository()));
    }

    @Test
    void aProjectAndItsBranchesAreResolvedInOnePass() {
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("main", entry(project("main", "DESIGN/Readable/Rates"),
                Instant.parse("2026-07-29T09:00:00Z")));
        entries.put("feature/rates", entry(project("feature/rates", "DESIGN/Readable/Rates"),
                Instant.parse("2026-07-29T10:00:00Z")));
        var branched = BranchedProject.create("Rates", "main", entries);

        var delegate = mock(DesignTimeRepository.class);
        when(delegate.getDesignProjects())
                .thenReturn(List.of(new DesignProject(branched.homeEntry().project(), branched)));
        var aclService = mock(RepositoryAclService.class);
        grantPathsContaining(aclService, "Readable");
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        var designProject = secured.getDesignProjects().iterator().next();

        // Both halves come from one question about the project, not one question each.
        verify(aclService, times(1)).filterGranted(anyCollection(), anyList());
        verify(delegate, never()).getBranchedProject(anyString(), anyString());
        assertInstanceOf(SecureBranchRepository.class, designProject.project().getRepository());
        assertEquals(List.of("main", "feature/rates"),
                List.copyOf(designProject.branches().entries().keySet()));
        designProject.branches().entries().values()
                .forEach(branch -> assertInstanceOf(SecureBranchRepository.class,
                        branch.project().getRepository()));
    }

    @Test
    void aProjectWithoutBranchesIsStillListedWhenItIsReadable() {
        var plain = project("main", "DESIGN/Readable/Rates");
        var delegate = mock(DesignTimeRepository.class);
        when(delegate.getDesignProjects()).thenReturn(List.of(new DesignProject(plain, null)));
        var aclService = mock(RepositoryAclService.class);
        when(aclService.isGranted(any(AProject.class), anyList())).thenReturn(true);
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        var designProject = secured.getDesignProjects().iterator().next();

        assertNull(designProject.branches());
        assertNotSame(plain.getRepository(), designProject.project().getRepository());
    }

    @Test
    void aProjectNoBranchOfWhichIsReadableIsLeftOut() {
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("main", entry(project("main", "DESIGN/Denied/Rates"),
                Instant.parse("2026-07-29T09:00:00Z")));
        var branched = BranchedProject.create("Rates", "main", entries);

        var delegate = mock(DesignTimeRepository.class);
        when(delegate.getDesignProjects())
                .thenReturn(List.of(new DesignProject(branched.homeEntry().project(), branched)));
        var aclService = mock(RepositoryAclService.class);
        grantPathsContaining(aclService, "Readable");
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        assertTrue(secured.getDesignProjects().isEmpty());
        // The listed project is the view of a branch that already refused, so it is not asked about again.
        verify(aclService, never()).isGranted(any(AProject.class), anyList());
    }

    @Test
    void readingOneProjectBuildsASecuredViewOfItsHomeAlone() throws Exception {
        var branches = 300;
        var entries = new LinkedHashMap<String, BranchEntry>();
        for (var i = 0; i < branches; i++) {
            var branch = i == 0 ? "main" : "feature/" + i;
            entries.put(branch, entry(project(branch, "DESIGN/Readable/Rates"),
                    Instant.parse("2026-07-29T09:00:00Z").plusSeconds(i)));
        }
        var branched = BranchedProject.create("Rates", "main", entries);

        var delegate = mock(DesignTimeRepository.class);
        when(delegate.getBranchedProject("design", "Rates")).thenReturn(Optional.of(branched));
        var aclService = mock(RepositoryAclService.class);
        grantPathsContaining(aclService, "Readable");
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        try (var factory = mockStatic(SecuredRepositoryFactory.class)) {
            factory.when(() -> SecuredRepositoryFactory.wrapToSecureRepo(any(), any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            assertNotNull(secured.getProject("design", "Rates"));

            // Asking for a project hands back its home, so the other 299 entries are never built into a view.
            factory.verify(() -> SecuredRepositoryFactory.wrapToSecureRepo(any(), any()), times(1));
        }
    }

    @Test
    void offeringARepositoryTakesOnePermissionBatchPerProjectAndNoSecuredView() throws Exception {
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("main", entry(project("main", "DESIGN/Readable/Rates"),
                Instant.parse("2026-07-29T09:00:00Z")));
        var branched = BranchedProject.create("Rates", "main", entries);
        var home = branched.homeEntry().project();

        var delegate = mock(DesignTimeRepository.class);
        when(delegate.getRepositories()).thenReturn(List.of(home.getRepository()));
        when(delegate.getProjects("design")).thenAnswer(invocation -> List.of(home));
        when(delegate.getBranchedProject("design", "Rates")).thenReturn(Optional.of(branched));
        var aclService = mock(RepositoryAclService.class);
        grantPathsContaining(aclService, "Readable");
        var secured = new SecureDesignTimeRepositoryImpl(delegate, aclService);

        assertEquals(1, secured.getRepositories().size());

        // Whether a repository has anything to offer is one bit, so no project of it is built into a view.
        verify(aclService, times(1)).filterGranted(anyCollection(), anyList());
        verify(aclService, never()).isGranted(any(AProject.class), anyList());
    }

    /**
     * Grants every artefact whose real path contains the marker, the way the ACL service answers a batch.
     */
    private static void grantPathsContaining(RepositoryAclService aclService, String marker) {
        when(aclService.filterGranted(anyCollection(), anyList())).thenAnswer(invocation -> {
            Collection<AProject> artefacts = invocation.getArgument(0);
            Set<AProject> granted = Collections.newSetFromMap(new IdentityHashMap<>());
            artefacts.stream().filter(project -> project.getRealPath().contains(marker)).forEach(granted::add);
            return granted;
        });
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
