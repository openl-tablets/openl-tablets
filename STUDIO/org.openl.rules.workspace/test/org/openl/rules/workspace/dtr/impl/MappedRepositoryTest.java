package org.openl.rules.workspace.dtr.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchTreeRevision;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.workspace.dtr.FolderMapper;

class MappedRepositoryTest {

    @TempDir
    Path root;

    /**
     * A project whose rules.xml has a blank name must be mapped under its folder name.
     *
     * An empty business name later resolves the local workspace path to "/", which escapes the
     * repository root on open. Such a descriptor cannot be produced through the UI, only on disk,
     * so the fallback is verified at the repository level. See {@code ProjectDescriptor#fillProjectName}.
     */
    @Test
    void blankRulesXmlNameFallsBackToFolderName() throws IOException {
        writeProject("no-name-project", "<project><name></name></project>");
        writeProject("absent-name-project", "<project></project>");
        writeProject("named-project", "<project><name>RealName</name></project>");

        var mapped = listMappedFolders("DESIGN/");

        assertEquals(3, mapped.size(), "All projects must be mapped: " + mapped);
        assertTrue(mapped.stream().anyMatch(name -> name.startsWith("DESIGN/no-name-project:")),
                "Blank rules.xml name must fall back to the folder name, but was: " + mapped);
        assertTrue(mapped.stream().anyMatch(name -> name.startsWith("DESIGN/absent-name-project:")),
                "Missing rules.xml name must fall back to the folder name, but was: " + mapped);
        assertTrue(mapped.stream().anyMatch(name -> name.startsWith("DESIGN/RealName:")),
                "Named project must keep its rules.xml name, but was: " + mapped);
    }

    /**
     * Two folders may declare the same project name, and both must keep it.
     *
     * A suffix invented to tell them apart shows a project no descriptor declares, and hides which folder that
     * name stands for. The folder hash the mapped name carries already keeps the two apart.
     */
    @Test
    void foldersDeclaringTheSameNameKeepIt() throws IOException {
        writeProject("catalog/quoting-core", "<project><name>Quoting</name></project>");
        writeProject("catalog/quoting-legacy", "<project><name>Quoting</name></project>");

        var delegate = new FileSystemRepository();
        delegate.setRoot(root);
        delegate.initialize();

        var mapped = MappedRepository.create(delegate, "DESIGN/");
        try {
            var names = mapped.listFolders("DESIGN/").stream().map(FileData::getName).toList();

            assertEquals(2, names.size(), "Both folders must be mapped: " + names);
            assertTrue(names.stream().allMatch(name -> name.startsWith("DESIGN/Quoting:")),
                    "Both folders must keep the declared name, but were: " + names);
            assertEquals(List.of("catalog/quoting-core", "catalog/quoting-legacy"),
                    names.stream().map(((FolderMapper) mapped)::getRealPath).sorted().toList(),
                    "Each mapped name must lead back to its own folder");
        } finally {
            ((Closeable) mapped).close();
        }
    }

    /** A folder that is gone is no longer one of the projects the repository maps. */
    @Test
    void aDeletedProjectIsNoLongerMapped() throws IOException {
        writeProject("catalog/quoting-core", "<project><name>Quoting</name></project>");
        writeProject("catalog/pricing", "<project><name>Pricing</name></project>");

        var delegate = new FileSystemRepository();
        delegate.setRoot(root);
        delegate.initialize();

        var mapped = MappedRepository.create(delegate, "DESIGN/");
        try {
            var quoting = mapped.listFolders("DESIGN/")
                    .stream()
                    .filter(data -> data.getName().startsWith("DESIGN/Quoting:"))
                    .findFirst()
                    .orElseThrow();

            assertTrue(mapped.delete(quoting), "The project must be deleted");

            assertNull(((FolderMapper) mapped).findMappedName("catalog/quoting-core"),
                    "The folder must be dropped from the mapping, not just left unreadable");
            var names = mapped.listFolders("DESIGN/").stream().map(FileData::getName).toList();
            assertEquals(1, names.size(), "Only the project that stays must be mapped: " + names);
            assertTrue(names.getFirst().startsWith("DESIGN/Pricing:"),
                    "The project that stays must keep its mapped name, but was: " + names);
        } finally {
            ((Closeable) mapped).close();
        }
    }

