package org.openl.rules.workspace.dtr.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.BranchTreeRevision;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService;
import org.openl.rules.workspace.dtr.FolderMapper;

class DesignTimeRepositoryImplTest {

    @Test
    void resolvesListedProjectVersionOnlyOnDemand() {
        var repository = mock(Repository.class);
        var fileData = mock(FileData.class);
        when(fileData.getName()).thenReturn("DESIGN/Rates");
        when(fileData.getVersion()).thenReturn("revision");

        var project = new AProject(repository, fileData);

        verify(fileData, never()).getVersion();
        assertEquals("revision", project.getHistoryVersion());
        assertEquals("revision", project.getHistoryVersion());
        verify(fileData).getVersion();
    }

    @Test
    void listsAProjectThatExistsOnlyInANonBaseBranch() throws Exception {
        var root = branchRepository("main");
        var main = branchRepository("main");
        var feature = branchRepository("feature/rates");
        var branchOnly = mock(FileData.class);
        when(branchOnly.getName()).thenReturn("DESIGN/Rates");
        var created = fileData("DESIGN/Created");
        when(root.listFolders("DESIGN/")).thenReturn(List.of());
        when(root.listBranches()).thenReturn(List.of("main", "feature/rates"));
        when(root.getBranchStatuses(anyCollection())).thenAnswer(invocation -> statuses(invocation.getArgument(0)));
        var treeGeneration = new AtomicInteger();
        when(root.getBranchTreeRevisions(anyCollection(), anyString()))
                .thenAnswer(invocation -> revisions(invocation.getArgument(0), treeGeneration.incrementAndGet()));
        when(root.forBranch("main")).thenReturn(main);
        when(root.forBranch("feature/rates")).thenReturn(feature);
        when(main.listFolders("DESIGN/")).thenReturn(List.of());
        when(feature.listFolders("DESIGN/")).thenReturn(List.of(branchOnly), List.of(branchOnly, created));

        var properties = mock(PropertyResolver.class);
        when(properties.getProperty("design-repository-configs")).thenReturn("design");
        when(properties.getProperty("repository.design.base.path")).thenReturn("DESIGN");
        var published = new CountDownLatch(1);
        try (var index = new BranchedProjectIndexService()) {
            var repository = new TestDesignTimeRepository(properties, index, root);
            repository.addListener(published::countDown);
            repository.init();

            assertTrue(published.await(5, TimeUnit.SECONDS));
            assertEquals(List.of("Rates"),
                    repository.getProjects().stream().map(AProject::getBusinessName).toList());
            var project = repository.getBranchedProject("design", "Rates").orElseThrow();
            assertEquals("feature/rates", project.homeBranch());
            assertEquals(List.of("feature/rates"), List.copyOf(project.entries().keySet()));
            assertEquals("feature/rates",
                    ((BranchRepository) repository.getProject("design", "Rates").getRepository()).getBranch());
            assertEquals("feature/rates",
                    ((BranchRepository) repository.getProject("design", "Rates", "feature/rates")
                            .getRepository()).getBranch());
            assertTrue(repository.hasProject("design", "Rates"));
            assertEquals(BranchedProjectIndexService.IndexState.READY,
                    repository.getProjectIndexHealth("design").orElseThrow().state());
            var historical = repository.getProjectByPath("design", "feature/rates", "DESIGN/Rates", "revision");
            assertEquals("feature/rates", ((BranchRepository) historical.getRepository()).getBranch());
            assertEquals("DESIGN/Rates", historical.getRealPath());
            repository.refreshBranch("design", "feature/rates").toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertEquals("Created", repository.getProject("design", "Created").getBusinessName());
            repository.refresh();

            verify(branchOnly, never()).getVersion();
            repository.destroy();
        }
    }

    @Test
    void keepsRequestedBranchWhenResolvingHistoryWithoutCurrentMembership() throws Exception {
        var root = branchRepository("main");
        var main = branchRepository("main");
        var feature = branchRepository("feature/rates");
        when(root.listFolders("DESIGN/")).thenReturn(List.of());
        when(root.listBranches()).thenReturn(List.of("main", "feature/rates"));
        when(root.getBranchStatuses(anyCollection())).thenAnswer(invocation -> statuses(invocation.getArgument(0)));
        when(root.getBranchTreeRevisions(anyCollection(), anyString()))
                .thenAnswer(invocation -> revisions(invocation.getArgument(0), 1));
        when(root.forBranch("main")).thenReturn(main);
        when(root.forBranch("feature/rates")).thenReturn(feature);
        when(main.listFolders("DESIGN/")).thenReturn(List.of(fileData("DESIGN/Rates")));
        when(main.forBranch("feature/rates")).thenReturn(feature);
        when(feature.listFolders("DESIGN/")).thenReturn(List.of());

        var properties = mock(PropertyResolver.class);
        when(properties.getProperty("design-repository-configs")).thenReturn("design");
        when(properties.getProperty("repository.design.base.path")).thenReturn("DESIGN");
        var published = new CountDownLatch(1);
        try (var index = new BranchedProjectIndexService()) {
            var repository = new TestDesignTimeRepository(properties, index, root);
            // The default branch's project is mapped across branches early; wait for the complete snapshot, where
            // the branch that does not hold it drops it again.
            repository.addListener(() -> {
                if (repository.getProjectIndexHealth("design")
                        .filter(health -> health.state() == BranchedProjectIndexService.IndexState.READY)
                        .isPresent()) {
                    published.countDown();
                }
            });
            repository.init();

            assertTrue(published.await(5, TimeUnit.SECONDS));
            var historical = repository.getProjectByPath("design",
                    "feature/rates",
                    "DESIGN/Rates",
                    "revision");

            assertSame(feature, historical.getRepository());
            verify(main).forBranch("feature/rates");
            repository.destroy();
        }
    }

