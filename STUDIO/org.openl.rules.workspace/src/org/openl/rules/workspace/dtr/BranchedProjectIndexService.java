package org.openl.rules.workspace.dtr;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import java.util.function.Consumer;
import java.util.regex.Pattern;

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
    private static final Pattern FOLDER_HASH = Pattern.compile("[0-9a-f]{64}");
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
     * <p>The build publishes twice: an early snapshot with the default branch's projects mapped across every branch
     * that holds them, then a complete snapshot that also lists projects living only on non-default branches.
     * {@code onPublished} runs after each publish so a reader can refresh, and is told whether the
     * snapshot differs from the one before it: a rebuild also runs for work that leaves the
     * repository alone — opening a project re-indexes and finds the same trees — and a reader that
     * turns a publish into a notification of its own has nothing to say about those.
     *
     * @param repository     branch-capable design repository
     * @param discoveryPath  repository-relative path listed for projects
     * @param onPublished    runs after every snapshot the build publishes, taking whether its
     *                       content differs from the previously published one
     * @return a stage completed after the first complete repository snapshot
     */
    public CompletionStage<RepositorySnapshot> register(BranchRepository repository,
                                                        String discoveryPath,
                                                        Consumer<Boolean> onPublished) {
        ensureOpen();
        var coordinator = new Coordinator(repository, normalizeDiscoveryPath(discoveryPath), onPublished);
        var existing = coordinators.putIfAbsent(repository.getId(), coordinator);
        if (existing != null) {
            throw new IllegalStateException("Repository '%s' is already registered.".formatted(repository.getId()));
        }
        return coordinator.invalidateRepository();
    }

    /**
     * Returns the latest published snapshot.
     *
     * <p>Until the first build publishes, that is an empty placeholder. A snapshot published while health is
     * {@link IndexState#INDEXING} lists the default branch's projects across their branches, but not yet the
     * projects that live only on other branches.
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
     * Returns the logical project the given name refers to.
     *
     * <p>A mapped name is matched on the folder it carries, so it finds the project whichever branch the name was
     * taken from. A plain name is matched case-insensitively against the name each project is displayed under.
     */
    public Optional<ProjectSnapshot> getProject(String repositoryId, String projectName) {
        return getSnapshot(repositoryId).project(projectName);
    }

    /**
     * The folder a mapped project name identifies, or {@code null} when the name identifies none.
     *
     * <p>A mapped repository names a project {@code businessName:folderHash}, so the folder is what the name ends
     * with. A name whose ending is not such a hash is a plain project name, even when it contains a colon.
     */
    public static @Nullable String folderOf(String name) {
        var separator = name.lastIndexOf(':');
        if (separator < 0) {
            return null;
        }
        var folder = name.substring(separator + 1);
        return FOLDER_HASH.matcher(folder).matches() ? folder : null;
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
     *
     * @param branches  every indexed branch, by branch name
     * @param projects  the repository's logical projects, by the internal folder each one lives in
     * @param published whether a build already replaced the placeholder every repository starts with
     */
    public record RepositorySnapshot(@NonNull String repositoryId,
                                     @NonNull Map<@NonNull String, @NonNull BranchSnapshot> branches,
                                     @NonNull Map<@NonNull String, @NonNull ProjectSnapshot> projects,
                                     @NonNull IndexHealth health,
                                     boolean published) {
        public RepositorySnapshot {
            Objects.requireNonNull(repositoryId);
            branches = immutableMap(branches);
            projects = immutableMap(projects);
            Objects.requireNonNull(health);
        }

        private static RepositorySnapshot indexing(String repositoryId) {
            return new RepositorySnapshot(repositoryId, Map.of(), Map.of(), IndexHealth.indexing(), false);
        }

        /**
         * The project the given name refers to.
         *
         * <p>A name is what a branch calls a project, not what identifies it, so a plain name may be shown by
         * more than one project and then answers with the first in display order. A mapped name carries the
         * folder, which does identify, and is matched on it whichever branch the name was taken from.
         */
        public Optional<ProjectSnapshot> project(String name) {
            var folder = folderOf(name);
            if (folder != null) {
                return projects.values()
                        .stream()
                        .filter(project -> project.entries()
                                .values()
                                .stream()
                                .anyMatch(entry -> folder.equals(folderOf(entry.externalName()))))
                        .findFirst();
            }
            return projects.values()
                    .stream()
                    .filter(project -> project.name().equalsIgnoreCase(name))
                    .findFirst();
        }

    }

    private final class Coordinator {
        private final BranchRepository repository;
        private final String discoveryPath;
        private final String revisionPath;
        private final Consumer<Boolean> onPublished;
        private final AtomicReference<RepositorySnapshot> snapshot;
        private final Object monitor = new Object();
        private final Set<String> dirtyBranches = new HashSet<>();
        private final List<Waiter> waiters = new ArrayList<>();

        private boolean dirtyRepository;
        private boolean scheduled;
        private boolean stopped;
        private long generation;
        private long invalidationSequence;

        private Coordinator(BranchRepository repository, String discoveryPath, Consumer<Boolean> onPublished) {
            this.repository = repository;
            this.discoveryPath = discoveryPath;
            this.onPublished = onPublished;
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
            try {
                index();
            } finally {
                // Whatever ended the pass, the coordinator must not stay marked as running: a later
                // invalidation has to be able to schedule it again.
                synchronized (monitor) {
                    scheduled = false;
                }
            }
        }

        private void index() {
            while (true) {
                Work work;
                synchronized (monitor) {
                    if (stopped || abandoned()) {
                        return;
                    }
                    if (!dirtyRepository && dirtyBranches.isEmpty()) {
                        return;
                    }
                    work = new Work(Set.copyOf(dirtyBranches), generation, invalidationSequence);
                    dirtyRepository = false;
                    dirtyBranches.clear();
                }

                var outcome = build(work);
                List<WaiterCompletion> completions;
                RepositorySnapshot previous;
                synchronized (monitor) {
                    // An abandoned pass stopped part-way through the branches, so its result describes less than
                    // the repository holds and must never replace the published snapshot.
                    if (stopped || abandoned() || generation != work.generation()) {
                        continue;
                    }
                    previous = snapshot.getAndSet(outcome.snapshot());
                    dirtyBranches.addAll(outcome.retryBranches());
                    completions = collectWaiterCompletions(work, outcome);
                }
                notifyPublished(previous, outcome.snapshot());
                completions.forEach(WaiterCompletion::complete);
            }
        }

        /**
         * Whether the pass must give up before the next branch. Closing the index and interrupting the thread that
         * carries the scan mean the same thing, and a repository with many branches takes long enough that waiting
         * for the whole pass to end would hold the repository open well past shutdown.
         *
         * <p>The interrupt is left set, so the thread still ends as interrupted.
         */
        private boolean abandoned() {
            return closed.get() || Thread.currentThread().isInterrupted();
        }

        /**
         * Tells the reader that a snapshot is available, and whether it holds anything the previous one
         * did not.
         *
         * <p>A rebuild runs on everything the repository reports, including work that leaves its content
         * alone — opening a project in a workspace re-indexes and finds the same trees — and the reader
         * decides for itself what an unchanged snapshot is worth.
         *
         * <p>A reader that fails to take a snapshot must not stop the repository from being indexed, so its
         * failure is reported and the pass continues.
         */
        private void notifyPublished(RepositorySnapshot previous, RepositorySnapshot published) {
            try {
                onPublished.accept(!published.equals(previous));
            } catch (RuntimeException e) {
                log.error("Failed to publish the project index of repository '{}'.", repository.getId(), e);
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
            var scan = new ScanState(previous, statuses, revisions, nextBranches, failures, succeeded, retryBranches);

            // Phase 1: index the default branch first, then map its projects onto every branch that holds them, so
            // those projects appear across all their branches before the slower discovery of the rest runs. Only the
            // first build has nothing to show yet; later ones already serve a full snapshot and skip the early pass.
            var baseBranch = resolveBaseBranch(branchNames);
            if (candidates.remove(baseBranch)) {
                scanCandidate(baseBranch, scan);
                if (!previous.published()) {
                    publishIntermediate(intermediateSnapshot(branchNames, baseBranch, scan), work);
                }
            }

            // Phase 2: index the remaining branches, surfacing projects that live only on non-default branches.
            for (String branch : candidates) {
                scanCandidate(branch, scan);
            }

            return finish(branchNames, scan);
        }

        /**
         * Indexes one branch: reuses the previous snapshot when its tree is unchanged, otherwise scans it and retries
         * if the branch tip moved while the scan ran.
         */
        private void scanCandidate(String branch, ScanState scan) {
            if (abandoned() || scan.failures().containsKey(branch)) {
                return;
            }
            var status = scan.statuses().get(branch);
            var revision = scan.revisions().get(branch);
            var old = revision == null ? null : scan.previous().branches().get(branch);
            if (revision == null) {
                scan.failures().put(branch, TREE_ERROR);
            } else if (old != null && canReuse(old, status, revision)) {
                scan.nextBranches().put(branch,
                        new BranchSnapshot(branch, status, revision.treeRevision(), old.projects()));
                scan.succeeded().add(branch);
            } else {
                try {
                    var scanned = scanBranch(branch, status, revision.treeRevision());
                    var verified = repository.getBranchStatuses(List.of(branch)).get(branch);
                    if (verified == null || !status.lastCommitRevision().equals(verified.lastCommitRevision())) {
                        scan.retryBranches().add(branch);
                    } else {
                        scan.nextBranches().put(branch, scanned);
                        scan.succeeded().add(branch);
                    }
                } catch (Exception e) {
                    log.error("Failed to index branch '{}' in repository '{}'.", branch, repository.getId(), e);
                    scan.failures().put(branch, SCAN_ERROR);
                }
            }
        }

        private BuildOutcome finish(List<String> branchNames, ScanState scan) {
            var healthFailures = new LinkedHashMap<>(scan.failures());
            scan.retryBranches().forEach(branch -> healthFailures.put(branch, "Branch changed while it was indexed."));
            var health = healthFailures.isEmpty()
                    ? new IndexHealth(IndexState.READY, Set.of(), null)
                    : new IndexHealth(IndexState.DEGRADED, failedBranchNames(healthFailures), lastError(healthFailures));
            var updated = snapshotOf(orderBranches(branchNames, scan.nextBranches()), health);
            return new BuildOutcome(updated, scan.succeeded(), scan.retryBranches(), scan.failures());
        }

        /**
         * The early snapshot: the branches indexed so far, plus the default branch's projects mapped onto every other
         * branch whose tree still holds them. It reports {@link IndexState#INDEXING} because Phase 2 has not run yet.
         */
        private RepositorySnapshot intermediateSnapshot(List<String> branchNames, String baseBranch, ScanState scan) {
            var branches = new LinkedHashMap<>(scan.nextBranches());
            var baseSnapshot = branches.get(baseBranch);
            if (baseSnapshot != null && !baseSnapshot.projects().isEmpty()) {
                branches.putAll(baseProjectMembership(baseSnapshot, branchNames, baseBranch, scan));
            }
            return snapshotOf(orderBranches(branchNames, branches), IndexHealth.indexing());
        }

        private RepositorySnapshot snapshotOf(Map<String, BranchSnapshot> branches, IndexHealth health) {
            return new RepositorySnapshot(repository.getId(),
                    branches,
                    buildProjects(branches.values(), repository.getBaseBranch()),
                    health,
                    true);
        }

        /**
         * Maps each default-branch project onto every not-yet-indexed branch whose tree still holds it, so those
         * projects are listed across their branches before the branches themselves are scanned.
         */
        private Map<String, BranchSnapshot> baseProjectMembership(BranchSnapshot baseSnapshot,
                                                                  List<String> branchNames,
                                                                  String baseBranch,
                                                                  ScanState scan) {
            var statuses = scan.statuses();
            var views = new LinkedHashMap<String, BranchRepository>();
            branchNames.stream()
                    .filter(branch -> !branch.equals(baseBranch))
                    .filter(branch -> statuses.get(branch) != null)
                    .filter(branch -> !scan.nextBranches().containsKey(branch))
                    .forEach(branch -> selectBranch(branch).ifPresent(view -> views.put(branch, view)));
            if (views.isEmpty()) {
                return Map.of();
            }

            var projectsByBranch = new LinkedHashMap<String, Map<String, BranchProject>>();
            baseSnapshot.projects().forEach((key, baseProject) -> membership(views.keySet(), baseProject)
                    .forEach(branch -> projectsByBranch.computeIfAbsent(branch, ignored -> new LinkedHashMap<>())
                            .put(key, memberProject(baseProject, branch, statuses.get(branch), views.get(branch)))));

            var result = new LinkedHashMap<String, BranchSnapshot>();
            projectsByBranch.forEach((branch, projects) ->
                    result.put(branch, new BranchSnapshot(branch, statuses.get(branch), null, projects)));
            return result;
        }

        /** The branches whose current tree holds the project, resolved with one path-scoped revision lookup. */
        private Collection<String> membership(Collection<String> branches, BranchProject project) {
            try {
                return repository.getBranchTreeRevisions(branches, project.externalPath())
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().treeRevision() != null)
                        .map(Map.Entry::getKey)
                        .toList();
            } catch (Exception e) {
                log.warn("Failed to map project '{}' across branches in repository '{}'.",
                        project.externalName(), repository.getId(), e);
                return List.of();
            }
        }

        private Optional<BranchRepository> selectBranch(String branch) {
            try {
                return Optional.of(repository.forBranch(branch));
            } catch (IOException e) {
                log.warn("Failed to select branch '{}' in repository '{}'.", branch, repository.getId(), e);
                return Optional.empty();
            }
        }

        private static BranchProject memberProject(BranchProject baseProject,
                                                   String branch,
                                                   BranchStatus status,
                                                   BranchRepository view) {
            return new BranchProject(baseProject.externalName(),
                    baseProject.externalPath(),
                    baseProject.internalPath(),
                    branch,
                    view,
                    branchFileData(baseProject.fileData(), branch, status));
        }

        private void publishIntermediate(RepositorySnapshot intermediate, Work work) {
            if (intermediate.projects().isEmpty()) {
                // Nothing to show yet: keep the placeholder so readers stay on the configured-branch listing.
                return;
            }
            RepositorySnapshot previous;
            synchronized (monitor) {
                if (stopped || abandoned() || generation != work.generation()) {
                    return;
                }
                previous = snapshot.getAndSet(intermediate);
            }
            notifyPublished(previous, intermediate);
        }

        private String resolveBaseBranch(List<String> branchNames) {
            var configured = repository.getBaseBranch();
            return actualBranch(branchNames, configured).orElse(configured);
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
                // Keyed by the folder the project lives in: that is what stays the same across branches,
                // while the name a branch shows it under does not.
                projects.putIfAbsent(internalPath,
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

    /**
     * Derives a branch-scoped file record for a project already indexed on the default branch, keeping its external
     * identity while reporting the target branch and its tip revision and author.
     */
    private static FileData branchFileData(FileData source, String branch, BranchStatus status) {
        var copy = new FileData();
        copy.setName(source.getName());
        copy.setBranch(branch);
        copy.setVersion(status.lastCommitRevision());
        copy.setAuthor(status.lastCommitAuthor());
        copy.setComment(status.lastCommitMessage());
        copy.setModifiedAt(Date.from(status.lastCommitAt()));
        copy.setUniqueId(source.getUniqueId());
        source.getAdditionalData().values().forEach(copy::addAdditionalData);
        return copy;
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

        var built = new HashMap<String, ProjectSnapshot>();
        for (var entry : entriesByProject.entrySet()) {
            var entries = entry.getValue();
            var homeBranch = chooseHomeBranch(entries.keySet(), branchSnapshots, baseBranch);
            var homeEntry = entries.get(homeBranch);
            var orderedEntries = new LinkedHashMap<String, BranchProject>();
            entries.keySet().stream().sorted(BRANCH_ORDER)
                    .forEach(branch -> orderedEntries.put(branch, entries.get(branch)));
            built.put(entry.getKey(), new ProjectSnapshot(homeEntry.externalName(), homeBranch, orderedEntries));
        }

        // Read in the order the projects are shown in, which the folder they live in does not give.
        var result = new LinkedHashMap<String, ProjectSnapshot>();
        built.entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<String, ProjectSnapshot>, String>comparing(e -> e.getValue().name(),
                        String.CASE_INSENSITIVE_ORDER).thenComparing(Map.Entry::getKey))
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    private static String chooseHomeBranch(Set<String> branches,
                                           Map<String, BranchSnapshot> snapshots,
                                           String baseBranch) {
        return actualBranch(branches, baseBranch).orElseGet(() -> branches.stream()
                .min(Comparator
                        .comparing((String branch) -> commitTime(snapshots.get(branch))).reversed()
                        .thenComparing(BRANCH_ORDER))
                .orElseThrow());
    }

    /**
     * The actual ref matching a configured branch name, whatever casing the configuration used.
     */
    private static Optional<String> actualBranch(Collection<String> branches, String configured) {
        return branches.stream().filter(branch -> branch.equalsIgnoreCase(configured)).findFirst();
    }

    /**
     * The given branch snapshots in repository listing order.
     */
    private static Map<String, BranchSnapshot> orderBranches(List<String> branchNames,
                                                             Map<String, BranchSnapshot> byBranch) {
        var ordered = new LinkedHashMap<String, BranchSnapshot>();
        branchNames.forEach(branch -> Optional.ofNullable(byBranch.get(branch))
                .ifPresent(value -> ordered.put(branch, value)));
        return ordered;
    }

    private static Instant commitTime(BranchSnapshot snapshot) {
        return snapshot.status().lastCommitAt();
    }

    private static RepositorySnapshot withHealth(RepositorySnapshot snapshot, Map<String, String> failures) {
        var health = new IndexHealth(IndexState.DEGRADED, failedBranchNames(failures), lastError(failures));
        return new RepositorySnapshot(snapshot.repositoryId(),
                snapshot.branches(),
                snapshot.projects(),
                health,
                snapshot.published());
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

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private record Work(Set<String> branches, long generation, long invalidationSequence) {
    }

    private record ScanState(RepositorySnapshot previous,
                             Map<String, BranchStatus> statuses,
                             Map<String, BranchTreeRevision> revisions,
                             Map<String, BranchSnapshot> nextBranches,
                             Map<String, String> failures,
                             Set<String> succeeded,
                             Set<String> retryBranches) {
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

    private record BuildOutcome(RepositorySnapshot snapshot,
                                Set<String> succeededBranches,
                                Set<String> retryBranches,
                                Map<String, String> failures) {
    }
}