    @Test
    void branchViewBuildsMappingFromSelectedBranch() throws Exception {
        var main = branchRepository("main", "rules/main-project", "MainProject");
        var feature = branchRepository("feature/rates", "features/rates-project", "RatesProject");
        when(main.forBranch("feature/rates")).thenReturn(feature);

        var mapped = (BranchRepository) MappedRepository.create(main, "DESIGN/");
        try {
            assertEquals(List.of("MainProject"), businessNames(mapped.listFolders("DESIGN/")));

            var featureMapped = mapped.forBranch("feature/rates");
            try {
                assertEquals(List.of("RatesProject"), businessNames(featureMapped.listFolders("DESIGN/")));
            } finally {
                featureMapped.close();
            }
        } finally {
            mapped.close();
        }
    }

    @Test
    void nonBaseBranchUsesRequestedMappedFolderForANewProject() throws Exception {
        var feature = branchRepository("feature", "existing/project", "Existing");
        try (var mapped = MappedRepository.create(feature, "DESIGN/")) {
            var project = fileData("DESIGN/NewProject");
            project.addAdditionalData(FileMappingData.forProject(
                    "DESIGN/NewProject", "custom/path", "NewProject"));
            var descriptor = new FileItem(
                    "DESIGN/NewProject/rules.xml",
                    new ByteArrayInputStream("<project><name>NewProject</name></project>"
                            .getBytes(StandardCharsets.UTF_8)));

            mapped.save(project, List.of(descriptor), ChangesetType.FULL);

            var folderCaptor = org.mockito.ArgumentCaptor.forClass(FileData.class);
            verify(feature).save(folderCaptor.capture(), any(), eq(ChangesetType.FULL));
            assertEquals("custom/path/NewProject", folderCaptor.getValue().getName());
        }
    }

    @Test
    void mappingScanDoesNotLoadDeletionOrAuditMetadata() throws Exception {
        var repository = mock(BranchRepository.class);
        var folder = mock(FileData.class);
        var descriptor = mock(FileData.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setFolders(true).build());
        when(folder.getName()).thenReturn("project");
        when(repository.listFolders("")).thenReturn(List.of(folder));
        when(repository.check("project/rules.xml")).thenReturn(descriptor);
        when(repository.read("project/rules.xml")).thenReturn(new FileItem(
                descriptor,
                new ByteArrayInputStream(
                        "<project><name>Project</name></project>".getBytes(StandardCharsets.UTF_8))));

        var mapped = MappedRepository.create(repository, "DESIGN/");
        try {
            verify(folder, never()).isDeleted();
            verify(folder, never()).getModifiedAt();
            verify(descriptor, never()).isDeleted();
            verify(descriptor, never()).getModifiedAt();
        } finally {
            mapped.close();
        }
    }

    @Test
    void discoversExcelOnlyProjectsWhenEnabled() throws IOException {
        writeExcelFile("upload/rates.xlsx");

        var mapped = listMappedFolders("DESIGN/", true);

        assertEquals(1, mapped.size(), "The Excel-only folder must be discovered when enabled: " + mapped);
        assertTrue(mapped.getFirst().startsWith("DESIGN/upload:"),
                "The folder name must be used as the project name, but was: " + mapped);
    }

    @Test
    void skipsExcelOnlyProjectsByDefault() throws IOException {
        writeExcelFile("upload/rates.xlsx");
        writeProject("upload/real-project", "<project><name>Rates</name></project>");

        var mapped = listMappedFolders("DESIGN/");

        assertEquals(1, mapped.size(), "Only the project with rules.xml must be discovered: " + mapped);
        assertTrue(mapped.getFirst().startsWith("DESIGN/Rates:"),
                "The descriptor project nested under the skipped folder must still be found, but was: " + mapped);
    }