    @Test
    void resolvesBranchedMappedProjectByBusinessName() throws Exception {
        var root = mappedBranchRepository("main");
        var feature = mappedBranchRepository("feature/rates");
        when(root.listBranches()).thenReturn(List.of("feature/rates"));
        when(root.getBranchStatuses(anyCollection())).thenAnswer(invocation -> statuses(invocation.getArgument(0)));
        when(root.getBranchTreeRevisions(anyCollection(), anyString()))
                .thenAnswer(invocation -> revisions(invocation.getArgument(0), 1));
        when(root.forBranch("feature/rates")).thenReturn(feature);
        when(feature.listFolders("DESIGN/")).thenReturn(List.of(fileData("DESIGN/Rates:path-hash")));

        var properties = mock(PropertyResolver.class);
        when(properties.getProperty("design-repository-configs")).thenReturn("design");
        when(properties.getProperty("repository.design.base.path")).thenReturn("DESIGN");
        var published = new CountDownLatch(1);
        try (var index = new BranchedProjectIndexService()) {
            var repository = new TestDesignTimeRepository(properties, index, root);
            repository.addListener(published::countDown);
            repository.init();

            assertTrue(published.await(5, TimeUnit.SECONDS));
            var project = repository.getBranchedProject("design", "rates").orElseThrow();

            assertEquals("Rates:path-hash", project.name());
            assertEquals("feature/rates", project.homeBranch());
            assertEquals("Rates", repository.getProject("design", "rates").getBusinessName());
            assertTrue(repository.hasProject("design", "rates"));
            repository.destroy();
        }
    }

    @Test
    void keepsConfiguredBranchFallbackWhenInitialIndexingFails() throws Exception {
        var root = branchRepository("main");
        when(root.listFolders("DESIGN/")).thenReturn(List.of(fileData("DESIGN/Rates")));
        when(root.listBranches()).thenThrow(new IOException("Provider credentials leaked here"));

        var properties = mock(PropertyResolver.class);
        when(properties.getProperty("design-repository-configs")).thenReturn("design");
        when(properties.getProperty("repository.design.base.path")).thenReturn("DESIGN");
        var published = new CountDownLatch(1);
        try (var index = new BranchedProjectIndexService()) {
            var repository = new TestDesignTimeRepository(properties, index, root);
            repository.addListener(published::countDown);
            repository.init();

            assertTrue(published.await(5, TimeUnit.SECONDS));
            assertEquals(List.of("Rates"),
                    repository.getProjects().stream().map(AProject::getBusinessName).toList());
            var health = repository.getProjectIndexHealth("design").orElseThrow();
            assertEquals(BranchedProjectIndexService.IndexState.DEGRADED, health.state());
            assertEquals("Repository branches cannot be indexed.", health.lastError());
            repository.destroy();
        }
    }

    @Test
    void keepsConfiguredBranchFallbackWhenTheFirstBuildIndexesNoBranch() throws Exception {
        var root = branchRepository("main");
        when(root.listFolders("DESIGN/")).thenReturn(List.of(fileData("DESIGN/Rates")));
        when(root.listBranches()).thenReturn(List.of("main", "feature/rates"));
        when(root.getBranchStatuses(anyCollection())).thenAnswer(invocation -> statuses(invocation.getArgument(0)));
        when(root.getBranchTreeRevisions(anyCollection(), anyString()))
                .thenThrow(new IOException("Provider credentials leaked here"));

        var properties = mock(PropertyResolver.class);
        when(properties.getProperty("design-repository-configs")).thenReturn("design");
        when(properties.getProperty("repository.design.base.path")).thenReturn("DESIGN");
        var published = new CountDownLatch(1);
        try (var index = new BranchedProjectIndexService()) {
            var repository = new TestDesignTimeRepository(properties, index, root);
            repository.addListener(published::countDown);
            repository.init();

            assertTrue(published.await(5, TimeUnit.SECONDS));
            // The build published a snapshot holding no branch at all, so the configured-branch listing must
            // still be shown rather than an empty workspace.
            assertEquals(List.of("Rates"),
                    repository.getProjects().stream().map(AProject::getBusinessName).toList());
            assertEquals(BranchedProjectIndexService.IndexState.DEGRADED,
                    repository.getProjectIndexHealth("design").orElseThrow().state());
            repository.destroy();
        }
    }

