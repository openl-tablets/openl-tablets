package org.openl.rules.workspace.dtr;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.BranchTreeRevision;
import org.openl.rules.repository.api.FileData;

/**
 * Builds immutable repository-wide project snapshots from the current trees of every repository branch.
 *
 * <p>Repository scans run on virtual threads and never on a caller that reads a snapshot. One coordinator runs per
 * repository. Notifications that arrive while a build is running are coalesced into a following pass.
 *
 * <p>A failed branch keeps its last successful snapshot. The repository health becomes {@link IndexState#DEGRADED}
 * until a later scan succeeds. A branch whose tip changes during a scan is retried before write waiters complete.
 */
@Slf4j
public final class BranchedProjectIndexService implements AutoCloseable {

    private static final Comparator<String> BRANCH_ORDER = String.CASE_INSENSITIVE_ORDER
            .thenComparing(Comparator.naturalOrder());
    private static final String LIST_ERROR = "Repository branches cannot be indexed.";
    private static final String STATUS_ERROR = "Branch status cannot be resolved.";
    private static final String TREE_ERROR = "Branch content revision cannot be resolved.";
    private static final String SCAN_ERROR = "Branch content cannot be indexed.";

    private final ExecutorService executor;
    private final Map<String, Coordinator> coordinators = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public BranchedProjectIndexService() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Registers a repository and starts its initial index build.
     *
     * @param repository     branch-capable design repository
     * @param discoveryPath repository-relative path listed for projects
     * @return a stage completed after the first successful repository snapshot
     */
    public CompletionStage<RepositorySnapshot> register(BranchRepository repository, String discoveryPath) {
        ensureOpen();
        var coordinator = new Coordinator(repository, normalizeDiscoveryPath(discoveryPath));
        var existing = coordinators.putIfAbsent(repository.getId(), coordinator);
        if (existing != null) {
            throw new IllegalStateException("Repository '%s' is already registered.".formatted(repository.getId()));
        }
        return coordinator.invalidateRepository();
    }

    /**
     * Returns the latest completed snapshot. The initial snapshot has {@link IndexState#INDEXING} health.
     */
    public RepositorySnapshot getSnapshot(String repositoryId) {
        ensureOpen();
        var coordinator = coordinators.get(repositoryId);
        if (coordinator == null) {
            throw new IllegalArgumentException("Repository '%s' is not registered.".formatted(repositoryId));
        }
        return coordinator.snapshot.get();
    }

    /**
     * Returns a logical project using the case-insensitive Studio project identity.
     */
    public Optional<ProjectSnapshot> getProject(String repositoryId, String projectName) {
        return getSnapshot(repositoryId).project(projectName);
    }

    /**
     * Schedules a batched repository refresh.
     */
    public CompletionStage<RepositorySnapshot> invalidateRepository(String repositoryId) {
        return coordinator(repositoryId).invalidateRepository();
    }

    /**
     * Schedules a targeted branch refresh.
     *
     * <p>The returned stage completes only after that branch is published, or exceptionally when the branch cannot
     * be indexed. A tip change detected during the scan is retried automatically.
     */
    public CompletionStage<RepositorySnapshot> invalidateBranch(String repositoryId, String branch) {
        return coordinator(repositoryId).invalidateBranch(branch);
    }