    @Test
    void identicalBranchTreesReuseMapping() throws Exception {
        var main = branchRepository("main", "rules/project", "Project", "descriptor-1", "tree-1");
        var feature = branchRepository("feature", "rules/project", "Project", "descriptor-1", "tree-1");
        when(main.forBranch("feature")).thenReturn(feature);

        var mapped = (BranchRepository) MappedRepository.create(main, "DESIGN/");
        try {
            var featureMapped = mapped.forBranch("feature");
            try {
                assertEquals(List.of("Project"), businessNames(featureMapped.listFolders("DESIGN/")));
                verify(feature, never()).listFolders("");
                verify(feature, never()).read("rules/project/rules.xml");
            } finally {
                featureMapped.close();
            }
        } finally {
            mapped.close();
        }
    }

    @Test
    void descriptorRevisionIsParsedOnceAcrossDifferentBranchTrees() throws Exception {
        var main = branchRepository("main", "rules/project", "Project", "descriptor-1", "tree-1");
        var feature = branchRepository("feature", "rules/project", "Project", "descriptor-1", "tree-2");
        when(main.forBranch("feature")).thenReturn(feature);

        var mapped = (BranchRepository) MappedRepository.create(main, "DESIGN/");
        try {
            var featureMapped = mapped.forBranch("feature");
            try {
                assertEquals(List.of("Project"), businessNames(featureMapped.listFolders("DESIGN/")));
                verify(feature, never()).read("rules/project/rules.xml");
            } finally {
                featureMapped.close();
            }
        } finally {
            mapped.close();
        }
    }

    @Test
    void blankDescriptorNameUsesEachBranchFolderWhenBlobIsReused() throws Exception {
        var main = branchRepository("main", "rules/project-one", "", "descriptor-1", "tree-1");
        var feature = branchRepository("feature", "rules/project-two", "", "descriptor-1", "tree-2");
        when(main.forBranch("feature")).thenReturn(feature);

        var mapped = (BranchRepository) MappedRepository.create(main, "DESIGN/");
        try {
            var featureMapped = mapped.forBranch("feature");
            try {
                assertEquals(List.of("project-two"), businessNames(featureMapped.listFolders("DESIGN/")));
                verify(feature, never()).read("rules/project-two/rules.xml");
            } finally {
                featureMapped.close();
            }
        } finally {
            mapped.close();
        }
    }