    @Test
    void retainsNonBranchRepositoryBehaviourAndReportsRefreshFailure() throws Exception {
        var repositoryListener = new AtomicReference<Listener>();
        var root = mock(BranchRepository.class);
        when(root.getId()).thenReturn("design");
        when(root.getName()).thenReturn("Design");
        when(root.supports()).thenReturn(new FeaturesBuilder(root).setBranches(false).setVersions(true).build());
        when(root.list("DESIGN/"))
                .thenReturn(List.of(fileData("DESIGN/Rates")))
                .thenThrow(new IOException("Repository unavailable"));
        doAnswer(invocation -> {
            repositoryListener.set(invocation.getArgument(0));
            return null;
        }).when(root).setListener(org.mockito.ArgumentMatchers.nullable(Listener.class));

        var properties = mock(PropertyResolver.class);
        when(properties.getProperty("design-repository-configs")).thenReturn("design");
        when(properties.getProperty("repository.design.base.path")).thenReturn("DESIGN");
        var designRepository = new TestDesignTimeRepository(properties,
                new BranchedProjectIndexService(),
                root);
        var notifications = new AtomicInteger();
        designRepository.addListener(notifications::incrementAndGet);
        designRepository.init();

        var listed = designRepository.getProject("design", "Rates");
        assertSame(listed, designRepository.getProjects("design").getFirst());
        assertTrue(designRepository.hasProject("design", "Rates"));
        assertFalse(designRepository.hasProject("design", "Missing"));
        assertTrue(designRepository.getBranchedProject("design", "Rates").isEmpty());
        assertTrue(designRepository.getProjectIndexHealth("design").isEmpty());
        assertEquals("DESIGN/Rates",
                designRepository.getProjectByPath("design", null, "DESIGN/Rates", "revision").getRealPath());
        assertNull(designRepository.getProjectByPath("design", null, "DESIGN/Missing", "revision"));

        repositoryListener.get().onChange();
        assertEquals(1, notifications.get());
        assertTrue(designRepository.getProjects().isEmpty());
        assertEquals(List.of("Repository 'Design' : Repository unavailable"), designRepository.getExceptions());

        designRepository.destroy();
        verify(root).setListener(isNull());
        verify(root).close();
    }

    private static BranchRepository branchRepository(String branch) {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getName()).thenReturn("Design");
        when(repository.getBranch()).thenReturn(branch);
        when(repository.getBaseBranch()).thenReturn("main");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository)
                .setFolders(true)
                .setVersions(true)
                .setBranches(true)
                .build());
        return repository;
    }

    private static BranchRepository mappedBranchRepository(String branch) {
        var repository = mock(BranchRepository.class, withSettings().extraInterfaces(FolderMapper.class));
        when(repository.getId()).thenReturn("design");
        when(repository.getName()).thenReturn("Design");
        when(repository.getBranch()).thenReturn(branch);
        when(repository.getBaseBranch()).thenReturn("main");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository)
                .setFolders(true)
                .setVersions(true)
                .setBranches(true)
                .setMappedFolders(true)
                .build());
        var mapper = (FolderMapper) repository;
        when(mapper.getRealPath(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.getBusinessName(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            var suffix = path.indexOf(':');
            return suffix < 0 ? path : path.substring(0, suffix);
        });
        return repository;
    }

    private static FileData fileData(String path) {
        var fileData = new FileData();
        fileData.setName(path);
        return fileData;
    }

    private static Map<String, BranchStatus> statuses(Collection<String> branches) {
        return branches.stream().collect(java.util.stream.Collectors.toMap(
                branch -> branch,
                branch -> new BranchStatus(
                        new UserInfo("author"),
                        Instant.parse("2026-07-29T10:00:00Z"),
                        "message",
                        branch + "-revision")));
    }

    private static Map<String, BranchTreeRevision> revisions(Collection<String> branches, int generation) {
        return branches.stream().collect(java.util.stream.Collectors.toMap(
                branch -> branch,
                branch -> new BranchTreeRevision(branch + "-revision", branch + "-tree-" + generation)));
    }

    private static final class TestDesignTimeRepository extends DesignTimeRepositoryImpl {
        private final Repository repository;

        private TestDesignTimeRepository(PropertyResolver properties,
                                         BranchedProjectIndexService index,
                                         Repository repository) {
            super(properties, index);
            this.repository = repository;
        }

        @Override
        protected Repository createRepo(String configName, String baseFolder) {
            return repository;
        }
    }
}
