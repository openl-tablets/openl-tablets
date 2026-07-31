package org.openl.rules.workspace.dtr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.BranchTreeRevision;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService.IndexState;

class BranchedProjectIndexServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");

    @Test
    void indexesProjectsAcrossBranchesAndChoosesBaseBranchAsHome() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW, "DESIGN/Common");
        repository.put("feature/rates",
                "rates-1",
                "tree-rates",
                NOW.plusSeconds(60),
                "DESIGN/Common",
                "DESIGN/Rates");

        try (var service = new BranchedProjectIndexService()) {
            var snapshot = await(service.register(repository.repository(), "DESIGN/"));

            assertEquals(IndexState.READY, snapshot.health().state());
            assertEquals(Set.of("main", "feature/rates"), snapshot.project("Common").orElseThrow().branches());
            assertEquals("main", snapshot.project("Common").orElseThrow().homeBranch());
            assertEquals("feature/rates", snapshot.project("Rates").orElseThrow().homeBranch());
            assertEquals("DESIGN/Rates",
                    snapshot.project("Rates").orElseThrow().homeEntry().internalPath());
            assertEquals(Set.of("DESIGN"), repository.revisionPaths());
            assertThrows(UnsupportedOperationException.class, snapshot.projects()::clear);
        }
    }

    @Test
    void choosesTheActualBaseRefWhenItsConfiguredCasingDiffers() throws Exception {
        var repository = new TestBranchRepository();
        repository.baseBranch("MAIN");
        repository.put("main", "main-1", "tree-main", NOW, "DESIGN/Common");
        repository.put("feature/rates",
                "rates-1",
                "tree-rates",
                NOW.plusSeconds(60),
                "DESIGN/Common");

        try (var service = new BranchedProjectIndexService()) {
            var snapshot = await(service.register(repository.repository(), "DESIGN/"));

            assertEquals("main", snapshot.project("Common").orElseThrow().homeBranch());
        }
    }

    @Test
    void choosesNewestNonBaseBranchAndUsesNameAsTieBreaker() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW);
        repository.put("feature/B", "b-1", "tree-b", NOW.plusSeconds(60), "DESIGN/Rates");
        repository.put("feature/a", "a-1", "tree-a", NOW.plusSeconds(60), "DESIGN/Rates");

        try (var service = new BranchedProjectIndexService()) {
            var snapshot = await(service.register(repository.repository(), "DESIGN/"));

            assertEquals("feature/a", snapshot.project("rates").orElseThrow().homeBranch());
        }
    }

    @Test
    void usesPhysicalRootRevisionForMappedRepositories() throws Exception {
        var repository = new TestBranchRepository(true);
        repository.put("main", "main-1", "tree-main", NOW, "DESIGN/Project");

        try (var service = new BranchedProjectIndexService()) {
            await(service.register(repository.repository(), "DESIGN/"));

            assertEquals(Set.of(""), repository.revisionPaths());
        }
    }

    @Test
    void preservesPathQualifiedExternalProjectIdentity() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main",
                "main-1",
                "tree-main",
                NOW,
                "DESIGN/Rates:path-one",
                "DESIGN/Rates:path-two");
        repository.put("feature", "feature-1", "tree-feature", NOW, "DESIGN/Rates:path-one");

        try (var service = new BranchedProjectIndexService()) {
            var snapshot = await(service.register(repository.repository(), "DESIGN/"));

            assertEquals(Set.of("main", "feature"), snapshot.project("Rates:path-one").orElseThrow().branches());
            assertEquals(Set.of("main"), snapshot.project("Rates:path-two").orElseThrow().branches());
            assertEquals(2, snapshot.projects().size());
        }
    }

    @Test
    void reusesUnchangedBranchesAndRemovesDeletedRefs() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW, "DESIGN/Main");
        repository.put("feature", "feature-1", "tree-feature", NOW, "DESIGN/Feature");

        try (var service = new BranchedProjectIndexService()) {
            await(service.register(repository.repository(), "DESIGN/"));
            assertEquals(1, repository.scanCount("main"));
            assertEquals(1, repository.scanCount("feature"));

            await(service.invalidateRepository("design"));
            assertEquals(1, repository.scanCount("main"));
            assertEquals(1, repository.scanCount("feature"));

            repository.remove("feature");
            var snapshot = await(service.invalidateRepository("design"));
            assertFalse(snapshot.branches().containsKey("feature"));
            assertTrue(snapshot.project("Feature").isEmpty());
        }
    }

    @Test
    void refreshesProjectMetadataWhenMergeTipPreservesTree() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW);
        repository.put("feature", "feature-1", "tree-feature", NOW, "DESIGN/Feature");

        try (var service = new BranchedProjectIndexService()) {
            var initial = await(service.register(repository.repository(), "DESIGN/"));
            assertEquals("feature-1",
                    initial.project("Feature").orElseThrow().entry("feature").orElseThrow().fileData().getVersion());

            repository.put("feature",
                    "feature-2",
                    "tree-feature",
                    true,
                    NOW.plusSeconds(60),
                    "DESIGN/Feature");
            var refreshed = await(service.invalidateRepository("design"));

            assertEquals(2, repository.scanCount("feature"));
            assertEquals("feature-2",
                    refreshed.project("Feature")
                            .orElseThrow()
                            .entry("feature")
                            .orElseThrow()
                            .fileData()
                            .getVersion());
        }
    }

    @Test
    void retainsLastKnownGoodBranchAfterScanFailure() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW);
        repository.put("feature", "feature-1", "tree-1", NOW, "DESIGN/Old");

        try (var service = new BranchedProjectIndexService()) {
            await(service.register(repository.repository(), "DESIGN/"));
            repository.put("feature", "feature-2", "tree-2", NOW.plusSeconds(60), "DESIGN/New");
            repository.failScan("feature");

            assertThrows(ExecutionException.class, () -> await(service.invalidateBranch("design", "feature")));
            var degraded = service.getSnapshot("design");
            assertEquals(IndexState.DEGRADED, degraded.health().state());
            assertEquals(Set.of("feature"), degraded.health().failedBranches());
            assertEquals("Branch content cannot be indexed.", degraded.health().lastError());
            assertTrue(degraded.project("Old").isPresent());
            assertTrue(degraded.project("New").isEmpty());

            repository.allowScan("feature");
            var recovered = await(service.invalidateBranch("design", "feature"));
            assertEquals(IndexState.READY, recovered.health().state());
            assertTrue(recovered.project("Old").isEmpty());
            assertTrue(recovered.project("New").isPresent());
        }
    }

    @Test
    void retainsBranchForMissingStatusAndRevisionUntilRecovery() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW);
        repository.put("feature", "feature-1", "tree-1", NOW, "DESIGN/Old");

        try (var service = new BranchedProjectIndexService()) {
            await(service.register(repository.repository(), "DESIGN/"));
            repository.put("feature", "feature-2", "tree-2", NOW.plusSeconds(60), "DESIGN/New");

            repository.omitStatus("feature");
            assertThrows(ExecutionException.class, () -> await(service.invalidateRepository("design")));
            assertTrue(service.getSnapshot("design").project("Old").isPresent());

            repository.includeStatus("feature");
            repository.omitRevision("feature");
            assertThrows(ExecutionException.class, () -> await(service.invalidateRepository("design")));
            assertTrue(service.getSnapshot("design").project("Old").isPresent());

            repository.includeRevision("feature");
            var recovered = await(service.invalidateRepository("design"));
            assertTrue(recovered.project("Old").isEmpty());
            assertTrue(recovered.project("New").isPresent());
        }
    }

    @Test
    void recoversFromRepositoryEnumerationFailureWithoutExposingProviderMessage() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW, "DESIGN/Project");
        repository.failBranchListing();

        try (var service = new BranchedProjectIndexService()) {
            assertThrows(ExecutionException.class, () -> await(service.register(repository.repository(), "DESIGN/")));
            var degraded = service.getSnapshot("design");
            assertEquals(IndexState.DEGRADED, degraded.health().state());
            assertEquals("Repository branches cannot be indexed.", degraded.health().lastError());

            repository.allowBranchListing();
            assertEquals(IndexState.READY, await(service.invalidateRepository("design")).health().state());

            repository.failBranchListing();
            assertThrows(ExecutionException.class, () -> await(service.invalidateBranch("design", "main")));
        }
    }

    @Test
    void retriesWhenBranchTipChangesDuringScan() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW);
        repository.put("feature", "feature-1", "tree-1", NOW, "DESIGN/Old");

        try (var service = new BranchedProjectIndexService()) {
            await(service.register(repository.repository(), "DESIGN/"));
            repository.put("feature", "feature-2", "tree-2", NOW.plusSeconds(60), "DESIGN/Old");
            repository.beforeScanReturns("feature",
                    () -> repository.put(
                            "feature",
                            "feature-3",
                            "tree-3",
                            NOW.plusSeconds(120),
                            "DESIGN/Old",
                            "DESIGN/New"));

            var snapshot = await(service.invalidateBranch("design", "feature"));

            assertTrue(snapshot.project("New").isPresent());
            assertEquals("feature-3",
                    Objects.requireNonNull(snapshot.branches().get("feature")).status().lastCommitRevision());
            assertEquals(3, repository.scanCount("feature"));
        }
    }

    @Test
    void coalescesInvalidationThatArrivesDuringBuild() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW);
        repository.put("feature", "feature-1", "tree-1", NOW, "DESIGN/Project");

        try (var service = new BranchedProjectIndexService()) {
            await(service.register(repository.repository(), "DESIGN/"));
            repository.put("feature", "feature-2", "tree-2", NOW.plusSeconds(60), "DESIGN/Project");
            var followUp = new AtomicReference<CompletionStage<BranchedProjectIndexService.RepositorySnapshot>>();
            repository.beforeScanReturns("feature", () -> {
                followUp.set(service.invalidateBranch("design", "feature"));
                repository.blockNextBranchListing();
            });

            await(service.invalidateBranch("design", "feature"));
            assertTrue(repository.awaitBlockedBranchListing());
            assertFalse(Objects.requireNonNull(followUp.get()).toCompletableFuture().isDone(),
                    "An invalidation must not be satisfied by a build that started before it arrived.");
            repository.releaseBranchListing();
            await(Objects.requireNonNull(followUp.get()));

            assertTrue(repository.statusReadCount() >= 3);
            assertEquals(2, repository.scanCount("feature"),
                    "The follow-up pass must reuse the tree that the preceding pass already indexed.");
        }
    }

    @Test
    void completesRefreshWithoutHoldingCoordinatorLock() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW, "DESIGN/Project");
        var scanStarted = new CountDownLatch(1);
        var releaseScan = new CountDownLatch(1);
        repository.beforeScanReturns("main", () -> {
            scanStarted.countDown();
            try {
                assertTrue(releaseScan.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        try (var service = new BranchedProjectIndexService()) {
            var refresh = service.register(repository.repository(), "DESIGN/");
            assertTrue(scanStarted.await(5, TimeUnit.SECONDS));
            var callback = refresh.thenRun(() -> {
                try {
                    CompletableFuture.runAsync(() -> service.invalidateBranch("design", "main"))
                            .get(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("A completion callback could not reacquire the coordinator.", e);
                }
            });
            releaseScan.countDown();

            await(callback.thenApply(ignored -> service.getSnapshot("design")));
        }
    }

    @Test
    void closeCancelsInFlightBuildAndPreventsPublication() throws Exception {
        var repository = new TestBranchRepository();
        repository.put("main", "main-1", "tree-main", NOW, "DESIGN/Project");
        var scanStarted = new CountDownLatch(1);
        var releaseScan = new CountDownLatch(1);
        repository.beforeScanReturns("main", () -> {
            scanStarted.countDown();
            try {
                assertTrue(releaseScan.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        var service = new BranchedProjectIndexService();

        var initialBuild = service.register(repository.repository(), "DESIGN/");
        assertTrue(scanStarted.await(5, TimeUnit.SECONDS));
        assertEquals(IndexState.INDEXING, service.getSnapshot("design").health().state());
        service.close();
        releaseScan.countDown();

        assertThrows(CancellationException.class, () -> await(initialBuild));
        assertThrows(IllegalStateException.class, () -> service.getSnapshot("design"));
    }

    private static BranchedProjectIndexService.RepositorySnapshot await(
            CompletionStage<BranchedProjectIndexService.RepositorySnapshot> stage) throws Exception {
        return stage.toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private static final class TestBranchRepository {
        private final BranchRepository repository;
        private final Map<String, BranchData> branches = new LinkedHashMap<>();
        private final Map<String, BranchRepository> views = new LinkedHashMap<>();
        private final Map<String, AtomicInteger> scanCounts = new LinkedHashMap<>();
        private final Map<String, Runnable> beforeScanReturns = new LinkedHashMap<>();
        private final Set<String> failingScans = new java.util.HashSet<>();
        private final Set<String> omittedStatuses = new java.util.HashSet<>();
        private final Set<String> omittedRevisions = new java.util.HashSet<>();
        private final Set<String> revisionPaths = new java.util.HashSet<>();
        private final AtomicInteger statusReads = new AtomicInteger();
        private volatile @Nullable CountDownLatch blockedListingStarted;
        private volatile @Nullable CountDownLatch blockedListingRelease;
        private volatile boolean blockListing;
        private boolean branchListingFails;

        private TestBranchRepository() throws IOException {
            this(false);
        }

        private TestBranchRepository(boolean mapped) throws IOException {
            repository = mapped
                    ? mock(BranchRepository.class, withSettings().extraInterfaces(FolderMapper.class))
                    : mock(BranchRepository.class);
            when(repository.getId()).thenReturn("design");
            when(repository.getName()).thenReturn("Design repository");
            when(repository.getBranch()).thenReturn("main");
            when(repository.getBaseBranch()).thenReturn("main");
            when(repository.supports())
                    .thenReturn(new FeaturesBuilder(repository).setFolders(true).setBranches(true).build());
            when(repository.listBranches()).thenAnswer(invocation -> {
                if (branchListingFails) {
                    throw new IOException("Sensitive branch provider endpoint");
                }
                var started = blockedListingStarted;
                var release = blockedListingRelease;
                if (blockListing && started != null && release != null) {
                    blockListing = false;
                    started.countDown();
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("Timed out waiting to release branch listing.");
                    }
                }
                return branches.keySet()
                        .stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
                        .toList();
            });
            when(repository.getBranchStatuses(anyCollection())).thenAnswer(invocation -> {
                statusReads.incrementAndGet();
                Collection<String> requested = invocation.getArgument(0);
                var result = new LinkedHashMap<String, BranchStatus>();
                requested.forEach(branch -> {
                    var data = branches.get(branch);
                    if (data != null && !omittedStatuses.contains(branch)) {
                        result.put(branch, data.status());
                    }
                });
                return result;
            });
            when(repository.getBranchTreeRevisions(anyCollection(), anyString())).thenAnswer(invocation -> {
                Collection<String> requested = invocation.getArgument(0);
                revisionPaths.add(invocation.getArgument(1));
                var result = new LinkedHashMap<String, BranchTreeRevision>();
                requested.forEach(branch -> {
                    var data = branches.get(branch);
                    if (data != null && !omittedRevisions.contains(branch)) {
                        result.put(branch,
                                new BranchTreeRevision(data.status().lastCommitRevision(),
                                        data.treeRevision(),
                                        data.tipAffectsPath()));
                    }
                });
                return result;
            });
            when(repository.forBranch(anyString())).thenAnswer(invocation -> views.get(invocation.getArgument(0)));
        }

        private BranchRepository repository() {
            return repository;
        }

        private void baseBranch(String branch) {
            when(repository.getBaseBranch()).thenReturn(branch);
        }

        private synchronized void put(String branch,
                                      String revision,
                                      @Nullable String treeRevision,
                                      Instant committedAt,
                                      String... projects) {
            var previous = branches.get(branch);
            var tipAffectsPath = previous == null || !Objects.equals(previous.treeRevision(), treeRevision);
            put(branch, revision, treeRevision, tipAffectsPath, committedAt, projects);
        }

        private synchronized void put(String branch,
                                      String revision,
                                      @Nullable String treeRevision,
                                      boolean tipAffectsPath,
                                      Instant committedAt,
                                      String... projects) {
            branches.put(branch,
                    new BranchData(
                            new BranchStatus(new UserInfo("author"), committedAt, "Change", revision),
                            treeRevision,
                            tipAffectsPath,
                            List.of(projects)));
            views.computeIfAbsent(branch, this::createView);
            scanCounts.computeIfAbsent(branch, ignored -> new AtomicInteger());
        }

        private synchronized void remove(String branch) {
            branches.remove(branch);
        }

        private synchronized void failScan(String branch) {
            failingScans.add(branch);
        }

        private synchronized void allowScan(String branch) {
            failingScans.remove(branch);
        }

        private synchronized void omitStatus(String branch) {
            omittedStatuses.add(branch);
        }

        private synchronized void includeStatus(String branch) {
            omittedStatuses.remove(branch);
        }

        private synchronized void omitRevision(String branch) {
            omittedRevisions.add(branch);
        }

        private synchronized void includeRevision(String branch) {
            omittedRevisions.remove(branch);
        }

        private synchronized void failBranchListing() {
            branchListingFails = true;
        }

        private synchronized void allowBranchListing() {
            branchListingFails = false;
        }

        private synchronized void beforeScanReturns(String branch, Runnable action) {
            beforeScanReturns.put(branch, action);
        }

        private void blockNextBranchListing() {
            blockedListingStarted = new CountDownLatch(1);
            blockedListingRelease = new CountDownLatch(1);
            blockListing = true;
        }

        private boolean awaitBlockedBranchListing() throws InterruptedException {
            return Objects.requireNonNull(blockedListingStarted).await(5, TimeUnit.SECONDS);
        }

        private void releaseBranchListing() {
            Objects.requireNonNull(blockedListingRelease).countDown();
        }

        private int scanCount(String branch) {
            return Objects.requireNonNull(scanCounts.get(branch)).get();
        }

        private int statusReadCount() {
            return statusReads.get();
        }

        private Set<String> revisionPaths() {
            return Set.copyOf(revisionPaths);
        }

        private BranchRepository createView(String branch) {
            var view = mock(BranchRepository.class);
            when(view.supports()).thenReturn(new FeaturesBuilder(view).setFolders(true).setBranches(true).build());
            try {
                when(view.listFolders(anyString())).thenAnswer(invocation -> scan(branch));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return view;
        }

        private synchronized List<FileData> scan(String branch) throws IOException {
            Objects.requireNonNull(scanCounts.get(branch)).incrementAndGet();
            if (failingScans.contains(branch)) {
                throw new IOException("Sensitive test repository URI");
            }
            var data = Objects.requireNonNull(branches.get(branch));
            var result = new ArrayList<FileData>();
            for (String project : data.projects()) {
                var fileData = new FileData();
                fileData.setName(project);
                fileData.setVersion(data.status().lastCommitRevision());
                result.add(fileData);
            }
            var action = beforeScanReturns.remove(branch);
            if (action != null) {
                action.run();
            }
            return result;
        }
    }

    private record BranchData(BranchStatus status,
                              @Nullable String treeRevision,
                              boolean tipAffectsPath,
                              List<String> projects) {
    }
}
