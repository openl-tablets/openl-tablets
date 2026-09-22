package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;

class FileToolTest {

    @Test
    void createsAnEmptyFileNamedByPrefixAndSuffix() throws IOException {
        var file = FileTool.createTempFile("openl-", ".part");
        try {
            assertTrue(Files.isRegularFile(file));
            assertEquals(0, Files.size(file));
            var name = file.getFileName().toString();
            assertTrue(name.startsWith("openl-") && name.endsWith(".part"), name);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void createsAnEmptyDirectoryNamedByPrefix() throws IOException {
        var directory = FileTool.createTempDirectory("openl-");
        try {
            assertTrue(Files.isDirectory(directory));
            assertTrue(directory.getFileName().toString().startsWith("openl-"));
        } finally {
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void onlyTheOwnerMayUseWhatIsCreated() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        var file = FileTool.createTempFile("openl-", null);
        var directory = FileTool.createTempDirectory("openl-");
        try {
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file));
            assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(directory));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void copiesTheStreamIntoATemporaryFile() throws IOException {
        var source = new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8));

        var file = FileTool.toTempFile(source, "stream.xls");

        assertNotNull(file);
        try {
            assertEquals("payload", Files.readString(file.toPath()));
            assertTrue(file.getName().startsWith("stream.xls"), file.getName());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    void reportsNoFileWhenTheStreamCannotBeRead() {
        var source = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("unreadable");
            }
        };

        assertNull(FileTool.toTempFile(source, "broken"));
    }
}