    /**
     * Stops pending work and prevents in-flight generations from publishing.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        coordinators.values().forEach(Coordinator::stop);
        coordinators.clear();
        executor.shutdownNow();
    }

    private Coordinator coordinator(String repositoryId) {
        ensureOpen();
        var coordinator = coordinators.get(repositoryId);
        if (coordinator == null) {
            throw new IllegalArgumentException("Repository '%s' is not registered.".formatted(repositoryId));
        }
        return coordinator;
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("The branched project index is closed.");
        }
    }

    private static String normalizeDiscoveryPath(@Nullable String path) {
        if (path == null || path.isEmpty() || path.endsWith("/")) {
            return path == null ? "" : path;
        }
        return path + "/";
    }

    public enum IndexState {
        @JsonProperty("indexing")
        INDEXING,
        @JsonProperty("ready")
        READY,
        @JsonProperty("degraded")
        DEGRADED
    }

    /**
     * Safe index diagnostics. Error values never contain repository URLs, credentials, or exception messages.
     */
    public record IndexHealth(
            @Parameter(description = "Current state of the cross-branch project index")
            @NonNull IndexState state,
            @Parameter(description = "Branches whose latest content could not be indexed")
            @NonNull Set<@NonNull String> failedBranches,
            @Parameter(description = "Safe summary of the latest indexing failure")
            @Nullable String lastError) {
        public IndexHealth {
            Objects.requireNonNull(state);
            failedBranches = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(failedBranches)));
        }

        private static IndexHealth indexing() {
            return new IndexHealth(IndexState.INDEXING, Set.of(), null);
        }
    }

    /**
     * One project as verified in one branch tree.
     */
    public record BranchProject(@NonNull String externalName,
                                @NonNull String externalPath,
                                @NonNull String internalPath,
                                @NonNull String branch,
                                @NonNull BranchRepository repository,
                                @NonNull FileData fileData) {
        public BranchProject {
            Objects.requireNonNull(externalName);
            Objects.requireNonNull(externalPath);
            Objects.requireNonNull(internalPath);
            Objects.requireNonNull(branch);
            Objects.requireNonNull(repository);
            Objects.requireNonNull(fileData);
        }
    }

    /**
     * Projects verified at one branch tip.
     */
    public record BranchSnapshot(@NonNull String branch,
                                 @NonNull BranchStatus status,
                                 @Nullable String treeRevision,
                                 @NonNull Map<@NonNull String, @NonNull BranchProject> projects) {
        public BranchSnapshot {
            Objects.requireNonNull(branch);
            Objects.requireNonNull(status);
            projects = immutableMap(projects);
        }
    }

    /**
     * A logical project and all branches whose current trees contain it.
     */
    public record ProjectSnapshot(@NonNull String name,
                                  @NonNull String homeBranch,
                                  @NonNull Map<@NonNull String, @NonNull BranchProject> entries) {
        public ProjectSnapshot {
            Objects.requireNonNull(name);
            Objects.requireNonNull(homeBranch);
            entries = immutableMap(entries);
        }

        public Set<String> branches() {
            return entries.keySet();
        }

        public BranchProject homeEntry() {
            return Objects.requireNonNull(entries.get(homeBranch));
        }

        public Optional<BranchProject> entry(String branch) {
            return Optional.ofNullable(entries.get(branch));
        }
    }

    /**
     * The atomically published project index for one design repository.
     */
    public record RepositorySnapshot(@NonNull String repositoryId,
                                     @NonNull Map<@NonNull String, @NonNull BranchSnapshot> branches,
                                     @NonNull Map<@NonNull String, @NonNull ProjectSnapshot> projects,
                                     @NonNull IndexHealth health) {
        public RepositorySnapshot {
            Objects.requireNonNull(repositoryId);
            branches = immutableMap(branches);
            projects = immutableMap(projects);
            Objects.requireNonNull(health);
        }

        private static RepositorySnapshot indexing(String repositoryId) {
            return new RepositorySnapshot(repositoryId, Map.of(), Map.of(), IndexHealth.indexing());
        }

        public Optional<ProjectSnapshot> project(String name) {
            return Optional.ofNullable(projects.get(projectKey(name)));
        }
    }

    private final class Coordinator {
        private final BranchRepository repository;
        private final String discoveryPath;
        private final String revisionPath;
        private final AtomicReference<RepositorySnapshot> snapshot;
        private final Object monitor = new Object();
        private final Set<String> dirtyBranches = new HashSet<>();
        private final List<Waiter> waiters = new ArrayList<>();

        private boolean dirtyRepository;
        private boolean scheduled;
        private boolean stopped;
        private long generation;
        private long invalidationSequence;

        private Coordinator(BranchRepository repository, String discoveryPath) {
            this.repository = repository;
            this.discoveryPath = discoveryPath;
            revisionPath = getRevisionPath(repository, discoveryPath);
            snapshot = new AtomicReference<>(RepositorySnapshot.indexing(repository.getId()));
        }

        private CompletionStage<RepositorySnapshot> invalidateRepository() {
            var future = new CompletableFuture<RepositorySnapshot>();
            synchronized (monitor) {
                rejectIfStopped(future);
                if (future.isDone()) {
                    return future;
                }
                dirtyRepository = true;
                waiters.add(new Waiter(null, ++invalidationSequence, future));
            }
            schedule();
            return future;
        }

        private CompletionStage<RepositorySnapshot> invalidateBranch(String branch) {
            Objects.requireNonNull(branch);
            var future = new CompletableFuture<RepositorySnapshot>();
            synchronized (monitor) {
                rejectIfStopped(future);
                if (future.isDone()) {
                    return future;
                }
                dirtyBranches.add(branch);
                waiters.add(new Waiter(branch, ++invalidationSequence, future));
            }
            schedule();
            return future;
        }

        private void schedule() {
            synchronized (monitor) {
                if (scheduled) {
                    return;
                }
                scheduled = true;
            }
            try {
                executor.execute(this::run);
            } catch (RejectedExecutionException e) {
                stop();
            }
        }

        private void run() {
            while (true) {
                Work work;
                synchronized (monitor) {
                    if (stopped || closed.get()) {
                        scheduled = false;
                        return;
                    }
                    if (!dirtyRepository && dirtyBranches.isEmpty()) {
                        scheduled = false;
                        return;
                    }
                    work = new Work(Set.copyOf(dirtyBranches), generation, invalidationSequence);
                    dirtyRepository = false;
                    dirtyBranches.clear();
                }

                var outcome = build(work);
                List<WaiterCompletion> completions;
                synchronized (monitor) {
                    if (stopped || closed.get() || generation != work.generation()) {
                        continue;
                    }
                    if (outcome.snapshot() != null) {
                        snapshot.set(outcome.snapshot());
                    }
                    dirtyBranches.addAll(outcome.retryBranches());
                    completions = collectWaiterCompletions(work, outcome);
                }
                completions.forEach(WaiterCompletion::complete);
            }
        }

        private BuildOutcome build(Work work) {
            var previous = snapshot.get();
            List<String> branchNames;
            try {
                branchNames = new ArrayList<>(repository.listBranches());
                branchNames.sort(BRANCH_ORDER);
            } catch (Exception e) {
                log.error("Failed to list branches for repository '{}'.", repository.getId(), e);
                var degraded = withHealth(previous, Map.of("", LIST_ERROR));
                return new BuildOutcome(degraded, Set.of(), Set.of(), Map.of("", LIST_ERROR));
            }

            Map<String, BranchStatus> statuses;
            try {
                statuses = repository.getBranchStatuses(branchNames);
            } catch (Exception e) {
                log.error("Failed to read branch statuses for repository '{}'.", repository.getId(), e);
                var degraded = withHealth(previous, Map.of("", STATUS_ERROR));
                return new BuildOutcome(degraded, Set.of(), Set.of(), Map.of("", STATUS_ERROR));
            }

            var actualBranches = new LinkedHashSet<>(branchNames);
            var nextBranches = new HashMap<String, BranchSnapshot>();
            previous.branches().forEach((branch, value) -> {
                if (actualBranches.contains(branch)) {
                    nextBranches.put(branch, value);
                }
            });

            var failures = new LinkedHashMap<String, String>();
            var candidates = new ArrayList<String>();
            var succeeded = new HashSet<String>();
            work.branches().stream()
                    .filter(branch -> !actualBranches.contains(branch))
                    .forEach(succeeded::add);
            for (String branch : branchNames) {
                var status = statuses.get(branch);
                if (status == null) {
                    failures.put(branch, STATUS_ERROR);
                    continue;
                }
                var old = previous.branches().get(branch);
                if (old == null ||
                        work.branches().contains(branch) ||
                        !old.status().lastCommitRevision().equals(status.lastCommitRevision())) {
                    candidates.add(branch);
                } else {
                    succeeded.add(branch);
                }
            }

            Map<String, BranchTreeRevision> revisions = Map.of();
            if (!candidates.isEmpty()) {
                try {
                    revisions = repository.getBranchTreeRevisions(candidates, revisionPath);
                } catch (Exception e) {
                    log.error("Failed to read branch tree revisions for repository '{}'.", repository.getId(), e);
                    candidates.forEach(branch -> failures.put(branch, TREE_ERROR));
                }
            }

            var retryBranches = new HashSet<String>();
            for (String branch : candidates) {
                if (failures.containsKey(branch)) {
                    continue;
                }
                var status = statuses.get(branch);
                var revision = revisions.get(branch);
                var old = revision == null ? null : previous.branches().get(branch);
                if (revision == null) {
                    failures.put(branch, TREE_ERROR);
                } else if (old != null && canReuse(old, status, revision)) {
                    nextBranches.put(branch,
                            new BranchSnapshot(branch, status, revision.treeRevision(), old.projects()));
                    succeeded.add(branch);
                } else {
                    try {
                        var scanned = scanBranch(branch, status, revision.treeRevision());
                        var verified = repository.getBranchStatuses(List.of(branch)).get(branch);
                        if (verified == null ||
                                !status.lastCommitRevision().equals(verified.lastCommitRevision())) {
                            retryBranches.add(branch);
                        } else {
                            nextBranches.put(branch, scanned);
                            succeeded.add(branch);
                        }
                    } catch (Exception e) {
                        log.error("Failed to index branch '{}' in repository '{}'.", branch, repository.getId(), e);
                        failures.put(branch, SCAN_ERROR);
                    }
                }
            }

            var orderedBranches = new LinkedHashMap<String, BranchSnapshot>();
            branchNames.forEach(branch -> Optional.ofNullable(nextBranches.get(branch))
                    .ifPresent(value -> orderedBranches.put(branch, value)));
            var projects = buildProjects(orderedBranches.values(), repository.getBaseBranch());
            var healthFailures = new LinkedHashMap<>(failures);
            retryBranches.forEach(branch -> healthFailures.put(branch, "Branch changed while it was indexed."));
            var health = healthFailures.isEmpty()
                    ? new IndexHealth(IndexState.READY, Set.of(), null)
                    : new IndexHealth(IndexState.DEGRADED, failedBranchNames(healthFailures), lastError(healthFailures));
            var updated = new RepositorySnapshot(repository.getId(), orderedBranches, projects, health);
            return new BuildOutcome(updated, succeeded, retryBranches, failures);
        }

        private static boolean canReuse(BranchSnapshot old, BranchStatus status, BranchTreeRevision revision) {
            if (!Objects.equals(old.treeRevision(), revision.treeRevision())) {
                return false;
            }
            var sameTip = old.status().lastCommitRevision().equals(status.lastCommitRevision());
            return sameTip || !revision.tipAffectsPath();
        }

        private BranchSnapshot scanBranch(String branch,
                                          BranchStatus status,
                                          @Nullable String treeRevision) throws IOException {
            var branchRepository = repository.forBranch(branch);
            Collection<FileData> files = branchRepository.supports().folders()
                    ? branchRepository.listFolders(discoveryPath)
                    : branchRepository.list(discoveryPath);
            var sortedFiles = new ArrayList<>(files);
            sortedFiles.sort(Comparator.comparing(FileData::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(FileData::getName));

            var projects = new LinkedHashMap<String, BranchProject>();
            for (FileData fileData : sortedFiles) {
                var externalPath = fileData.getName();
                var externalName = getExternalName(externalPath);
                var internalPath = branchRepository instanceof FolderMapper mapper
                        ? mapper.getRealPath(externalPath)
                        : externalPath;
                projects.putIfAbsent(projectKey(externalName),
                        new BranchProject(
                                externalName,
                                externalPath,
                                internalPath,
                                branch,
                                branchRepository,
                                fileData));
            }
            return new BranchSnapshot(branch, status, treeRevision, projects);
        }

        private List<WaiterCompletion> collectWaiterCompletions(Work work, BuildOutcome outcome) {
            var completions = new ArrayList<WaiterCompletion>();
            var iterator = waiters.iterator();
            while (iterator.hasNext()) {
                var waiter = iterator.next();
                if (waiter.invalidationSequence() > work.invalidationSequence()) {
                    continue;
                }
                if (outcome.failures().containsKey("")) {
                    completions.add(WaiterCompletion.failure(
                            waiter.future(),
                            new IOException(lastError(outcome.failures()))));
                    iterator.remove();
                } else if (waiter.branch() == null) {
                    if (outcome.failures().isEmpty() && outcome.retryBranches().isEmpty()) {
                        completions.add(WaiterCompletion.success(waiter.future(), snapshot.get()));
                        iterator.remove();
                    } else if (!outcome.failures().isEmpty()) {
                        completions.add(WaiterCompletion.failure(
                                waiter.future(),
                                new IOException(lastError(outcome.failures()))));
                        iterator.remove();
                    }
                } else if (outcome.succeededBranches().contains(waiter.branch())) {
                    completions.add(WaiterCompletion.success(waiter.future(), snapshot.get()));
                    iterator.remove();
                } else if (outcome.failures().containsKey(waiter.branch())) {
                    completions.add(WaiterCompletion.failure(
                            waiter.future(),
                            new IOException(outcome.failures().get(waiter.branch()))));
                    iterator.remove();
                }
            }
            return completions;
        }

        private void stop() {
            List<CompletableFuture<RepositorySnapshot>> pending;
            synchronized (monitor) {
                stopped = true;
                generation++;
                pending = waiters.stream().map(Waiter::future).toList();
                waiters.clear();
                dirtyBranches.clear();
                dirtyRepository = false;
            }
            var exception = new CancellationException("The branched project index is closed.");
            pending.forEach(future -> future.completeExceptionally(exception));
        }

        private void rejectIfStopped(CompletableFuture<RepositorySnapshot> future) {
            if (stopped || closed.get()) {
                future.completeExceptionally(new CancellationException("The branched project index is closed."));
            }
        }
    }

    private static String getExternalName(String externalPath) {
        var slash = externalPath.lastIndexOf('/');
        return slash < 0 ? externalPath : externalPath.substring(slash + 1);
    }

    private static String getRevisionPath(BranchRepository repository, String discoveryPath) {
        if (repository instanceof FolderMapper || discoveryPath.isEmpty()) {
            return "";
        }
        return discoveryPath.substring(0, discoveryPath.length() - 1);
    }

    private static Map<String, ProjectSnapshot> buildProjects(Collection<BranchSnapshot> branches,
                                                              String baseBranch) {
        var entriesByProject = new HashMap<String, Map<String, BranchProject>>();
        var branchSnapshots = new HashMap<String, BranchSnapshot>();
        for (BranchSnapshot branch : branches) {
            branchSnapshots.put(branch.branch(), branch);
            branch.projects().forEach((key, entry) -> entriesByProject
                    .computeIfAbsent(key, ignored -> new HashMap<>())
                    .put(branch.branch(), entry));
        }

        var projectKeys = new ArrayList<>(entriesByProject.keySet());
        projectKeys.sort(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()));
        var result = new LinkedHashMap<String, ProjectSnapshot>();
        for (String key : projectKeys) {
            var entries = entriesByProject.get(key);
            var homeBranch = chooseHomeBranch(entries.keySet(), branchSnapshots, baseBranch);
            var homeEntry = entries.get(homeBranch);
            var orderedEntries = new LinkedHashMap<String, BranchProject>();
            entries.keySet().stream().sorted(BRANCH_ORDER)
                    .forEach(branch -> orderedEntries.put(branch, entries.get(branch)));
            result.put(key, new ProjectSnapshot(homeEntry.externalName(), homeBranch, orderedEntries));
        }
        return result;
    }

    private static String chooseHomeBranch(Set<String> branches,
                                           Map<String, BranchSnapshot> snapshots,
                                           String baseBranch) {
        var actualBaseBranch = branches.stream()
                .filter(branch -> branch.equalsIgnoreCase(baseBranch))
                .findFirst();
        if (actualBaseBranch.isPresent()) {
            return actualBaseBranch.orElseThrow();
        }
        return branches.stream()
                .min(Comparator
                        .comparing((String branch) -> commitTime(snapshots.get(branch))).reversed()
                        .thenComparing(BRANCH_ORDER))
                .orElseThrow();
    }

    private static Instant commitTime(BranchSnapshot snapshot) {
        return snapshot.status().lastCommitAt();
    }

    private static RepositorySnapshot withHealth(RepositorySnapshot snapshot, Map<String, String> failures) {
        var health = new IndexHealth(IndexState.DEGRADED, failedBranchNames(failures), lastError(failures));
        return new RepositorySnapshot(snapshot.repositoryId(), snapshot.branches(), snapshot.projects(), health);
    }

    private static Set<String> failedBranchNames(Map<String, String> failures) {
        var names = new LinkedHashSet<String>();
        failures.keySet().stream().filter(name -> !name.isEmpty()).forEach(names::add);
        return names;
    }

    private static String lastError(Map<String, String> failures) {
        String last = null;
        for (String value : failures.values()) {
            last = value;
        }
        return Objects.requireNonNull(last);
    }

    private static String projectKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private record Work(Set<String> branches, long generation, long invalidationSequence) {
    }

    private record Waiter(@Nullable String branch,
                          long invalidationSequence,
                          CompletableFuture<RepositorySnapshot> future) {
    }

    private record WaiterCompletion(CompletableFuture<RepositorySnapshot> future,
                                    @Nullable RepositorySnapshot snapshot,
                                    @Nullable Exception failure) {

        private static WaiterCompletion success(CompletableFuture<RepositorySnapshot> future,
                                                RepositorySnapshot snapshot) {
            return new WaiterCompletion(future, snapshot, null);
        }

        private static WaiterCompletion failure(CompletableFuture<RepositorySnapshot> future, Exception failure) {
            return new WaiterCompletion(future, null, failure);
        }

        private void complete() {
            if (failure == null) {
                future.complete(Objects.requireNonNull(snapshot));
            } else {
                future.completeExceptionally(failure);
            }
        }
    }

    private record BuildOutcome(@Nullable RepositorySnapshot snapshot,
                                Set<String> succeededBranches,
                                Set<String> retryBranches,
                                Map<String, String> failures) {
    }
}
