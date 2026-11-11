package org.openl.rules.repository.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;

/**
 * A version the repository does not number names no version of a file. The history then answers as for a version it
 * does not have, instead of failing.
 */
class DBRepositoryTest {

    private static final String FILE = "project/rules.xlsx";
    private static final String COPY = "copy/rules.xlsx";
    private static final byte[] CONTENT = "content".getBytes(StandardCharsets.UTF_8);

    private JdbcDBRepository repository;
    private String version;

    @BeforeEach
    void setUp() throws IOException {
        repository = new JdbcDBRepository();
        repository.setId("design");
        repository.setName("H2");
        repository.setUri("jdbc:h2:mem:" + getClass().getSimpleName() + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        repository.initialize();
        repository.save(fileData(FILE), new ByteArrayInputStream(CONTENT));
        version = repository.check(FILE).getVersion();
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void check_history_finds_a_saved_version() throws IOException {
        var found = repository.checkHistory(FILE, version);

        assertNotNull(found);
        assertEquals(version, found.getVersion());
    }

    @Test
    void check_history_answers_nothing_for_a_version_that_is_not_a_number() throws IOException {
        assertNull(repository.checkHistory(FILE, "DEFAULT"));
    }

    @Test
    void check_history_answers_nothing_for_a_number_it_does_not_have() throws IOException {
        assertNull(repository.checkHistory(FILE, "999999"));
    }

    @Test
    void read_history_reads_a_saved_version() throws IOException {
        var item = repository.readHistory(FILE, version);

        assertArrayEquals(CONTENT, contentOf(item));
    }

    @Test
    void read_history_without_a_version_reads_the_file() throws IOException {
        var item = repository.readHistory(FILE, null);

        assertArrayEquals(CONTENT, contentOf(item));
    }

    @Test
    void read_history_answers_nothing_for_a_version_that_is_not_a_number() throws IOException {
        assertNull(repository.readHistory(FILE, "DEFAULT"));
    }

    @Test
    void delete_history_deletes_nothing_for_a_version_that_is_not_a_number() throws IOException {
        var data = fileData(FILE);
        data.setVersion("DEFAULT");

        assertFalse(repository.deleteHistory(data));
        assertNotNull(repository.checkHistory(FILE, version));
    }

    @Test
    void delete_history_deletes_a_saved_version() throws IOException {
        var data = fileData(FILE);
        data.setVersion(version);

        assertTrue(repository.deleteHistory(data));
        assertNull(repository.checkHistory(FILE, version));
    }

    @Test
    void copy_history_copies_a_saved_version() throws IOException {
        repository.copyHistory(FILE, fileData(COPY), version);

        assertNotNull(repository.check(COPY));
    }

    @Test
    void copy_history_rejects_a_version_that_is_not_a_number() {
        var error = assertThrows(FileNotFoundException.class,
                () -> repository.copyHistory(FILE, fileData(COPY), "DEFAULT"));

        assertEquals("The version 'DEFAULT' of the file 'project/rules.xlsx' is not found.", error.getMessage());
    }

    @Test
    void copy_history_rejects_a_number_it_does_not_have() throws IOException {
        var error = assertThrows(FileNotFoundException.class,
                () -> repository.copyHistory(FILE, fileData(COPY), "999999"));

        assertEquals("The version '999999' of the file 'project/rules.xlsx' is not found.", error.getMessage());
        assertNull(repository.check(COPY));
    }

    private static byte[] contentOf(@Nullable FileItem item) throws IOException {
        assertNotNull(item);
        try (var stream = item.getStream()) {
            return stream.readAllBytes();
        }
    }

    private static FileData fileData(String name) {
        var data = new FileData();
        data.setName(name);
        data.setComment("comment");
        return data;
    }
}