    /**
     * A save holds the lock of the repository while it checks its files against the mapping, as the access check of
     * a secured repository does. Meanwhile the change listener of an earlier save rebuilds the mapping, which reads
     * the repository. Neither may wait for the other, or both requests hang with every request that needs them.
     */
    @Test
    void saveAndMappingRebuildDoNotWaitForEachOther() throws Exception {
        var repositoryLock = new ReentrantReadWriteLock();
        var repository = branchRepository("main", "rules/project", "Project", "descriptor-1", "tree-1");
        var listener = new AtomicReference<Listener>();
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(0));
            return null;
        }).when(repository).setListener(any());
        var mapped = MappedRepository.create(repository, "DESIGN/");
        mapped.setListener(() -> {
        });
        var project = mapped.listFolders("DESIGN/").getFirst();

        var saving = new CountDownLatch(1);
        var rebuilding = new CountDownLatch(1);
        when(repository.getBranchTreeRevisions(List.of("main"), "")).thenAnswer(invocation -> {
            rebuilding.countDown();
            repositoryLock.readLock().lock();
            try {
                return Map.of("main", new BranchTreeRevision("main-tip", "tree-2"));
            } finally {
                repositoryLock.readLock().unlock();
            }
        });
        when(repository.save(any(FileData.class), any(), eq(ChangesetType.FULL))).thenAnswer(invocation -> {
            repositoryLock.writeLock().lock();
            try {
                saving.countDown();
                await(rebuilding);
                Iterable<FileItem> files = invocation.getArgument(1);
                files.forEach(file -> {
                });
                return invocation.getArgument(0);
            } finally {
                repositoryLock.writeLock().unlock();
            }
        });
        Iterable<FileItem> checkedFiles = () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                try {
                    mapped.check(project.getName());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return false;
            }

            @Override
            public FileItem next() {
                throw new NoSuchElementException();
            }
        };

        var failure = new AtomicReference<Exception>();
        var save = Thread.ofPlatform().daemon().start(() -> {
            try {
                mapped.save(project, checkedFiles, ChangesetType.FULL);
            } catch (IOException | RuntimeException e) {
                failure.set(e);
            }
        });
        await(saving);
        var onChange = Objects.requireNonNull(listener.get(), "The mapped repository listens to the repository");
        var rebuild = Thread.ofPlatform().daemon().start(onChange::onChange);
        save.join(Duration.ofSeconds(5));
        rebuild.join(Duration.ofSeconds(5));

        assertFalse(save.isAlive() || rebuild.isAlive(), "The save and the rebuild of the mapping wait for each other");
        assertNull(failure.get());
        ((Closeable) mapped).close();
    }

    /**
     * A project is added while the change listener of an earlier save scans the repository. The scan listed the
     * folders before the project was written, so its mapping is older than the one with the added project and must
     * not replace it.
     */
    @Test
    void aProjectAddedDuringARebuildStaysMapped() throws Exception {
        writeProject("rules/first", "<project><name>First</name></project>");
        var listener = new AtomicReference<Listener>();
        var delegate = watchedRepository(listener);
        var mapped = MappedRepository.create(delegate, "DESIGN/");
        try {
            mapped.setListener(() -> {
            });
            var scanned = new CountDownLatch(1);
            var added = new CountDownLatch(1);
            var firstScan = new AtomicBoolean(true);
            doAnswer(invocation -> {
                var folders = invocation.callRealMethod();
                if (firstScan.getAndSet(false)) {
                    scanned.countDown();
                    await(added);
                }
                return folders;
            }).when(delegate).listFolders("rules/");

            var onChange = Objects.requireNonNull(listener.get(), "The mapped repository listens to the repository");
            var rebuild = Thread.ofPlatform().daemon().start(onChange::onChange);
            await(scanned);
            writeProject("rules/second", "<project><name>Second</name></project>");
            ((FolderMapper) mapped).addMapping("rules/second");
            added.countDown();
            rebuild.join(Duration.ofSeconds(5));

            assertFalse(rebuild.isAlive(), "The rebuild of the mapping did not end");
            assertEquals(List.of("First", "Second"),
                    businessNames(mapped.listFolders("DESIGN/")).stream().sorted().toList());
        } finally {
            ((Closeable) mapped).close();
        }
    }

    /**
     * A new project is saved under a folder of its own choice. The change listener of an earlier save scans the
     * repository before the new folder is written, so the scan does not find it. The project must stay mapped while it
     * is saved, since the save and every other request translate its paths by the mapping.
     */
    @Test
    void aProjectBeingSavedStaysMappedWhileTheMappingIsRebuilt() throws Exception {
        writeProject("rules/first", "<project><name>First</name></project>");
        var listener = new AtomicReference<Listener>();
        var delegate = watchedRepository(listener);
        var mapped = MappedRepository.create(delegate, "DESIGN/");
        try {
            mapped.setListener(() -> {
            });
            var saving = new CountDownLatch(1);
            var rebuilt = new CountDownLatch(1);
            doAnswer(invocation -> {
                saving.countDown();
                await(rebuilt);
                return invocation.callRealMethod();
            }).when(delegate).save(any(FileData.class), any(), any(ChangesetType.class));
            var failure = new AtomicReference<Exception>();
            var save = Thread.ofPlatform().daemon().start(() -> saveSecondProject(mapped, failure));
            await(saving);
            Objects.requireNonNull(listener.get(), "The mapped repository listens to the repository").onChange();
            var mappedWhileSaving = ((FolderMapper) mapped).findMappedName("rules/second");
            rebuilt.countDown();
            save.join(Duration.ofSeconds(5));

            assertTrue(mappedWhileSaving != null && mappedWhileSaving.startsWith("DESIGN/Second:"),
                    "The rebuild dropped the project being saved, which is mapped as " + mappedWhileSaving);
            assertFalse(save.isAlive(), "The save did not end");
            assertNull(failure.get());
            assertEquals(List.of("First", "Second"),
                    businessNames(mapped.listFolders("DESIGN/")).stream().sorted().toList());
        } finally {
            ((Closeable) mapped).close();
        }
    }

    /**
     * The mapping is rebuilt while a new project is saved under a folder of its own choice. The scan does not find the
     * folder, which is written only after the scan has listed the folders, and the save ends before the scan does. The
     * project must stay mapped after the save.
     */
    @Test
    void aProjectSavedDuringARebuildStaysMapped() throws Exception {
        writeProject("rules/first", "<project><name>First</name></project>");
        var listener = new AtomicReference<Listener>();
        var delegate = watchedRepository(listener);
        var mapped = MappedRepository.create(delegate, "DESIGN/");
        try {
            mapped.setListener(() -> {
            });
            var saving = new CountDownLatch(1);
            var scanned = new CountDownLatch(1);
            var saved = new CountDownLatch(1);
            doAnswer(invocation -> {
                saving.countDown();
                await(scanned);
                return invocation.callRealMethod();
            }).when(delegate).save(any(FileData.class), any(), any(ChangesetType.class));
            var firstScan = new AtomicBoolean(true);
            doAnswer(invocation -> {
                var folders = invocation.callRealMethod();
                if (firstScan.getAndSet(false)) {
                    scanned.countDown();
                    await(saved);
                }
                return folders;
            }).when(delegate).listFolders("rules/");

            var failure = new AtomicReference<Exception>();
            var save = Thread.ofPlatform().daemon().start(() -> {
                saveSecondProject(mapped, failure);
                saved.countDown();
            });
            await(saving);
            var onChange = Objects.requireNonNull(listener.get(), "The mapped repository listens to the repository");
            var rebuild = Thread.ofPlatform().daemon().start(onChange::onChange);
            save.join(Duration.ofSeconds(5));
            rebuild.join(Duration.ofSeconds(5));

            assertFalse(save.isAlive() || rebuild.isAlive(), "The save or the rebuild of the mapping did not end");
            assertNull(failure.get());
            assertEquals(List.of("First", "Second"),
                    businessNames(mapped.listFolders("DESIGN/")).stream().sorted().toList());
        } finally {
            ((Closeable) mapped).close();
        }
    }

    @Test
    void addedFolderIsMappedByItsDescriptorOrItsName() throws IOException {
        var delegate = new FileSystemRepository();
        delegate.setRoot(root);
        delegate.initialize();
        var mapped = (FolderMapper) MappedRepository.create(delegate, "DESIGN/");
        try {
            // Written after the mapping was built, so only adding them maps them
            writeProject("rules/described", "<project><name>Described</name></project>");
            Files.createDirectories(root.resolve("rules/plain"));

            mapped.addMapping("rules/described/");
            mapped.addMapping("rules/plain");

            assertEquals(List.of("Described", "plain"), businessNames(((Repository) mapped).listFolders("DESIGN/")));
            assertThrows(IOException.class, () -> mapped.addMapping("rules/described"));
        } finally {
            ((Closeable) mapped).close();
        }
    }

    @Test
    void unreadableRepositoryIsMappedAsEmpty() throws Exception {
        var repository = branchRepository("main", "rules/project", "Project");
        when(repository.listFolders("")).thenThrow(new IOException("The repository cannot be read"));

        var mapped = MappedRepository.create(repository, "DESIGN/");
        try {
            assertTrue(mapped.listFolders("DESIGN/").isEmpty());
        } finally {
            ((Closeable) mapped).close();
        }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(5, TimeUnit.SECONDS), "The other thread did not get there in time");
    }

    /** A repository of {@link #root}, whose change listener the test notifies itself. */
    private FileSystemRepository watchedRepository(AtomicReference<Listener> listener) throws IOException {
        var delegate = spy(new FileSystemRepository());
        delegate.setRoot(root);
        delegate.initialize();
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(0));
            return null;
        }).when(delegate).setListener(any());
        return delegate;
    }

    /** Saves the project Second into the rules/second folder, which the mapping does not know yet. */
    private static void saveSecondProject(Repository mapped, AtomicReference<Exception> failure) {
        var folder = fileData("DESIGN/Second");
        folder.addAdditionalData(new FileMappingData("DESIGN/Second", "rules/second"));
        var descriptor = new FileItem("DESIGN/Second/rules.xml",
                new ByteArrayInputStream("<project><name>Second</name></project>".getBytes(StandardCharsets.UTF_8)));
        try {
            mapped.save(folder, List.of(descriptor), ChangesetType.FULL);
        } catch (IOException | RuntimeException e) {
            failure.set(e);
        }
    }

    private static BranchRepository branchRepository(String branch, String path, String projectName)
            throws IOException {
        return branchRepository(branch, path, projectName, null, null);
    }

    private static BranchRepository branchRepository(String branch,
                                                      String path,
                                                      String projectName,
                                                      @Nullable String descriptorRevision,
                                                      @Nullable String treeRevision) throws IOException {
        var repository = mock(BranchRepository.class);
        var folder = fileData(path);
        var descriptor = fileData(path + "/rules.xml");
        descriptor.setUniqueId(descriptorRevision);
        when(repository.supports())
                .thenReturn(new FeaturesBuilder(repository).setFolders(true).setBranches(true).build());
        when(repository.getBranch()).thenReturn(branch);
        when(repository.getBaseBranch()).thenReturn("main");
        if (treeRevision != null) {
            when(repository.getBranchTreeRevisions(List.of(branch), ""))
                    .thenReturn(Map.of(branch, new BranchTreeRevision(branch + "-tip", treeRevision)));
        }
        when(repository.listFolders("")).thenReturn(List.of(folder));
        when(repository.check(path)).thenReturn(folder);
        when(repository.check(path + "/rules.xml")).thenReturn(descriptor);
        when(repository.read(path + "/rules.xml")).thenAnswer(invocation -> new FileItem(
                descriptor,
                new ByteArrayInputStream(
                        "<project><name>%s</name></project>".formatted(projectName)
                                .getBytes(StandardCharsets.UTF_8))));
        return repository;
    }

    private static FileData fileData(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }

    private static List<String> businessNames(List<FileData> folders) {
        return folders.stream()
                .map(FileData::getName)
                .map(name -> name.substring("DESIGN/".length(), name.indexOf(':')))
                .toList();
    }

    private List<String> listMappedFolders(String baseFolder) throws IOException {
        return listMappedFolders(baseFolder, false);
    }

    private List<String> listMappedFolders(String baseFolder,
                                           boolean includeExcelFilesInProjectDiscovery) throws IOException {
        var delegate = new FileSystemRepository();
        delegate.setRoot(root);
        delegate.initialize();

        Repository mapped = MappedRepository.create(delegate, baseFolder, includeExcelFilesInProjectDiscovery);
        try {
            return mapped.listFolders(baseFolder).stream().map(FileData::getName).toList();
        } finally {
            if (mapped instanceof Closeable closeable) {
                closeable.close();
            }
        }
    }

    private void writeProject(String folder, String rulesXml) throws IOException {
        var projectFolder = root.resolve(folder);
        Files.createDirectories(projectFolder);
        Files.writeString(projectFolder.resolve("rules.xml"), rulesXml);
    }

    private void writeExcelFile(String file) throws IOException {
        var path = root.resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "test");
    }

}
