package org.openl.rules.webstudio.web.repository.project;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectFileTest {

    private static final byte[] CONTENT = "{\"openapi\": \"3.0.1\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void streamFileIsSavedToTempFileOnDemand() throws IOException {
        var projectFile = new ProjectFile("openapi.json", new ByteArrayInputStream(CONTENT));
        try {
            var tempFile = projectFile.getTempFile();
            assertTrue(tempFile.isFile());
            assertSame(tempFile, projectFile.getTempFile());
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                assertEquals("rw-------",
                        PosixFilePermissions.toString(Files.getPosixFilePermissions(tempFile.toPath())));
            }
            assertEquals(CONTENT.length, projectFile.getSize());
            // The content stays readable after the source stream has been consumed into the temporary file.
            try (var input = projectFile.getInput()) {
                assertArrayEquals(CONTENT, input.readAllBytes());
            }
            try (var input = projectFile.getInput()) {
                assertArrayEquals(CONTENT, input.readAllBytes());
            }
        } finally {
            projectFile.destroy();
        }
    }

    @Test
    void destroyDeletesTempFileAndIsRepeatable() throws IOException {
        var projectFile = new ProjectFile("openapi.json", new ByteArrayInputStream(CONTENT));
        try {
            var tempFile = projectFile.getTempFile();
            projectFile.destroy();
            assertFalse(tempFile.exists());
            projectFile.destroy();
            assertNull(projectFile.getTempFile());
        } finally {
            projectFile.destroy();
        }
    }

    @Test
    void failedStreamLeavesNoTempFile() throws IOException {
        var brokenStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Broken upload stream.");
            }
        };
        Set<Path> before = listUploadTempFiles();
        var projectFile = new ProjectFile("openapi.json", brokenStream);
        try {
            assertThrows(IOException.class, projectFile::getTempFile);
            assertEquals(before, listUploadTempFiles());
        } finally {
            projectFile.destroy();
        }
    }

    @Test
    void destroyClosesUnreadStream() throws IOException {
        var closed = new AtomicBoolean();
        var projectFile = new ProjectFile("openapi.json", new ByteArrayInputStream(CONTENT) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        });
        projectFile.destroy();
        assertTrue(closed.get());
        assertNull(projectFile.getTempFile());
    }

    @Test
    void boundedCopyRejectsOversizedStream(@TempDir Path tempDir) throws IOException {
        var target = tempDir.resolve("upload").toFile();
        assertThrows(IOException.class,
                () -> ProjectFile.copyBounded(new ByteArrayInputStream(CONTENT), target, CONTENT.length - 1));

        ProjectFile.copyBounded(new ByteArrayInputStream(CONTENT), target, CONTENT.length);
        assertArrayEquals(CONTENT, Files.readAllBytes(target.toPath()));
    }

    private static Set<Path> listUploadTempFiles() throws IOException {
        try (var files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return files.filter(f -> f.getFileName().toString().startsWith("openl-upload"))
                    .collect(Collectors.toSet());
        }
    }
}
