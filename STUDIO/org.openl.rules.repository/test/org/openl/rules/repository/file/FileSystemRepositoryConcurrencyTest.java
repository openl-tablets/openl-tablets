package org.openl.rules.repository.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.util.FileReplacement;

/**
 * A folder saved whole while it is saved by another request, as OpenL Rule Services do with a deployment deployed
 * twice at the same time, ends as one of the versions saved, and a reading never meets it half saved.
 *
 * @author Yury Molchan
 */
class FileSystemRepositoryConcurrencyTest {

    private static final List<String> FIRST = List.of("rules.xml", "first.xlsx", "first.yaml");
    private static final List<String> SECOND = List.of("rules.xml", "second.xlsx", "second.yaml");

    @TempDir
    Path root;

    @Test
    void aFullSaveKeepsTheTemporaryFileOfAWriteUnderWay() throws IOException {
        var repo = repository();
        save(repo, FIRST, "first");
        var underWay = root.resolve("deploy/.second.xlsx" + FileReplacement.TEMPORARY_SUFFIX);
        Files.writeString(underWay, "half written");

        save(repo, FIRST, "again");

        assertTrue(Files.exists(underWay));
    }

    @Test
    void concurrentFullSavesLeaveOneVersionAndAreNeverReadHalfSaved() throws Exception {
        var repo = repository();
        save(repo, FIRST, "first");
        var failures = new ConcurrentLinkedQueue<Throwable>();
        var seen = new ConcurrentLinkedQueue<Set<String>>();
        var saving = new AtomicBoolean(true);

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            var readers = new ArrayList<Future<?>>();
            for (var i = 0; i < 2; i++) {
                readers.add(executor.submit(() -> {
                    while (saving.get()) {
                        run(failures, () -> seen.add(names(repo)));
                    }
                }));
            }
            var writers = List.of(executor.submit(() -> saveRepeatedly(repo, FIRST, failures)),
                    executor.submit(() -> saveRepeatedly(repo, SECOND, failures)));
            try {
                for (var writer : writers) {
                    writer.get(60, TimeUnit.SECONDS);
                }
            } finally {
                // the readers stop even when a writer fails or stalls
                saving.set(false);
            }
            for (var reader : readers) {
                reader.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "The repository operations did not stop");
        }

        assertEquals(List.of(), List.copyOf(failures));
        var versions = Set.of(Set.copyOf(FIRST), Set.copyOf(SECOND));
        assertTrue(versions.containsAll(seen), () -> "Half saved: " + seen);
        var names = names(repo);
        assertTrue(versions.contains(names));
        // every file holds the content of one save, the one that wrote these names
        var contents = new HashSet<String>();
        for (var name : names) {
            contents.add(Files.readString(root.resolve("deploy").resolve(name)));
        }
        assertEquals(1, contents.size(), () -> "Mixed saves: " + contents);
        assertTrue(contents.iterator().next().startsWith(names.contains(FIRST.get(1)) ? "first " : "second "));
    }

    @Test
    void aReadingWaitsForASaveUnderWay() throws Exception {
        var repo = repository();
        save(repo, FIRST, "first");
        var paused = new CountDownLatch(1);
        var resume = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // the save stops in its second file, the first one already written
            var saving = executor.submit(() -> {
                save(repo, SECOND, file -> file.equals(SECOND.get(1)) ? pausing(paused, resume) : stream("second"));
                return null;
            });
            assertTrue(paused.await(10, TimeUnit.SECONDS), "The save did not start");
            var reading = executor.submit(() -> names(repo));
            assertThrows(TimeoutException.class, () -> reading.get(200, TimeUnit.MILLISECONDS));

            resume.countDown();
            saving.get(10, TimeUnit.SECONDS);
            assertEquals(Set.copyOf(SECOND), reading.get(10, TimeUnit.SECONDS));
        }
    }

    /** A content that, once read, waits until it is let go on. */
    private static InputStream pausing(CountDownLatch paused, CountDownLatch resume) {
        return new FilterInputStream(stream("second")) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                paused.countDown();
                try {
                    if (!resume.await(10, TimeUnit.SECONDS)) {
                        throw new IOException("The save was not let go on");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
                return super.read(b, off, len);
            }
        };
    }

    private static void saveRepeatedly(FileSystemRepository repo,
                                       List<String> files,
                                       ConcurrentLinkedQueue<Throwable> failures) {
        // each save writes its own content: the version it saves and its turn
        var version = files.equals(FIRST) ? "first " : "second ";
        for (var i = 0; i < 40; i++) {
            var content = version + i;
            run(failures, () -> save(repo, files, content));
        }
    }

    private interface Action {
        void run() throws IOException;
    }

    private static void run(ConcurrentLinkedQueue<Throwable> failures, Action action) {
        try {
            action.run();
        } catch (IOException | RuntimeException e) {
            failures.add(e);
        }
    }

    private FileSystemRepository repository() {
        var repo = new FileSystemRepository();
        repo.setRoot(root);
        repo.initialize();
        return repo;
    }

    private static Set<String> names(FileSystemRepository repo) throws IOException {
        return repo.list("deploy/")
                .stream()
                .map(data -> data.getName().substring("deploy/".length()))
                .collect(Collectors.toSet());
    }

    private static void save(FileSystemRepository repo, List<String> files, String content) throws IOException {
        save(repo, files, file -> stream(content));
    }

    private static void save(FileSystemRepository repo,
                             List<String> files,
                             Function<String, InputStream> content) throws IOException {
        var folder = new FileData();
        folder.setName("deploy");
        var changes = new ArrayList<FileItem>();
        for (var file : files) {
            var data = new FileData();
            data.setName("deploy/" + file);
            changes.add(new FileItem(data, content.apply(file)));
        }
        repo.save(folder, changes, ChangesetType.FULL);
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
