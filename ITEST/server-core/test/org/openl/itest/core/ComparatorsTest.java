package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class ComparatorsTest {

    @Test
    void patternToRegexp() {
        assertEquals("\\$<\\?\\{[#\\d]+\\}\\+\\[[^￿]*\\]\\.\\{\\\\\\}\\?>\\^", Comparators.patternToRegexp("$<?{###}+[***].{\\}?>^"));
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(out)) {
            for (var entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static void assertSpec(String spec, byte[] actual) throws IOException {
        Comparators.zip(spec.getBytes(StandardCharsets.UTF_8), actual);
    }

    @Test
    void zipSpec_matches_by_name_and_content_wildcards() throws IOException {
        byte[] actual = zip(Map.of("rules.xml", "<project/>", "tags.properties", "Environment=prod\n"));
        assertDoesNotThrow(() -> assertSpec("rules.xml\ntags.properties = *Environment*prod*", actual));
    }

    @Test
    void zipSpec_name_glob_and_catch_all_ignore_extra_entries() throws IOException {
        var entries = new LinkedHashMap<String, String>();
        entries.put("DbSource/rules.xml", "x");
        entries.put("DbSource/tags.properties", "Environment=prod");
        entries.put("DbSource/extra.txt", "ignored");
        byte[] actual = zip(entries);
        assertDoesNotThrow(() -> assertSpec("*rules.xml = *\n*tags.properties = *prod*\n*", actual));
    }

    @Test
    void zipSpec_fails_on_missing_entry() throws IOException {
        byte[] actual = zip(Map.of("rules.xml", "x"));
        assertThrows(AssertionFailedError.class, () -> assertSpec("rules.xml\ntags.properties", actual));
    }

    @Test
    void zipSpec_fails_on_content_mismatch() throws IOException {
        byte[] actual = zip(Map.of("tags.properties", "Environment=dev"));
        assertThrows(AssertionFailedError.class, () -> assertSpec("tags.properties = *prod*", actual));
    }

    @Test
    void zipSpec_fails_on_unexpected_entry_without_catch_all() throws IOException {
        var entries = new LinkedHashMap<String, String>();
        entries.put("rules.xml", "x");
        entries.put("sneaky.txt", "y");
        byte[] actual = zip(entries);
        assertThrows(AssertionFailedError.class, () -> assertSpec("rules.xml", actual));
    }

    @Test
    void zip_binary_compare_still_works_for_real_archives() throws IOException {
        byte[] archive = zip(Map.of("a.txt", "one", "b.txt", "two"));
        assertDoesNotThrow(() -> Comparators.zip(archive, archive));
    }
}
